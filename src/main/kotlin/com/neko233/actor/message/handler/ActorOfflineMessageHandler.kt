package com.neko233.actor.message.handler;

import com.neko233.actor.core.Actor

interface ActorOfflineMessageHandler<T> {

    /**
     * 在 toActorId 线程上执行
     *
     * @param fromActor   发送者
     * @param toActorId     接收人 (自己)
     * @param message  消息
     */
    fun handle(
        fromActor: Actor,
        toActorId: String,
        message: Any,
    )

}

