package com.club.utils

class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val initialCapacity: Int = 10,
    private val maxCapacity: Int = 100
) {
    private val pool = mutableListOf<T>()
    private var createdCount = 0
    
    init {
        repeat(initialCapacity) {
            pool.add(factory())
            createdCount++
        }
    }
    
    fun obtain(): T {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else if (createdCount < maxCapacity) {
            createdCount++
            factory()
        } else {
            throw IllegalStateException("Object pool exhausted")
        }
    }
    
    fun free(obj: T) {
        if (pool.size < maxCapacity) {
            reset(obj)
            pool.add(obj)
        }
    }
    
    fun getPoolSize(): Int = pool.size
    fun getCreatedCount(): Int = createdCount
}
