package com.appliedrec.verid3.facecapture

import android.graphics.RectF
import android.util.Log
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.FaceDetection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class FaceCaptureSession(
    val settings: FaceCaptureSessionSettings,
    createFaceDetection: suspend () -> FaceDetection,
    createFaceTrackingPlugins: suspend () -> List<FaceTrackingPlugin<*>> = { emptyList() },
    createFaceTrackingResultTransformers: suspend () -> List<FaceTrackingResultTransformer> = { emptyList() }
): SessionFaceTrackingDelegate {

    val id: String = UUID.randomUUID().toString()
    val faceTrackingResult: SharedFlow<FaceTrackingResult>
        get() = _faceTrackingResult
    val result: StateFlow<FaceCaptureSessionResult?>
        get() = _result
    var resultCallback: ((FaceCaptureSessionResult) -> Unit)? = null
    private val _result: MutableStateFlow<FaceCaptureSessionResult?> =
        MutableStateFlow(null)
    private val _faceTrackingResult: MutableSharedFlow<FaceTrackingResult> =
        MutableStateFlow(FaceTrackingResult.Created(Bearing.STRAIGHT))

    private var sessionTask: Job? = null
    private val inputFlow: MutableSharedFlow<FaceCaptureSessionImageInput> = MutableSharedFlow()
    private val faceTrackingResultTransformers: MutableList<FaceTrackingResultTransformer> =
        mutableListOf()
    private val cancellationRequested = AtomicBoolean(false)

    init {
        if (SecurityInfo.isEmulator()) {
            throw Exception("Session cannot run in an emulator")
        }
        this.sessionTask = CoroutineScope(Dispatchers.Default).launch {
            val faceDetection = createFaceDetection()
            val faceTracking = SessionFaceTracking(faceDetection, settings)
            faceTracking.delegate = this@FaceCaptureSession
            faceTrackingResultTransformers.clear()
            faceTrackingResultTransformers.addAll(
                createFaceTrackingResultTransformers()
            )
            val capturedFaces = mutableListOf<CapturedFace>()
            var result: FaceCaptureSessionResult? = null
            val plugins: List<FaceTrackingPlugin<*>> =
                createFaceTrackingPlugins()
            var error: Throwable? = null
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
                            if (capturedFaces.size >= settings.faceCaptureCount) {
                                keepCollecting = false
                                return@collect
                            }
                        }
                    }
            } catch (e: CancellationException) {
                if (!cancellationRequested.get()) {
                    error = e
                }
            } catch (e: Exception) {
                error = e
            } finally {
                val metadata = withContext(NonCancellable) {
                    try {
                        _faceTrackingResult.emit(
                            FaceTrackingResult.Waiting(
                                Bearing.STRAIGHT,
                                RectF(0f, 0f, 0f, 0f)
                            )
                        )
                    } catch (e: Exception) {
                        // Ignore dispatch failures while shutting down
                    }
                    plugins.associate { it.stop() }
                }
                faceDetection.close()
                result = when {
                    cancellationRequested.get() -> FaceCaptureSessionResult.Cancelled()
                    error != null -> FaceCaptureSessionResult.Failure(capturedFaces, metadata, error!!)
                    plugins.any { it.hasException } -> {
                        val exception = FaceTrackingPluginException(plugins)
                        FaceCaptureSessionResult.Failure(capturedFaces, metadata, exception)
                    }
                    capturedFaces.size < settings.faceCaptureCount -> FaceCaptureSessionResult.Cancelled()
                    else -> FaceCaptureSessionResult.Success(capturedFaces, metadata)
                }
            }
            try {
                withContext(Dispatchers.Main) {
                    if (_result.value == null) {
                        _result.value = result
                        resultCallback?.invoke(result!!)
                    }
                }
                this@FaceCaptureSession.sessionTask?.cancel()
                this@FaceCaptureSession.sessionTask = null
            } catch (e: Exception) {
                Log.e("Ver-ID session", "Failed to dispatch on main", e)
            }
        }
    }

    fun cancel() {
        cancellationRequested.set(true)
        MainScope().launch {
            sessionTask?.cancel()
        }
    }

    suspend fun submitImageInput(imageInput: FaceCaptureSessionImageInput) {
        if (sessionTask == null || sessionTask?.isActive != true) {
            return
        }
        this.inputFlow.emit(imageInput)
    }

    override fun transformFaceResult(faceTrackingResult: FaceTrackingResult): FaceTrackingResult {
        return if (this.faceTrackingResultTransformers.isEmpty()) {
            if (faceTrackingResult is FaceTrackingResult.FaceAligned) {
                FaceTrackingResult.FaceCaptured(
                    faceTrackingResult.requestedBearing,
                    faceTrackingResult.expectedFaceBounds!!,
                    faceTrackingResult.input!!,
                    faceTrackingResult.face!!,
                    faceTrackingResult.smoothedFace!!
                )
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
