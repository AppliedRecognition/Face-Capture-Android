package com.appliedrec.verid3.facecapture

import android.graphics.RectF
import com.appliedrec.verid3.common.Bearing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class FaceTrackingPlugin<T> {
    abstract val name: String

    private val faceTrackingResultFlow = MutableSharedFlow<FaceTrackingResult>(extraBufferCapacity = 1)
    private var task: Job? = null
    private var isActive = true
    private val lock = ReentrantLock()
    private val _results = mutableListOf<FaceTrackingPluginResult<T>>()
    private var _stoppedTaskResults: TaskResults<T>? = null
    val results: List<FaceTrackingPluginResult<T>> get() = lock.withLock { Collections.unmodifiableList(_results) }
    val hasException: Boolean get() = results.any { it.result.isFailure }

    init {
        task = CoroutineScope(Dispatchers.Default).launch {
            try {
                faceTrackingResultFlow
                    .takeWhile { isActive }
                    .buffer(1, BufferOverflow.DROP_OLDEST)
                    .collect { faceTrackingResult ->
                        faceTrackingResult.input?.let { input ->
                            var value: Result<T>? = null
                            try {
                                processFaceTrackingResult(faceTrackingResult)?.let {
                                    value = Result.success(it)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                value = Result.failure(e)
                            }
                            value?.let {
                                val result =
                                    FaceTrackingPluginResult(input.serialNumber, input.time, it)
                                lock.withLock {
                                    _results.add(result)
                                }
                            }
                        }
                    }
            } catch (e: CancellationException) {
                // Do nothing
            } catch (e: Exception) {
                lock.withLock {
                    _results.add(FaceTrackingPluginResult(0UL, System.currentTimeMillis(), Result.failure(e)))
                }
            }
        }
    }

    suspend fun stop(): Pair<String, TaskResults<T>> {
        lock.withLock {
            _stoppedTaskResults?.let {
                return Pair(name, it)
            }
        }
        isActive = false
        faceTrackingResultFlow.emit(FaceTrackingResult.Waiting(Bearing.STRAIGHT,
            RectF(0f, 0f, 0f, 0f)
        ))
        task?.let {
            if (it.isActive) {
                it.join()
            } else {
                it.cancel()
            }
        }
        task = null
        val finalResult: FaceTrackingPluginResult<T>? = try {
            createFinalResult()?.let { value ->
                val lastResult = lock.withLock { _results.lastOrNull() }
                FaceTrackingPluginResult(
                    lastResult?.serialNumber ?: 0uL,
                    lastResult?.time ?: System.currentTimeMillis(),
                    Result.success(value)
                )
            }
        } catch (e: Exception) {
            val lastResult = lock.withLock { _results.lastOrNull() }
            FaceTrackingPluginResult(
                lastResult?.serialNumber ?: 0uL,
                lastResult?.time ?: System.currentTimeMillis(),
                Result.failure(e)
            )
        }
        finalResult?.let {
            lock.withLock {
                _results.add(it)
            }
        }
        val results = lock.withLock { _results.toList() }
        val summary = createSummaryFromResults(results)
        val taskResults = TaskResults(summary, results)
        lock.withLock {
            _stoppedTaskResults = taskResults
        }
        return Pair(name, taskResults)
    }

    suspend fun submitFaceTrackingResult(faceTrackingResult: FaceTrackingResult) {
//        Log.d("Ver-ID session", "Submitting face tracking result $faceTrackingResult to plugin: $name")
        faceTrackingResultFlow.emit(faceTrackingResult)
    }

    protected abstract suspend fun processFaceTrackingResult(faceTrackingResult: FaceTrackingResult): T?
    protected abstract suspend fun createSummaryFromResults(results: List<FaceTrackingPluginResult<T>>): String
    protected open suspend fun createFinalResult(): T? = null
}
