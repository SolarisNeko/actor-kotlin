package com.demo.actor.message.handler;

public interface ActorMessageTypeHandler<T> extends ActorMessageHandler<T> {

    Class<T> getType();

}
