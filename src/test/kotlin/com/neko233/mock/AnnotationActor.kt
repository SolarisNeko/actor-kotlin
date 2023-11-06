package com.neko233.mock

import com.neko233.actor.annotation.ActorMethodOffline
import com.neko233.actor.annotation.ActorMethodOnline
import com.neko233.actor.core.Actor
import com.neko233.mock.dto.OfflineMockData
import org.slf4j.LoggerFactory

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-11-06
 * */
class AnnotationActor(actorId: String) : Actor(actorId) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(AnnotationActor::class.java)!!

    }

    @ActorMethodOnline
    fun onlineFun(
        sender: Actor,
        data: String,
    ) {
        LOGGER.warn(
            "\nfromActorId={}, toActorId={}, data={}\n", sender.getActorId(),
            this.getActorId(),
            data
        )
    }

    @ActorMethodOffline
    fun offlineFun(
        toActorId: String,
        data: OfflineMockData,
    ) {
        LOGGER.warn(
            "\nmyActorId={}, toActorId={}, data={}\n", this.getActorId(),
            toActorId,
            data
        )
    }


}