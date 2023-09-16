package com.neko233.actor.worker.impl

import com.neko233.actor.worker.ActorWorker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// 实现 ThreadWorker，它是 Worker 接口的一个具体实现
class ThreadActorWorker(
    centerName: String,
    id: Int
) : ActorWorker {

    val executor: ExecutorService

    init {
        executor = Executors.newSingleThreadExecutor {
            val thread = Thread(it)
            thread.name = "$centerName-$id"
            thread
        }
    }


    override fun execute(task: Runnable) {
        executor.submit(task)
    }

    override fun shutdown() {
        executor.shutdown()
    }
}
