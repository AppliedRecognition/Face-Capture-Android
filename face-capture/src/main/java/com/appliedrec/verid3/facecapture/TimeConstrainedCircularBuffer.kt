package com.appliedrec.verid3.facecapture

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class TimeConstrainedCircularBuffer<T>(val duration: Long): Iterable<T> {

    private class BufferIterator<T>(private val items: List<T>): Iterator<T> {
        var index: Int = 0

        override fun hasNext(): Boolean {
            return index < items.size
        }

        override fun next(): T {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            return items[index++]
        }
    }

    private val buffer: MutableList<Pair<T,Long>> = mutableListOf()
    private var currentIndex: Int = 0
    var hasRemovedElements: Boolean = false
        private set
    private val lock = ReentrantLock()

    override fun iterator(): Iterator<T> {
        return BufferIterator(this.buffer.map { it.first })
    }

    fun append(element: T) {
        val timestamp = System.currentTimeMillis()
        lock.withLock {
            this@TimeConstrainedCircularBuffer.removeExpiredElements()
            this@TimeConstrainedCircularBuffer.buffer.add(Pair(element, timestamp))
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun removeFirst(): T? = lock.withLock {
        if (this@TimeConstrainedCircularBuffer.buffer.isEmpty()) {
            null
        } else {
            this@TimeConstrainedCircularBuffer.buffer.removeFirst().first
        }
    }

    operator fun get(index: Int): T? {
        return lock.withLock {
            if (index < 0 || index >= this@TimeConstrainedCircularBuffer.buffer.size) {
                null
            } else {
                this@TimeConstrainedCircularBuffer.buffer[index].first
            }
        }
    }

    val first: T?
        get() = lock.withLock {
            this@TimeConstrainedCircularBuffer.buffer.firstOrNull()?.first
        }

    val last: T?
        get() = lock.withLock {
            this@TimeConstrainedCircularBuffer.buffer.lastOrNull()?.first
        }

    val count: Int
        get() = lock.withLock {
            this@TimeConstrainedCircularBuffer.buffer.size
        }

    fun clear() = lock.withLock {
        this@TimeConstrainedCircularBuffer.hasRemovedElements = false
        this@TimeConstrainedCircularBuffer.buffer.clear()
    }

    val isEmpty: Boolean
        get() = lock.withLock {
            this@TimeConstrainedCircularBuffer.buffer.isEmpty()
        }

    fun allSatisfy(predicate: (T) -> Boolean): Boolean = lock.withLock {
        this@TimeConstrainedCircularBuffer.buffer.map { it.first }.all(predicate)
    }

    fun suffix(maxLength: Int): List<T> = lock.withLock {
        this@TimeConstrainedCircularBuffer.buffer.map { it.first }.takeLast(maxLength)
    }

    val oldestElementTimestamp: Long?
        get() = lock.withLock {
            this@TimeConstrainedCircularBuffer.buffer.firstOrNull()?.second
        }

    fun filter(predicate: (T) -> Boolean) = lock.withLock {
        this@TimeConstrainedCircularBuffer.buffer.map { it.first }.filter(predicate)
    }

    fun min(predicate: (T) -> Boolean) = lock.withLock {
        this@TimeConstrainedCircularBuffer.buffer.map { it.first }.minByOrNull(predicate)
    }

    fun max(predicate: (T) -> Boolean) = lock.withLock {
        this@TimeConstrainedCircularBuffer.buffer.map { it.first }.maxByOrNull(predicate)
    }

    val oldestElement: T?
        get() = lock.withLock {
            this@TimeConstrainedCircularBuffer.buffer.minByOrNull { it.second }?.first
        }

    private fun removeExpiredElements() {
        val currentTime = System.currentTimeMillis()
        val startCount = this.buffer.size
        this.buffer.removeAll { element ->
            val elapsedTime = currentTime - element.second
            elapsedTime >= this.duration
        }
        if (startCount > this.buffer.size) {
            this.hasRemovedElements = true
        }
    }
}