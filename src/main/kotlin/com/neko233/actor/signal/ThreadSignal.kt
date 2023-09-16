package com.neko233.actor.signal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport

@Deprecated("有 Bug")
class ThreadSignal {
    private var signals = AtomicInteger(0)

    // 等待（唤醒）
    fun await() {
        while (signals.get() <= 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10))
        }

        signals.decrementAndGet()
    }

    // 唤醒
    fun awake(count: Int = 1) {
        signals.accumulateAndGet(count) { v1, v2 -> v1 + v2 }

        for (i in 0 until count) {
            LockSupport.unpark(Thread.currentThread())
        }
    }
}