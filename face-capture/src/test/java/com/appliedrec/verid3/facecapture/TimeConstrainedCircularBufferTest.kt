package com.appliedrec.verid3.facecapture

import org.junit.Assert
import org.junit.Test

class TimeConstrainedCircularBufferTest {

    @Test
    fun appendedElementIsRetrievable() {
        val buffer = TimeConstrainedCircularBuffer<Int>(5000)
        buffer.append(42)
        Assert.assertEquals(1, buffer.count)
        Assert.assertEquals(42, buffer.last)
        Assert.assertFalse(buffer.isEmpty)
    }

    @Test
    fun elementsExpireAfterDuration() {
        val buffer = TimeConstrainedCircularBuffer<Int>(50)
        buffer.append(1)
        Assert.assertFalse(buffer.isEmpty)
        Thread.sleep(100)
        buffer.append(2) // triggers removeExpiredElements
        Assert.assertEquals(1, buffer.count)
        Assert.assertEquals(2, buffer.last)
    }

    @Test
    fun hasRemovedElementsIsFalseInitially() {
        val buffer = TimeConstrainedCircularBuffer<Int>(5000)
        Assert.assertFalse(buffer.hasRemovedElements)
        buffer.append(1)
        Assert.assertFalse(buffer.hasRemovedElements)
    }

    @Test
    fun hasRemovedElementsSetAfterExpiry() {
        val buffer = TimeConstrainedCircularBuffer<Int>(50)
        buffer.append(1)
        Assert.assertFalse(buffer.hasRemovedElements)
        Thread.sleep(100)
        buffer.append(2) // triggers removal of element 1
        Assert.assertTrue(buffer.hasRemovedElements)
    }

    @Test
    fun clearResetsHasRemovedElements() {
        val buffer = TimeConstrainedCircularBuffer<Int>(50)
        buffer.append(1)
        Thread.sleep(100)
        buffer.append(2) // sets hasRemovedElements = true
        Assert.assertTrue(buffer.hasRemovedElements)
        buffer.clear()
        Assert.assertFalse(buffer.hasRemovedElements)
        Assert.assertTrue(buffer.isEmpty)
    }

    @Test
    fun allSatisfyReturnsTrueWhenAllMatch() {
        val buffer = TimeConstrainedCircularBuffer<Int>(5000)
        buffer.append(2)
        buffer.append(4)
        buffer.append(6)
        Assert.assertTrue(buffer.allSatisfy { it % 2 == 0 })
    }

    @Test
    fun allSatisfyReturnsFalseWhenOneDoesNot() {
        val buffer = TimeConstrainedCircularBuffer<Int>(5000)
        buffer.append(2)
        buffer.append(3)
        buffer.append(6)
        Assert.assertFalse(buffer.allSatisfy { it % 2 == 0 })
    }

    @Test
    fun suffixReturnsLastNElements() {
        val buffer = TimeConstrainedCircularBuffer<Int>(5000)
        buffer.append(1)
        buffer.append(2)
        buffer.append(3)
        buffer.append(4)
        val suffix = buffer.suffix(2)
        Assert.assertEquals(listOf(3, 4), suffix)
    }

    @Test
    fun getByIndexReturnsNullOutOfBounds() {
        val buffer = TimeConstrainedCircularBuffer<Int>(5000)
        buffer.append(1)
        Assert.assertNull(buffer[-1])
        Assert.assertNull(buffer[1])
    }
}
