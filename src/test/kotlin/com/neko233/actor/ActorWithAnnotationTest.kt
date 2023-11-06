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
        val system = ActorApp233.run("demo")


        // here
        val a1 = AnnotationActor("demo")
        system.addActor(a1)

        println()

        // test
        a1.send("demo", "demo")
        a1.send("demo", "test-thread2")


        a1.send(
            "demo233", OfflineMockData(
                name = "offline-thread1"
            )
        )
        a1.send(
            "demo233", OfflineMockData(
                name = "offline-thread2"
            )
        )

        println()

    }
}