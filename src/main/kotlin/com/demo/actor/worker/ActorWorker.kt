package com.demo.actor.worker

/**
 * Actor 的工作者
 */
interface ActorWorker {
    /**
     * 执行任务
     */
    fun execute(task: Runnable)

    /**
     * 关闭
     */
    fun shutdown()
}