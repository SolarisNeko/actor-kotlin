package com.neko233.actor.worker

import com.neko233.actor.worker.api.ActorWorkerCreator
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 工作中心
 */
@Suppress("ReplaceGetOrSet")
class ActorWorkerCenter(
    private val centerName: String = "actorWorkerCenter",
    private val maxWorkers: Int,
    private val actorWorkerCreator: ActorWorkerCreator,
) : ActorWorkerCenterApi {

    // actorId : ActorWorker | 从 0 开始
    private val workers: MutableMap<Int, ActorWorker> = ConcurrentHashMap()

    // 处理 workerId 未匹配到任何 worker 的 job, index = 0
    private val remainWorker: ActorWorker


    init {

        LOGGER.info("=============== start to init ActorWorker ================")
        // 从 1 开始
        for (i in 1..maxWorkers) {
            workers[i] = actorWorkerCreator.invoke(centerName, i)
        }
        remainWorker = actorWorkerCreator.invoke(centerName, 0)

    }

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(ActorWorkerCenter::class.java)!!

    }

    override fun getWorkerSize(): Int {
        return workers.size
    }

    override fun async(
        actorId: String,
        task: Runnable,
    ) {
        // 根据 actorId 和策略选择合适的 Worker
        val workerId = chooseWorker(actorId)

        // 实际执行
        val actorWorker = workers.get(workerId)
        if (actorWorker == null) {
            LOGGER.error("not found workerId to worker. actorId=${actorId}, workerId=${workerId}")

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
