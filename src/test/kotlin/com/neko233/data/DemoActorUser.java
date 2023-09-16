package com.neko233.data;

import com.neko233.actor.core.Actor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DemoActorUser extends Actor {

    public DemoActorUser(String actorId) {
        super(actorId);
    }

    @Override
    public void onReceiveMessageWithoutAnyMatch(String sender,
                                                Object message) {
        // 未匹配的数据
        log.error("no match message type. message = {}", message);
    }
}
