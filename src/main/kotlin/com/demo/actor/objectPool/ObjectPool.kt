package com.demo.actor.objectPool

import java.util.*

class ObjectPool<T>(
    private val uniqueName: String,
    private val maxCacheSize: Int,
    private val creator: () -> T,
    private val destroyer: (T) -> T
) : ObjectPoolApi<T> {

    private val objPoolQueue: Queue<T> = LinkedList()

    override fun getUniqueName(): String {
        return uniqueName
    }

    override fun getObjectPoolQueue(): Queue<T> {
        return objPoolQueue
    }

    override fun getMaxCacheCount(): Int {
        return maxCacheSize
    }

    override fun getDestroyer(): (T) -> T {
        return destroyer
    }

    override fun getCreator(): () -> T {
        return creator
    }
}
