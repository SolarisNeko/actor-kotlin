package com.demo.actor.worker

import com.demo.actor.worker.impl.CoroutineActorWorker
import com.demo.actor.worker.impl.CoroutineActorWorker.Companion.log
import com.neko233.skilltree.commons.core.base.ArrayUtils233
import java.util.concurrent.ConcurrentHashMap

/**
 * 工作中心
 */
@Suppress("ReplaceGetOrSet")
class DefaultActorWorkerCenter(
    private val centerName: String = "actorWorkerCenter",
    private val maxWorkers: Int,
    private val actorWorkerCreator: (String, Int) -> CoroutineActorWorker
) : ActorWorkerCenter {

    // actorId : ActorWorker | 从 0 开始
    private val workers: MutableMap<Int, ActorWorker> = ConcurrentHashMap()

    // 处理 workerId 未匹配到任何 worker 的 job, index = 0
    private val remainWorker: ActorWorker


    init {
        // 从 1 开始
        for (i in 1..maxWorkers) {
            workers[i] = actorWorkerCreator.invoke(centerName, i)
        }

        remainWorker = actorWorkerCreator.invoke(centerName, 0)
    }

    override fun getWorkerSize(): Int {
        return workers.size
    }

    override fun async(actorId: String, task: Runnable) {
        // 根据 actorId 和策略选择合适的 Worker
        val workerId = chooseWorker(actorId)

        // 实际执行
        val actorWorker = workers.get(workerId)
        if (actorWorker == null) {
            log.error("not found workerId to worker. actorId=${actorId}, workerId=${workerId}")

            remainWorker.execute(task)
            return
        }

        actorWorker.execute(task)
    }

    private fun chooseWorker(actorId: String): Int {
        // 这里可以根据 actorId 和策略来选择合适的 Worker
        // 这里简单地将 actorId 转换为整数作为 Worker 的索引
        val actorIdHashCode = actorId.hashCode()
        return Math.abs(actorIdHashCode) % maxWorkers
    }

    // 关闭 WorkerCenter 时需要关闭所有 ExecutorService
    override fun shutdown() {
        workers.values.forEach { it.shutdown() }
    }
}
