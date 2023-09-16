package com.demo.actor.worker

/**
 * 工作中心
 */
interface ActorWorkerCenter {
    fun getWorkerSize(): Int
    fun async(actorId: String, task: Runnable)
    fun shutdown()
}