package com.neko233.actor

import com.neko233.actor.system.ActorSystem
import com.neko233.actor.system.SimpleActorSystem
import com.neko233.actor.worker.ActorWorkerCenter
import com.neko233.actor.worker.ActorWorkerCenterApi
import com.neko233.actor.worker.api.ActorWorkerCreator
import com.neko233.actor.worker.impl.CoroutineActorWorker

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-10-01
 * */
object ActorApp233 {

    /**
     * 快速开启一个 ActorSystem
     */
    @JvmStatic
    fun run(
        actorSystemName: String,
        workerNum: Int = Runtime.getRuntime().availableProcessors() * 2
    ): ActorSystem {

        // worker
        val actorWorkerCenter: ActorWorkerCenterApi = ActorWorkerCenter(
            "actorWorkerCenter-${actorSystemName}",
            workerNum,
            object : ActorWorkerCreator {
                override fun invoke(name: String, workerId: Int): CoroutineActorWorker {
                    return CoroutineActorWorker(name, workerId)
                }
            }
        )

        // system
        val actorSystem: ActorSystem = SimpleActorSystem(
            "actorSystem-${actorSystemName}",
            actorWorkerCenter
        )
        actorSystem.start()


        val closedThread = Thread {
            actorSystem.shutdown()
        }
        closedThread.name = "actorSystem-closeThread-${actorSystemName}"
        Runtime.getRuntime().addShutdownHook(closedThread)

        return actorSystem
    }
}