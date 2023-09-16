package com.demo.actor.system

import com.demo.actor.annotation.NotThreadSafeField
import com.demo.actor.core.Actor
import com.demo.actor.message.ActorSyncMessage
import com.demo.actor.worker.ActorWorkerCenter
import com.neko233.skilltree.annotation.ThreadSafe
import com.neko233.skilltree.commons.core.base.MapUtils233
import com.neko233.skilltree.commons.core.base.StringUtils233
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


/**
 * Actor System
 *
 * @author SolarisNeko on 2023-01-01
 */
abstract class ActorSystem(
    private val systemName: String,
    // 执行层 | systemName, workerId ->
    private val actorWorkerCenter: ActorWorkerCenter,
    private val maxShutdownWaitMs: Long = TimeUnit.SECONDS.toMillis(30),
) {

    constructor(
        systemName: String,
        actorWorkerCenter: ActorWorkerCenter,
    ) : this(
        systemName,
        actorWorkerCenter,
        TimeUnit.SECONDS.toMillis(30)
    )

    @ThreadSafe
    private val isShutdown = AtomicBoolean(false)


    // 系统 n 次没有消息可消费
    @ThreadSafe
    private val systemNoMsgDispatchTermCount = AtomicInteger(0)

    // actorId : Actor
    @NotThreadSafeField
    private val actorIdToActorMap: MutableMap<String, Actor> = ConcurrentHashMap()

    // actorId : 未读消息数
    @NotThreadSafeField
    private val actorIdToUnreadMsgCounterMap: MutableMap<String, AtomicInteger> = ConcurrentHashMap()


    companion object {
        val log = LoggerFactory.getLogger(this::class.java)!!

        // 最大多少次没有消息消费, 则进入【怠慢模式】
        const val MAX_NO_HANDLE_COUNT = 10
    }

    /**
     * 分发器 | 单线程, 后续考虑多线程 + 竞争, 但意义不大
     */
    private val dispatcherThread = Thread(dispatchActorMsgRunnable())

    /**
     * 检查 + 启动
     */
    fun start() {
        log.info("========== ActorSystem = {}, start up start ==========", systemName)
        // dispatcher
        dispatcherThread.setName("$systemName-thread-dispatcher")
        dispatcherThread.start()
        log.info("========== ActorSystem = {}, start up end ==========", systemName)
    }

    private fun dispatchActorMsgRunnable(): Runnable {
        return Runnable {
            // infinite loop
            while (true) {

//                dispatcherSignal.await();
                try {
                    if (isShutdown.get()) {
                        if (actorIdToUnreadMsgCounterMap.isEmpty()) {
                            break
                        }
                        dispatchActorMessageToCosume()
                    }

                    // 没有需要执行的
                    if (actorIdToUnreadMsgCounterMap.isEmpty()) {
                        systemNoMsgDispatchTermCount.getAndIncrement()
                    }

                    // 失败次数过多
                    if (systemNoMsgDispatchTermCount.get() > MAX_NO_HANDLE_COUNT) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000)
                        } catch (e: InterruptedException) {
                            // 被唤醒. 但不一定是因为
                            log.debug("awake, maybe can consume")
                            systemNoMsgDispatchTermCount.set(0)
                        }
                        continue
                    }
                    if (actorIdToUnreadMsgCounterMap.isEmpty()) {
                        continue
                    }

                    // @NeedThreadSafe 这部分 | 目前单线程
                    dispatchActorMessageToCosume()
                } catch (t: Throwable) {
                    log.error(
                        "[ActorSystem] dispatcher-thread meet unknown-exception. will continue work",
                        t
                    )
                }
            }
        }
    }

    private fun logShutdown() {
        log.info(
            "\n" + """
                                    ====================================
                                            Actor-System shutdown
                                            # ${systemName} #
                                    ===================================
                                """.trimIndent()
        )
    }

    private fun dispatchActorMessageToCosume() {
        for ((receiverActorId, unreadMsgCounter) in actorIdToUnreadMsgCounterMap) {

            // 消息数
            val messageCount = unreadMsgCounter.get()
            if (messageCount <= 0) {
                // 删除监听
                actorIdToUnreadMsgCounterMap.remove(receiverActorId)
                continue
            }

            // 收到消息的 actor
            val receiverActor = actorIdToActorMap[receiverActorId] ?: continue
            val inExecuteState = receiverActor.isInExecuteState
            if (inExecuteState) {
                continue
            }

            // 正在执行中
            val isExecute = receiverActor.switchIdleToExecuteState()
            if (!isExecute) {
                continue
            }

            // 未读消息 - 1 ｜ todo 有多线程问题
            val isConsumeMessageSuccess = unreadMsgCounter.decrementAndGet() >= 0
            if (!isConsumeMessageSuccess) {
                val consumeMessageCount = unreadMsgCounter.get()
                log.warn(
                    "actorId 消费未读消息失败. actorId = {}, consumeMessageCount = {}",
                    receiverActorId,
                    consumeMessageCount
                )
                continue
            }

            // 成功处理
            systemNoMsgDispatchTermCount.set(0)

            // worker. async delegate ActorWorkCenter to execute real business logic
            actorWorkerCenter.async(receiverActorId) { receiverActor.consumeMessage() }
        }
    }

    fun addActor(actor: Actor) {
        val actorId = actor.getActorId()
        if (StringUtils.isBlank(actorId)) {
            val exceptionLog = StringUtils233.format(
                "actorId can not is blank! actorId = \"\${actorId}\"",
                actorId
            )
            throw IllegalArgumentException(exceptionLog)
        }
        actorIdToActorMap[actorId] = actor
        actor.registerActorSystem(this)
    }

    /**
     * 异步消息
     *
     * @param senderActorId   发送者的 actorId
     * @param receiverActorId 接收人的 actorId
     * @param message         消息
     * @return 是否成功
     */
    fun sendMessageAsync(
        senderActorId: String,
        receiverActorId: String,
        message: Any
    ): Boolean {
        return _send(senderActorId, receiverActorId, message)
    }

    /**
     * 【Core】所有发消息的最终入口
     *
     * @return isOk | 投递消息失败, 由外层业务自己处理
     */
    private fun _send(senderActorId: String, receiverActorId: String, message: Any): Boolean {
        if (isShutdown.get()) {
            log.warn(
                "actor-system is shutdown now. so not send message from actorId = {}, to actorId = {}",
                senderActorId,
                receiverActorId
            )
            return false
        }
        val receiverActor = actorIdToActorMap[receiverActorId]
        if (receiverActor == null) {
            log.error("not found actorId = {}", receiverActorId)
            return false
        }

        // 发给 receiver 的 mailbox
        val isSuccess = receiverActor.receiveMessage(senderActorId, message)
        if (!isSuccess) {
            return false
        }

        // 记录待处理
        addMessageCount(receiverActorId)

//        // 响应式
//        dispatcherSignal.awake(1);
        if (dispatcherThread.state != Thread.State.RUNNABLE) {
            dispatcherThread.interrupt()
        }
        return true
    }

    private fun addMessageCount(actorId: String) {
        val unreadMsgCounter = actorIdToUnreadMsgCounterMap.computeIfAbsent(
            actorId
        ) { key: String? -> AtomicInteger(0) }
        unreadMsgCounter.getAndIncrement()
    }

    /**
     * 同步霸占式调用
     *
     * @param receiverActorName 接收人 actorId
     * @param senderActorName   发送者 actorId
     * @param message           消息
     * @return is success ?
     */
    fun sendMessageSyncPredatory(
        receiverActorName: String,
        senderActorName: String,
        message: Any
    ): Boolean {
        val actor = actorIdToActorMap[receiverActorName]
        if (actor == null) {
            log.error("not found actorId = {}", receiverActorName)
            return false
        }

        // 同步调用, 自旋同步尝试
        while (true) {
            return try {
                val isSetStateSuccess = actor.switchIdleToExecuteState()
                if (!isSetStateSuccess) {
                    TimeUnit.MILLISECONDS.sleep(50)
                    continue
                }
                actor.handleMessageByPipeline(senderActorName, message)
                break
            } catch (e: Exception) {
                log.error(
                    "同步执行报错. senderActorName={}, receiverActorName={}",
                    senderActorName,
                    receiverActorName,
                    e
                )
                false
            } finally {
                actor.switchExecuteToIdleState()
            }
        }
        return true
    }

    /**
     * 同步有序式调用
     *
     * @param receiverActorName 接收人 actorId
     * @param senderActorName   发送者 actorId
     * @param message           消息
     * @return is success ?
     */
    fun sendMessageSyncOrderly(
        receiverActorName: String,
        senderActorName: String,
        message: Any
    ): Boolean {
        val actor = actorIdToActorMap[receiverActorName]
        if (actor == null) {
            log.error("not found actorId = {}", receiverActorName)
            return false
        }
        if (message is ActorSyncMessage) {
            log.error("不允许发送 type = ActorSyncMessage 作为数据的同步消息!")
            return false
        }
        val syncMessage = ActorSyncMessage(
            senderActorName,
            message
        )
        sendMessageAsync(senderActorName, receiverActorName, syncMessage)
        try {
            syncMessage.waitFinish()
        } catch (e: Exception) {
            log.error("同步等待时报错. message = {}", message, e)
            return false
        }
        return true
    }

    fun getActorByName(actorId: String): Actor? {
        return actorIdToActorMap[actorId]
    }


    /**
     * 关闭系统
     */
    fun shutdown() {
        val isOk = isShutdown.compareAndSet(false, true)
        if (!isOk) {
            return
        }


        val startMs = System.currentTimeMillis()
        while (isNotConsumeAllMessage()) {

            val endMs = System.currentTimeMillis()
            val haveWaitMs = endMs - startMs
            if (haveWaitMs > maxShutdownWaitMs) {
                break
            }

            // wait
            TimeUnit.SECONDS.sleep(1)
        }


        // actor all
        val actors = actorIdToActorMap.values
        actorIdToActorMap.clear()

        actors.forEach {
            it.shutdown()
        }

        // worker
        actorWorkerCenter.shutdown()

        logShutdown()

    }

    private fun isNotConsumeAllMessage(): Boolean {
        return !isConsumeAllMessage()
    }

    /**
     * 是否所有消息都消费完了
     */
    private fun isConsumeAllMessage(): Boolean {
        return MapUtils233.isEmpty(actorIdToUnreadMsgCounterMap)
    }
}

