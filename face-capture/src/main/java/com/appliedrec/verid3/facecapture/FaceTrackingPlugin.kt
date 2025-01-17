package com.appliedrec.verid3.facecapture

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class FaceTrackingPlugin<T> {
    abstract val name: String

    private val faceTrackingResultFlow = MutableSharedFlow<FaceTrackingResult>(extraBufferCapacity = 1)
    private var task: Job? = null
    private val results = mutableListOf<FaceTrackingPluginResult<T>>()
    private var isActive = true
    private val mutex = Mutex()

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
                            } catch (e: Exception) {
                                value = Result.failure(e)
                            }
                            value?.let {
                                val result =
                                    FaceTrackingPluginResult(input.serialNumber, input.time, it)
                                mutex.withLock {
                                    results.add(result)
                                }
                            }
                        }
                    }
            } catch (e: CancellationException) {
                // Do nothing
            } catch (e: Exception) {
                mutex.withLock {
                    results.add(FaceTrackingPluginResult(0UL, System.currentTimeMillis(), Result.failure(e)))
                }
            }
        }
    }

    suspend fun stop(): Pair<String, TaskResults<T>> {
        isActive = false
        task?.cancel()
        task = null
        val results = mutex.withLock { this.results.toList() }
        val summary = createSummaryFromResults(results)
        return Pair(name, TaskResults(summary, results))
    }

    suspend fun submitFaceTrackingResult(faceTrackingResult: FaceTrackingResult) {
//        Log.d("Ver-ID session", "Submitting face tracking result $faceTrackingResult to plugin: $name")
        faceTrackingResultFlow.emit(faceTrackingResult)
    }

    protected abstract suspend fun processFaceTrackingResult(faceTrackingResult: FaceTrackingResult): T?
    protected abstract suspend fun createSummaryFromResults(results: List<FaceTrackingPluginResult<T>>): String
}