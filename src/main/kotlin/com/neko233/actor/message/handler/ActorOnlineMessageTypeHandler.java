package com.neko233.actor.message.handler;

public interface ActorOnlineMessageTypeHandler<T> extends ActorOnlineMessageHandler<T> {

    Class<T> getType();

}
