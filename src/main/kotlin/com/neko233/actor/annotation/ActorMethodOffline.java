package com.neko233.actor.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 该方法执行时, Actor this 作为 Sender 发送人
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ActorMethodOffline {
}
