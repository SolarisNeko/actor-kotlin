package com.neko233.actor.worker

/**
 * 工作中心
 */
interface ActorWorkerCenterApi {

    /**
     * 工作者人数
     */
    fun getWorkerSize(): Int

    /**
     * 异步执行
     */
    fun async(
        actorId: String,
        task: Runnable,
    )

    /**
     * 关闭
     */
    fun shutdown()
}