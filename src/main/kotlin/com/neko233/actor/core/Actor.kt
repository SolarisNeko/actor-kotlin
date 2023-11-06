package com.neko233.actor.core

import com.alibaba.fastjson2.JSON
import com.neko233.actor.annotation.ActorMethodOffline
import com.neko233.actor.annotation.ActorMethodOnline
import com.neko233.actor.annotation.OnOtherActorThread
import com.neko233.actor.message.ActorMessage
import com.neko233.actor.message.ActorSyncMessage
import com.neko233.actor.message.handler.ActorOfflineMessageHandler
import com.neko233.actor.message.handler.ActorOnlineMessageHandler
import com.neko233.actor.message.handler.ActorOnlineMessageHandler.Companion.handle
import com.neko233.actor.message.handler.ActorOnlineMessageTypeHandler
import com.neko233.actor.system.ActorSystem
import com.neko233.skilltree.annotation.Nullable
import com.neko233.skilltree.commons.json.JsonUtils233
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


abstract class Actor
protected constructor(
    // 唯一标识 actorId
    private val actorId: String,
    // 邮箱大小
    private val maxMailboxSize: Int = 16,
) : ActorLifecycle {


    // state
    private val isExecute = AtomicBoolean(false)

    // 是否执行中
    val isInExecuteState: Boolean
        /**
         * @return 是否在执行中?
         */
        get() = isExecute.get()


    // 邮箱
    protected val mailbox: BlockingQueue<ActorMessage> = if (maxMailboxSize > 0) {
        ArrayBlockingQueue(maxMailboxSize)
    } else {
        LinkedBlockingQueue()
    }
    protected open var actorSystem: ActorSystem? = null
        get() {
            return field
        }


    // messageType: Class -> messageHandler
    protected val onlineMsgTypeHandlerMap: MutableMap<Class<*>, ActorOnlineMessageHandler<*>> = HashMap()
    protected val offlineMsgTypeHandlerMap: MutableMap<Class<*>, ActorOfflineMessageHandler<*>> = HashMap()


    companion object {
        val LOGGER = LoggerFactory.getLogger(Actor::class.java)!!
    }

    /**
     * default mailbox size
     */
    constructor(actorId: String)
            : this(actorId, 16)


    init {
        // 注册自己的注解方法
        this.registerActorHandlerOnlineByAnnotation()
    }

    fun getActorId(): String {
        return actorId
    }

    // actor system invoke
    fun registerToActorSystem(actorSystem: ActorSystem?) {
        this.actorSystem = actorSystem
    }

    /**
     * 收到一个其他人发来的的消息 (sync/ async)
     *
     * @param sender  发送者
     * @param message 消息
     * @return 是否接收成功
     */
    fun sendMessageToMailBox(
        sender: String?,
        message: Any?,
    ): Boolean {
        val msg = ActorMessage.createNew(sender, message)
        return mailbox.offer(msg)
    }

    fun <T> registerOnlineMessageHandler(actorMessageTypeHandler: ActorOnlineMessageTypeHandler<T>): Actor {
        return registerOnlineMessageHandler(actorMessageTypeHandler.getType(), actorMessageTypeHandler)
    }

    /**
     * 注册消息处理器
     * handle message by message Type
     */
    fun <T> registerOnlineMessageHandler(
        type: Class<T>?,
        actorMessageTypeHandler: ActorOnlineMessageHandler<T>?,
    ): Actor {
        if (type == null) {
            return this
        }
        if (actorMessageTypeHandler == null) {
            return this
        }

        onlineMsgTypeHandlerMap.merge(
            type,
            actorMessageTypeHandler
        ) { v1: ActorOnlineMessageHandler<*>?, v2: ActorOnlineMessageHandler<*> ->
            LOGGER.info("typeHandler merge to newHandler. type = {}", type.getName())
            v2
        }
        return this
    }

    /**
     * 离线消息处理器
     */
    fun <T> registerOfflineMessageHandler(
        type: Class<T>?,
        actorMessageTypeHandler: ActorOfflineMessageHandler<T>?,
    ): Actor {
        if (type == null) {
            return this
        }
        if (actorMessageTypeHandler == null) {
            return this
        }

        offlineMsgTypeHandlerMap.merge(
            type,
            actorMessageTypeHandler
        ) { v1, v2 ->
            LOGGER.info("typeHandler merge to newHandler. type = {}", type.getName())
            v2
        }
        return this
    }

    /**
     * 处理消息
     *
     * @param fromActorId 发送者的 actorId
     * @param message       消息
     */
    fun executeOnlineMessageHandlerByPipeline(
        fromActorId: String,
        message: Any,
    ) {
        val sender = getActorById(fromActorId)
        if (sender == null) {
            LOGGER.error(
                "sender not found. fromActorId={}, toActorId={}, data={}",
                fromActorId,
                this.actorId,
                JSON.toJSONString(message)
            )
            return
        }

        // sync message (同步, 反而是特殊的, 因为大部分 sender hope always running not blocking)
        if (message is ActorSyncMessage) {
            // sync start
            val syncMessage = message
            val data = syncMessage.message

            // 异步后等待
            this.handleMessageAsync(sender, data)

            // sync done
            syncMessage.finish()
            return
        }

        // default = async message
        this.handleMessageAsync(sender, message)
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
        message: Any,
    ) {
        val type: Class<*> = message.javaClass

        // message type to handler
        val actorMessageTypeHandler = onlineMsgTypeHandlerMap[type]
        if (actorMessageTypeHandler != null) {
            actorMessageTypeHandler.handle(sender, this, message)
            return
        }
        this.onReceiveMessageWithoutAnyMatch(sender.getActorId(), message)
    }

    @Nullable
    private fun getActorById(actorId: String): Actor? {
        return if (actorSystem == null) {
            null
        } else actorSystem!!.getActorByName(actorId)
    }

    /**
     * 当收到没有任何 Handler 处理的 message
     *
     * @param sender  发送人
     * @param message 消息
     */
    open fun onReceiveMessageWithoutAnyMatch(
        sender: String?,
        message: Any?,
    ) {

    }

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
                executeOnlineMessageHandlerByPipeline(
                    message.sender,
                    message.message
                )
            } catch (e: Throwable) {
                LOGGER.error("handle message error. message = {}", JsonUtils233.toJsonString(message), e)
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
                LOGGER.warn("空的调度. actorId = {}", actorId)
                return
            }

            // handle
            executeOnlineMessageHandlerByPipeline(
                message.sender,
                message.message
            )
        } catch (e: InterruptedException) {
            LOGGER.error("handle message error. message = {}", JsonUtils233.toJsonString(message), e)
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
        message: Any?,
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
        message: Any,
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
        message: Any,
    ): Boolean {
        return actorSystem!!.sendMessageSyncOrderly(receiverActorId!!, actorId, message)
    }

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

    override fun shutdown() {

    }

    /**
     * 注册注解标注的方法
     */
    @Suppress("UNCHECKED_CAST")
    fun registerActorHandlerOnlineByAnnotation() {
        val objClass = this.javaClass
        val methods = objClass.declaredMethods

        for (method in methods) {

            registerActorMethodOnline(method)
            registerActorMethodOffline(method)
        }
    }

    private fun registerActorMethodOnline(method: Method) {
        method.getAnnotation(ActorMethodOnline::class.java)
            ?: return
        // 找到标注了 @ActorHandler 的方法
        val parameterTypes = method.parameterTypes

        // 检查方法是否符合指定参数
        if (parameterTypes.size != 2) {
            LOGGER.error(
                "@ActorMethodOnline 在线 ActorHandler 有问题, args size = 2. class={}, methodName={}",
                this.javaClass,
                method.name
            )
            return
        }

        val paramType1 = parameterTypes[0]
        val paramType2 = parameterTypes[1]

        if (paramType1 == Actor::class.java
            && paramType2 == Actor::class.java
        ) {
            LOGGER.error(
                "@ActorMethodOnline 在线 ActorHandler 有问题, 不可以都为 Actor 类型. class={}, methodName={}",
                this.javaClass,
                method.name
            )
            return
        }

        // 消息类型
        var messageType: Class<Any>
        if (paramType1 == Actor::class.java) {
            messageType = paramType2 as Class<Any>
        } else {
            messageType = paramType1 as Class<Any>
        }

        // 前两个是 actor
        val self = this
        val handler = object : ActorOnlineMessageTypeHandler<Any> {
            override fun handle(
                sender: Actor,
                receiver: Actor,
                message: Any?,
            ) {
                method.invoke(self, sender, message)
            }

            override fun getType(): Class<Any> {
                return messageType
            }

        }

        this.registerOnlineMessageHandler(messageType, handler)
    }

    /**
     * 注册离线消息
     */
    private fun registerActorMethodOffline(method: Method) {
        method.getAnnotation(ActorMethodOffline::class.java)
            ?: return
        // 找到标注了 @ActorHandler 的方法
        val parameterTypes = method.parameterTypes

        // 检查方法是否符合指定参数
        if (parameterTypes.size != 2) {
            LOGGER.error(
                "@ActorMethodOffline 有问题. class={}, methodName={}. 参考 methodName(toActorId: String, message: Event)",
                this.javaClass,
                method.name
            )
            return
        }

        val paramType1 = parameterTypes[0]
        val paramType2 = parameterTypes[1]

        if (paramType1 == String::class.java
            && paramType2 == String::class.java
        ) {
            LOGGER.error(
                "@ActorMethodOffline 离线 ActorHandler 不允许消息为 String. class={}, methodName={}",
                this.javaClass,
                method.name
            )
            return
        }

        // 消息类型
        var messageType: Class<Any>
        if (paramType1 == String::class.java) {
            messageType = paramType2 as Class<Any>
        } else {
            messageType = paramType1 as Class<Any>
        }
        val messageIndex = parameterTypes.indexOf(messageType)

        // 前两个是 actor
        val self = this
        val handler = object : ActorOfflineMessageHandler<Any> {

            override fun handle(
                fromActor: Actor,
                toActorId: String,
                message: Any,
            ) {
                if (messageIndex == 0) {
                    method.invoke(self, message, toActorId)
                } else {
                    method.invoke(self, toActorId, message)
                }
            }

        }

        this.registerOfflineMessageHandler(messageType, handler)
    }

    /**
     * 处理其他 Actor 的离线消息
     */
    @OnOtherActorThread
    fun handleOfflineMessage(
        toActorId: String,
        message: Any,
    ) {
        val type: Class<*> = message.javaClass

        // message type to handler
        val offlineHandler = offlineMsgTypeHandlerMap[type]
        if (offlineHandler == null) {
            return
        }
        offlineHandler.handle(this, toActorId, message)
    }


}

