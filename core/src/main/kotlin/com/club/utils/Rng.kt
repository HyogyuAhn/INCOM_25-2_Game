package com.club.utils

import java.util.*

class Rng(seed: Long = System.currentTimeMillis()) {
    private val random = Random(seed)
    
    fun nextFloat(): Float = random.nextFloat()
    fun nextInt(bound: Int): Int = random.nextInt(bound)
    fun nextInt(min: Int, max: Int): Int = random.nextInt(max - min + 1) + min
    fun nextBoolean(): Boolean = random.nextBoolean()
    
    fun nextFloat(min: Float, max: Float): Float = min + (max - min) * nextFloat()
    
    fun <T> choose(items: Array<T>, weights: FloatArray): T {
        require(items.isNotEmpty()) { "items must not be empty" }
        require(items.size == weights.size) { "items and weights size mismatch" }
        var total = 0f
        for (w in weights) total += w
        var r = nextFloat(0f, total)
        for (i in items.indices) {
            r -= weights[i]
            if (r <= 0f) return items[i]
        }
        return items.last()
    }
    
    companion object {
        fun dailySeed(): Long {
            val now = Calendar.getInstance()
            return (now.get(Calendar.YEAR) * 10000 + 
                   (now.get(Calendar.MONTH) + 1) * 100 + 
                   now.get(Calendar.DAY_OF_MONTH)).toLong()
        }
    }
}
