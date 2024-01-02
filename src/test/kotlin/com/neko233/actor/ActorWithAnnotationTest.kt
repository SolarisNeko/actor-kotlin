package com.neko233.actor

import com.neko233.mock.AnnotationActor
import com.neko233.mock.dto.OfflineMockData
import org.junit.Test

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-11-07
 * */
class ActorWithAnnotationTest {


    @Test
    fun demo() {
        // create a Actor-System name of "demo"
        val actorSystem = ActorApp233.run("demo")


        // register actor
        val actor1 = AnnotationActor("demo")
        actorSystem.addActor(actor1)

        println()

        // send to online actor (myself)
        actor1.send("demo", "demo")
        actor1.send("demo", "test-thread2")


        // online-actor send to offline-actor
        actor1.send(
            "demo233", OfflineMockData(
                name = "offline-thread1"
            )
        )
        actor1.send(
            "demo233", OfflineMockData(
                name = "offline-thread2"
            )
        )

        println()

    }
}