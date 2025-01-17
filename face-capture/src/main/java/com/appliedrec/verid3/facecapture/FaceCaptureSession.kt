package com.appliedrec.verid3.facecapture

import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.appliedrec.verid3.common.Bearing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class FaceCaptureSession(val settings: FaceCaptureSessionSettings, sessionModuleFactories: FaceCaptureSessionModuleFactories = FaceCaptureSessionModuleFactories.defaultInstance): SessionFaceTrackingDelegate {

    val id: String = UUID.randomUUID().toString()
    val faceTrackingResult: SharedFlow<FaceTrackingResult>
        get() = _faceTrackingResult
    val result: StateFlow<FaceCaptureSessionResult?>
        get() = _result
    var resultCallback: ((FaceCaptureSessionResult) -> Unit)? = null
    private val _result: MutableStateFlow<FaceCaptureSessionResult?> = MutableStateFlow(null)
    private val _faceTrackingResult: MutableSharedFlow<FaceTrackingResult> = MutableStateFlow(FaceTrackingResult.Created(Bearing.STRAIGHT))
    private val faceTracking: SessionFaceTracking =
        SessionFaceTracking(sessionModuleFactories.createFaceDetection(), settings)
    private var sessionTask: Job? = null
    private var input: FlowCollector<FaceCaptureSessionImageInput>? = null
    private val inputFlow: MutableSharedFlow<FaceCaptureSessionImageInput> = MutableSharedFlow()
//    private var pluginFutures: List<Deferred<out Pair<String, TaskResults<out Any?>>>> = emptyList()
    private val faceTrackingResultTransformers: List<FaceTrackingResultTransformer>

    init {
        if (SecurityInfo.isEmulator()) {
            throw Exception("Session cannot run in an emulator")
        }
        this.faceTracking.delegate = this
        this.faceTrackingResultTransformers = sessionModuleFactories.createFaceTrackingResultTransformers()
        this.sessionTask = CoroutineScope(Dispatchers.Default).launch {
            val capturedFaces = mutableListOf<CapturedFace>()
            var result: FaceCaptureSessionResult
            val plugins: List<FaceTrackingPlugin<*>> = sessionModuleFactories.createFaceTrackingPlugins()
            try {
                var keepCollecting = true
                this@FaceCaptureSession.inputFlow
                    .takeWhile { isActive && keepCollecting }
                    .buffer(1, BufferOverflow.DROP_OLDEST)
                    .collect { imageInput ->
                        if (imageInput.time > settings.maxDuration) {
                            throw Exception("Session timed out")
                        }
                        val faceTrackingResult = faceTracking.trackFace(imageInput)
                        _faceTrackingResult.emit(faceTrackingResult)
                        plugins.forEach { plugin ->
                            plugin.submitFaceTrackingResult(faceTrackingResult)
                        }
                        faceTrackingResult.capturedFace?.let { capturedFace ->
                            capturedFaces.add(capturedFace)
                            Log.d("Ver-ID session", "Added captured face")
                            if (capturedFaces.size >= settings.faceCaptureCount) {
                                Log.d("Ver-ID session", "Stop collecting faces")
                                keepCollecting = false
                                return@collect
                            }
                        }
                    }
                Log.d("Ver-ID session", "Stopped collecting faces")
                if (!isActive || capturedFaces.size < settings.faceCaptureCount) {
                    finishSession()
                    return@launch
                }
//                val metadata = pluginFutures.associate { it.await() }
                val metadata = plugins.associate { it.stop() }
                finishSession()
                result = FaceCaptureSessionResult.Success(capturedFaces, metadata)
            } catch (e: Exception) {
                if (!isActive) {
                    finishSession()
                    return@launch
                }
//                val metadata = pluginFutures.associate { it.await() }
                val metadata = plugins.associate { it.stop() }
                finishSession()
                result = FaceCaptureSessionResult.Failure(capturedFaces, metadata, e)
            }
            Log.d("Ver-ID session", "Will dispatch on main, current thread ${Thread.currentThread().name}")
            try {
                withContext(Dispatchers.Main) {
                    Log.d("Ver-ID session", "Emit session result")
                    _result.value = result
                    resultCallback?.invoke(result)
                }
                this@FaceCaptureSession.sessionTask?.cancel()
                this@FaceCaptureSession.sessionTask = null
            } catch (e: Exception) {
                Log.e("Ver-ID session", "Failed to dispatch on main", e)
            }
            Log.d("Ver-ID session", "After dispatching on main, current thread ${Thread.currentThread().name}")
        }
    }

    fun cancel() {
        MainScope().launch {
            if (result.value == null) {
//                _result.emit(FaceCaptureSessionResult.Cancelled())
                _result.value = FaceCaptureSessionResult.Cancelled()
                resultCallback?.invoke(FaceCaptureSessionResult.Cancelled())
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            finishSession()
        }
    }

    suspend fun submitImageInput(imageInput: FaceCaptureSessionImageInput) {
        if (sessionTask == null || sessionTask?.isActive != true) {
            return
        }
        this.inputFlow.emit(imageInput)
    }

    private suspend fun finishSession() {
        Log.d("Ver-ID session", "Finish session")
        this.input?.let { currentCoroutineContext().cancel(null) }
        this.input = null
//        this.pluginFutures.forEach { it.cancel() }
    }

    override fun transformFaceResult(faceTrackingResult: FaceTrackingResult): FaceTrackingResult {
        return if (this.faceTrackingResultTransformers.isEmpty()) {
            if (faceTrackingResult is FaceTrackingResult.FaceAligned) {
                FaceTrackingResult.FaceCaptured(faceTrackingResult.requestedBearing, faceTrackingResult.expectedFaceBounds!!, faceTrackingResult.input!!, faceTrackingResult.face!!, faceTrackingResult.smoothedFace!!)
            } else {
                faceTrackingResult
            }
        } else {
            this.faceTrackingResultTransformers.fold(faceTrackingResult) { result, transformer ->
                transformer.transformFaceResult(result)
            }
        }
    }
}