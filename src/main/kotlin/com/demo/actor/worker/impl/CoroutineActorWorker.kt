package com.demo.actor.worker.impl

import com.demo.actor.worker.ActorWorker
import kotlinx.coroutines.*
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

// 知道正在使用不稳定的 API = DelicateCoroutinesApi，并且您愿意接受与之相关的变化。
@OptIn(DelicateCoroutinesApi::class)
@Slf4j
class CoroutineActorWorker(
    centerName: String,
    id: Int
) : ActorWorker {

    // 协程 作用域
    private val coroutineScope: CoroutineScope
    private val isShutdown = AtomicBoolean(false)


    companion object {
        val log = LoggerFactory.getLogger(this::class.java)
    }


    init {
        val uniqueActorCoroutineDispatcher: ExecutorCoroutineDispatcher =
            newFixedThreadPoolContext(1, "$centerName-$id-coroutine")

        val uniqueId = "$centerName-$id"

        coroutineScope = CoroutineScope(
            SupervisorJob()
                    + uniqueActorCoroutineDispatcher
                    + CoroutineName(uniqueId)
        )
//        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName(uniqueId))

        coroutineScope.launch {
            log.info("init coroutine-actor-worker done. uniqueId = ${uniqueId}")
        }
    }

    override fun execute(task: Runnable) {
        if (isShutdown.get()) {
            throw IllegalStateException("Worker has been shut down")
        }

        coroutineScope.launch {
            try {
                task.run()
            } catch (t: Throwable) {
                // 捕获并处理异常
                handleUncaughtException(t)
            }
        }
    }

    override fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            coroutineScope.coroutineContext.cancelChildren()

            coroutineScope.cancel()

            log.info("shutdown coroutine")
        }
    }

    private fun handleUncaughtException(t: Throwable) {
        // 在这里处理异常，例如记录日志或采取其他措施
        log.error("uncaught exception", t)
    }
}
