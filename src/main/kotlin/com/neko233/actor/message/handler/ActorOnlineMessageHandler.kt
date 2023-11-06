package com.neko233.actor.message.handler;

import com.neko233.actor.core.Actor

interface ActorOnlineMessageHandler<T> {

    /**
     * @param sender   发送者
     * @param receiver 接收人 (自己)
     * @param message  消息
     */
    fun handle(
        sender: Actor,
        receiver: Actor,
        message: T
    )

    companion object {

        /**
         * @JvmDefault
         * Need = -Xjvm-Default=Compatible
         */
        fun <T> ActorOnlineMessageHandler<T>.handle(sender: Actor, receiver: Actor, message: Any) {
            handle(sender, receiver, message as T)
        }
    }
}

