package com.neko233.actor.worker.api

import com.neko233.actor.worker.impl.CoroutineActorWorker

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-10-01
 * */
interface ActorWorkerCreator {


    /**
     * 执行
     */
    fun invoke(name: String, workerId: Int): CoroutineActorWorker
}