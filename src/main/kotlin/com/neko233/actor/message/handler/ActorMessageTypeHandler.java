package com.neko233.actor.message.handler;

public interface ActorMessageTypeHandler<T> extends ActorMessageHandler<T> {

    Class<T> getType();

}
