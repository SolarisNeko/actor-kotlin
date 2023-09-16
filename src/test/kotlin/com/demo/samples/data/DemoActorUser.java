package com.demo.samples.data;

import com.demo.actor.core.Actor;
import com.neko233.skilltree.commons.core.base.StringUtils233;
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
