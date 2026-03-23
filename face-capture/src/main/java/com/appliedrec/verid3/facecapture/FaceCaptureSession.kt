package com.appliedrec.verid3.facecapture

import android.graphics.RectF
import android.util.Log
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.FaceDetection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

interface FaceCaptureSession {
    val settings: FaceCaptureSessionSettings
    val faceTrackingResult: SharedFlow<FaceTrackingResult>
    val result: StateFlow<FaceCaptureSessionResult?>
    fun start()
    fun cancel()
}

@Suppress("FunctionName")
fun FaceCaptureSession(
    settings: FaceCaptureSessionSettings,
    createFaceDetection: suspend () -> FaceDetection,
    createFaceTrackingPlugins: suspend () -> List<FaceTrackingPlugin<*>> = { emptyList() },
    createFaceTrackingResultTransformers: suspend () -> List<FaceTrackingResultTransformer> = { emptyList() }
): FaceCaptureSession = FaceCaptureSessionImpl(
    settings,
    createFaceDetection,
    createFaceTrackingPlugins,
    createFaceTrackingResultTransformers
)

internal suspend fun FaceCaptureSession.submitImageInput(imageInput: FaceCaptureSessionImageInput) {
    (this as? FaceCaptureSessionImpl)?.submitImageInput(imageInput)
}

internal class FaceCaptureSessionImpl(
    override val settings: FaceCaptureSessionSettings,
    private val createFaceDetection: suspend () -> FaceDetection,
    private val createFaceTrackingPlugins: suspend () -> List<FaceTrackingPlugin<*>> = { emptyList() },
    private val createFaceTrackingResultTransformers: suspend () -> List<FaceTrackingResultTransformer> = { emptyList() }
) : FaceCaptureSession {

    private val id: String = UUID.randomUUID().toString()
    override val faceTrackingResult: SharedFlow<FaceTrackingResult>
        get() = _faceTrackingResult
    override val result: StateFlow<FaceCaptureSessionResult?>
        get() = _result
    private val _result: MutableStateFlow<FaceCaptureSessionResult?> =
        MutableStateFlow(null)
    private val _faceTrackingResult: MutableSharedFlow<FaceTrackingResult> =
        MutableStateFlow(FaceTrackingResult.Created(Bearing.STRAIGHT))

    private var sessionTask: Job? = null
    private val inputFlow: MutableSharedFlow<FaceCaptureSessionImageInput> = MutableSharedFlow()
    private val cancellationRequested = AtomicBoolean(false)
    private val started = AtomicBoolean(false)

    init {
        if (SecurityInfo.isEmulator()) {
            throw Exception("Session cannot run in an emulator")
        }
    }

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        this.sessionTask = CoroutineScope(Dispatchers.Default).launch {
            val faceDetection = createFaceDetection()
            val transformers = createFaceTrackingResultTransformers()
            val faceTracking = SessionFaceTracking(faceDetection, settings)
            faceTracking.delegate = DefaultFaceTrackingResultTransformDelegate(transformers)
            val capturedFaces = mutableListOf<CapturedFace>()
            var result: FaceCaptureSessionResult? = null
            val plugins: List<FaceTrackingPlugin<*>> =
                createFaceTrackingPlugins()
            var error: Throwable? = null
            try {
                var keepCollecting = true
                this@FaceCaptureSessionImpl.inputFlow
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
                    cancellationRequested.get() -> FaceCaptureSessionResult.Cancelled
                    error != null -> FaceCaptureSessionResult.Failure(capturedFaces, metadata, error!!)
                    plugins.any { it.hasException } -> {
                        val exception = FaceTrackingPluginException(plugins)
                        FaceCaptureSessionResult.Failure(capturedFaces, metadata, exception)
                    }
                    capturedFaces.size < settings.faceCaptureCount -> FaceCaptureSessionResult.Cancelled
                    else -> FaceCaptureSessionResult.Success(capturedFaces, metadata)
                }
            }
            try {
                withContext(Dispatchers.Main) {
                    if (_result.value == null) {
                        _result.value = result
                    }
                }
                this@FaceCaptureSessionImpl.sessionTask?.cancel()
                this@FaceCaptureSessionImpl.sessionTask = null
            } catch (e: Exception) {
                Log.e("Ver-ID session", "Failed to dispatch on main", e)
            }
        }
    }

    override fun cancel() {
        cancellationRequested.set(true)
        sessionTask?.cancel()
    }

    internal suspend fun submitImageInput(imageInput: FaceCaptureSessionImageInput) {
        if (sessionTask == null || sessionTask?.isActive != true) {
            return
        }
        this.inputFlow.emit(imageInput)
    }
}
