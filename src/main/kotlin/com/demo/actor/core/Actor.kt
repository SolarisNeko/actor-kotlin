package com.demo.actor.core

import com.demo.actor.message.ActorMessage
import com.demo.actor.message.ActorSyncMessage
import com.demo.actor.message.handler.ActorMessageHandler
import com.demo.actor.message.handler.ActorMessageHandler.Companion.handle
import com.demo.actor.message.handler.ActorMessageTypeHandler
import com.demo.actor.system.ActorSystem
import com.neko233.skilltree.annotation.Nullable
import com.neko233.skilltree.commons.json.JsonUtils233
import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


abstract class Actor
protected constructor(
// register to actor-system-java get
    private val actorId: String,
    // 最大多少邮箱
    private val maxMailboxSize: Int = 10000,
) {


    constructor(actorId: String)
            : this(actorId, 10000)

    protected open var actorSystem: ActorSystem? = null
        get() {
            return field
        }

    // 邮箱
    protected val mailbox: BlockingQueue<ActorMessage> = if (maxMailboxSize > 0) {
        ArrayBlockingQueue(maxMailboxSize)
    } else {
        LinkedBlockingQueue()
    }

    // messageType: Class -> messageHandler
    protected val messageTypeHandlerMap: MutableMap<Class<*>, ActorMessageHandler<*>> = HashMap()

    // state
    private val isExecute = AtomicBoolean(false)


    companion object {
        val log = LoggerFactory.getLogger(this::class.java)!!

    }

    fun getActorId(): String {
        return actorId
    }

    // actor system invoke
    fun registerActorSystem(actorSystem: ActorSystem?) {
        this.actorSystem = actorSystem
    }

    /**
     * 收到一个其他人发来的的消息 (sync/ async)
     *
     * @param sender  发送者
     * @param message 消息
     * @return 是否接收成功
     */
    fun receiveMessage(
        sender: String?,
        message: Any?
    ): Boolean {
        val msg = ActorMessage.createNew(sender, message)
        return mailbox.offer(msg)
    }

    fun <T> registerMessageHandler(actorMessageTypeHandler: ActorMessageTypeHandler<T>): Actor {
        return registerMessageHandler(actorMessageTypeHandler.getType(), actorMessageTypeHandler)
    }

    /**
     * 注册消息处理器
     * handle message by message Type
     */
    fun <T> registerMessageHandler(
        type: Class<T>?,
        actorMessageTypeHandler: ActorMessageHandler<T>?
    ): Actor {
        if (type == null) {
            return this
        }
        if (actorMessageTypeHandler == null) {
            return this
        }

        messageTypeHandlerMap.merge(
            type,
            actorMessageTypeHandler
        ) { v1: ActorMessageHandler<*>?, v2: ActorMessageHandler<*> ->
            log.info("typeHandler merge to newHandler. type = {}", type.getName())
            v2
        }
        return this
    }

    /**
     * 处理消息
     *
     * @param senderActorId 发送者的 actorId
     * @param message       消息
     */
    fun handleMessageByPipeline(
        senderActorId: String,
        message: Any
    ) {
        val sender = getActorById(senderActorId)
        if (sender == null) {
            log.error("senderActorId = {} not found", senderActorId)
            return
        }

        // sync message (同步, 反而是特殊的, 因为大部分 sender hope always running not blocking)
        if (message is ActorSyncMessage) {
            // sync start
            val syncMessage = message
            val data = syncMessage.message
            handleMessageAsync(sender, data)

            // sync done
            syncMessage.finish()
            return
        }

        // default = async message
        handleMessageAsync(sender, message)
        return
    }

    /**
     * 异步处理消息
     *
     * @param sender  发送者
     * @param message 消息
     */
    private fun handleMessageAsync(
        sender: Actor,
        message: Any
    ) {
        val type: Class<*> = message.javaClass

        // message type to handler
        val actorMessageTypeHandler = messageTypeHandlerMap[type]
        if (actorMessageTypeHandler != null) {
            actorMessageTypeHandler.handle(sender, this, message)
            return
        }
        onReceiveMessageWithoutAnyMatch(sender.getActorId(), message)
    }

    @Nullable
    private fun getActorById(actorId: String): Actor? {
        return if (actorSystem == null) {
            null
        } else actorSystem!!.getActorByName(actorId)
    }

    /**
     * 当收到消息
     *
     * @param sender  发送人
     * @param message 消息
     */
    abstract fun onReceiveMessageWithoutAnyMatch(
        sender: String?,
        message: Any?
    )

    /**
     * 消费所有消息
     */
    fun takeAll() {
        while (true) {
            // preemption
            val isPremptionOk = switchIdleToExecuteState()
            if (!isPremptionOk) {
                continue
            }
            var message: ActorMessage? = null
            try {
                message = mailbox.poll(100, TimeUnit.MILLISECONDS)
                if (message == null) {
                    break
                }
                handleMessageByPipeline(
                    message.sender,
                    message.message
                )
            } catch (e: Throwable) {
                log.error("handle message error. message = {}", JsonUtils233.toJsonString(message), e)
            } finally {
                switchExecuteToIdleState()
            }
        }
    }

    /**
     * 消费消息
     */
    fun consumeMessage() {
        var message: ActorMessage? = null
        try {
            // get msg from mailbox
            message = mailbox.poll(100, TimeUnit.MILLISECONDS)
            if (message == null) {
                log.warn("空的调度. actorId = {}", actorId)
                return
            }

            // handle
            handleMessageByPipeline(
                message.sender,
                message.message
            )
        } catch (e: InterruptedException) {
            log.error("handle message error. message = {}", JsonUtils233.toJsonString(message), e)
        }
        switchExecuteToIdleState()
    }

    /**
     * @param receiverActorId 接收人 actorId
     * @param message         消息
     * @return is success ?
     */
    fun send(
        receiverActorId: String?,
        message: Any?
    ): Boolean {
        return actorSystem!!.sendMessageAsync(actorId, receiverActorId!!, message!!)
    }

    /**
     * 同步调用. 跨线程, 霸占式
     *
     * @param receiverActorId 接收人的 actorId
     * @param message         消息
     * @return is success ?
     */
    fun talkPredatory(
        receiverActorId: String?,
        message: Any
    ): Boolean {
        return actorSystem!!.sendMessageSyncPredatory(receiverActorId!!, actorId, message)
    }

    /**
     * 同步调用. 跨线程, 顺序式
     *
     * @param receiverActorId 接收人的 actorId
     * @param message         消息
     * @return is success ?
     */
    fun talkOrderly(
        receiverActorId: String?,
        message: Any
    ): Boolean {
        return actorSystem!!.sendMessageSyncOrderly(receiverActorId!!, actorId, message)
    }

    val isInExecuteState: Boolean
        /**
         * @return 是否在执行中?
         */
        get() = isExecute.get()

    /**
     * @return 尝试设置 execute 状态
     */
    fun switchIdleToExecuteState(): Boolean {
        return isExecute.compareAndSet(false, true)
    }

    /**
     * @return 尝试设置 idle 状态
     */
    fun switchExecuteToIdleState(): Boolean {
        return isExecute.compareAndSet(true, false)
    }

    fun shutdown() {

    }

}

