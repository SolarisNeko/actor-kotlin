package com.demo.actor.objectPool

import java.util.*

interface ObjectPoolApi<T> {
    fun getUniqueName(): String
    fun getObjectPoolQueue(): Queue<T>
    fun getMaxCacheCount(): Int
    fun getDestroyer(): (T) -> T
    fun getCreator(): () -> T

    fun getOne(): T {
        val objPoolQueue = getObjectPoolQueue()
        return if (objPoolQueue.isNotEmpty()) {
            objPoolQueue.poll()
        } else {
            getCreator().invoke()
        }
    }

    fun recycle(obj: T): Boolean {
        if (obj == null) {
            println("不能够回收 null 对象")
            return false
        }

        val destroyer = getDestroyer()
        val queue = getObjectPoolQueue()
        return if (queue.size < getMaxCacheCount()) {
            destroyer.invoke(obj)
            queue.offer(obj)
            true
        } else {
            destroyer.invoke(obj)
            false
        }
    }

    fun clear() {
        getObjectPoolQueue().clear()
    }

    fun getCurrentCacheCount(): Int = getObjectPoolQueue().size
}
