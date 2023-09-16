package com.demo.actor.message;

import com.demo.actor.objectPool.ObjectPool;
import lombok.Getter;

// Actor message
@SuppressWarnings("Convert2MethodRef")
@Getter
public class ActorMessage {

    private static final ObjectPool<ActorMessage> actorMessagePool = new ObjectPool<>(
            "actorMessagePool",
            10000,
            () -> {
                return new ActorMessage();
            },
            (actorMessage) -> {
                actorMessage.sender = null;
                actorMessage.message = null;
                return actorMessage;
            }
    );


    // field | but not can modify
    private String sender;
    private Object message;

    public String getSender() {
        return sender;
    }

    public Object getMessage() {
        return message;
    }

    // object pool use
    private ActorMessage() {

    }


    public static ActorMessage createNew(String sender,
                                         Object message) {
        return new ActorMessage(sender, message);
    }

    /**
     * 优化方式创建. objectPool / new
     *
     * @param sender  发送者
     * @param message 消息内容
     * @return ActorMessage
     */
    public static ActorMessage create(String sender,
                                      Object message) {
        ActorMessage one = actorMessagePool.getOne();
        one.sender = sender;
        one.message = message;
        return one;
    }

    private ActorMessage(String sender,
                         Object message) {
        this.sender = sender;
        this.message = message;
    }

}