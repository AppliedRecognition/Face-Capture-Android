package com.appliedrec.verid3.facecapture.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.facecapture.AngleBearingEvaluation
import com.appliedrec.verid3.facecapture.FaceCaptureSession
import com.appliedrec.verid3.facecapture.FaceCaptureSessionImageInput
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingResult
import com.appliedrec.verid3.facecapture.R
import com.appliedrec.verid3.facecapture.SecurityInfo
import com.appliedrec.verid3.facecapture.copyWithExpectedBounds
import com.appliedrec.verid3.facecapture.scaledToViewSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun FaceCaptureView(
    session: FaceCaptureSession,
    modifier: Modifier = Modifier,
    configuration: FaceCaptureViewConfiguration = FaceCaptureViewConfiguration(LocalContext.current),
    preview: @Composable (imageFlow: MutableSharedFlow<IImage>, width: Dp, height: Dp, scaledFaceTrackingResult: FaceTrackingResult, cameraTransform: GraphicsLayerScope.() -> Unit) -> Unit = { imageFlow, width, height, scaledFaceTrackingResult, cameraTransform ->
        CameraPreviewView(
            imageSharedFlow = imageFlow,
            modifier = Modifier
                .size(
                    width = width,
                    height = height
                )
                .clip(
                    FaceOval(scaledFaceTrackingResult)
                )
                .graphicsLayer(cameraTransform),
            useBackCamera = configuration.useBackCamera
        )
    },
    onResult: (FaceCaptureSessionResult) -> Unit = {}
) {
    val context = LocalContext.current
    if (SecurityInfo.isDeviceRooted(context)) {
        Text("Rooted device detected")
        return
    }
    session.resultCallback = onResult
    val imageFlow = remember {
        MutableSharedFlow<IImage>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    var serialNumber: ULong = remember(session) { 0uL }
    var startTime: Long? = remember(session) { null }
    var cameraPermissionGranted by remember { mutableStateOf<Boolean?>(null) }
    var countdownSeconds by remember { mutableIntStateOf(-1) }
    var secondsRemainingToStart by remember { mutableIntStateOf(countdownSeconds) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        cameraPermissionGranted = isGranted
    }
    val faceTrackingResult by session.faceTrackingResult.collectAsState(initial = FaceTrackingResult.Created(Bearing.STRAIGHT))
    LaunchedEffect(key1 = imageFlow, key2 = cameraPermissionGranted) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            cameraPermissionGranted = true
            imageFlow.onEach { image ->
                val now = System.currentTimeMillis()
                if (startTime == null) {
                    startTime = now
                }
                val elapsed = now - startTime
                session.submitImageInput(FaceCaptureSessionImageInput(serialNumber++, elapsed, image))
            }.launchIn(this)
        }
    }
    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds > 0) {
            (0..countdownSeconds).asFlow()
                .flatMapConcat { delay(1000L); flowOf(countdownSeconds - it) }
                .collect { remaining ->
                    secondsRemainingToStart = remaining
                }
        }
    }
    LaunchedEffect(faceTrackingResult) {
        if (faceTrackingResult is FaceTrackingResult.Launched) {
            countdownSeconds = session.settings.countdownSeconds
        }
    }
    if (cameraPermissionGranted == null) {
        Text(stringResource(R.string.loading))
        return
    }
    if (cameraPermissionGranted == false) {
        Text(stringResource(R.string.camera_permission_denied))
        return
    }
    val angleBearingEvaluation = AngleBearingEvaluation(session.settings)
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            if (faceTrackingResult is FaceTrackingResult.Waiting) {
                Column {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                    )
                    Text(stringResource(R.string.finishing_capture))
                }
                return@BoxWithConstraints
            }
            val density = LocalDensity.current.density
            val expectedFaceBounds =
                session.settings.expectedFaceBoundsInSize(maxWidth.toFloat(density), maxHeight.toFloat(density))
            val scaledFaceTrackingResult = faceTrackingResult.scaledToViewSize(
                maxWidth.toFloat(density),
                maxHeight.toFloat(density),
                expectedFaceBounds,
                !configuration.useBackCamera
            )
            val prompt = configuration.textPromptProvider(scaledFaceTrackingResult)

            val cameraTransform: GraphicsLayerScope.() -> Unit = createCameraTransform(
                scaledFaceTrackingResult = scaledFaceTrackingResult,
                sessionSettings = session.settings,
                maxWidth = maxWidth,
                maxHeight = maxHeight
            )
            LaunchedEffect(faceTrackingResult) {
                configuration.textPrompt.emit(prompt)
            }
            preview(imageFlow, maxWidth, maxHeight, scaledFaceTrackingResult, cameraTransform)
            if (faceTrackingResult is FaceTrackingResult.Created) {
                Column {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                    )
                    Text(stringResource(R.string.loading))
                }
                return@BoxWithConstraints
            }
            if (faceTrackingResult is FaceTrackingResult.Starting && secondsRemainingToStart > 0) {
                Text(
                    text = "%d".format(secondsRemainingToStart),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = ((faceTrackingResult.expectedFaceBounds?.height() ?: 96f) * 0.5f).sp,
                    color = Color.White
                )
            }
            FaceArrow(
                faceTrackingResult = scaledFaceTrackingResult,
                angleBearingEvaluation = angleBearingEvaluation,
                isMirrored = !configuration.useBackCamera
            )
            if (configuration.showTextPrompts) {
                Text(
                    text = prompt,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun createCameraTransform(
    scaledFaceTrackingResult: FaceTrackingResult,
    sessionSettings: FaceCaptureSessionSettings,
    maxWidth: Dp,
    maxHeight: Dp
): GraphicsLayerScope.() -> Unit {
    val density = LocalDensity.current.density
    val viewWidthPx = maxWidth.toFloat(density)
    val viewHeightPx = maxHeight.toFloat(density)

    // Pure target, recomputed on recomposition only
    val target by remember(
        scaledFaceTrackingResult,
        sessionSettings,
        viewWidthPx,
        viewHeightPx
    ) {
        derivedStateOf {
            if (maxWidth == 0.dp || maxHeight == 0.dp) {
                CameraTransformTarget()
            } else {
                val expected = sessionSettings.expectedFaceBoundsInSize(viewWidthPx, viewHeightPx)
                val withExpected = when {
                    scaledFaceTrackingResult.expectedFaceBounds == null ->
                        scaledFaceTrackingResult.copyWithExpectedBounds(expected)
                    else -> scaledFaceTrackingResult
                }
                computeCameraTransformTarget(withExpected, viewWidthPx, viewHeightPx)
            }
        }
    }

    val transformAnimatable = remember { Animatable(CameraTransformTarget(), CameraTransformTargetVectorConverter) }

    val springSpec = remember {
        spring<CameraTransformTarget>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    // Deadband to avoid buzzing from tiny measurement noise
    val epsScale = 0.001f
    val epsTrans = 0.5f

    LaunchedEffect(target) {
        val current = transformAnimatable.value

        val close = current.scaleX.isCloseTo(target.scaleX, epsScale) &&
                    current.scaleY.isCloseTo(target.scaleY, epsScale) &&
                    current.translationX.isCloseTo(target.translationX, epsTrans) &&
                    current.translationY.isCloseTo(target.translationY, epsTrans)

        if (close) {
            transformAnimatable.snapTo(target)
        } else {
            transformAnimatable.animateTo(target, springSpec)
        }
    }

    // Layer lambda: reads only, no state writes
    return {
        transformOrigin = TransformOrigin(0f, 0f)
        this.scaleX = transformAnimatable.value.scaleX
        this.scaleY = transformAnimatable.value.scaleY
        this.translationX = transformAnimatable.value.translationX
        this.translationY = transformAnimatable.value.translationY
    }
}

@Immutable
private data class CameraTransformTarget(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f
)

private val CameraTransformTargetVectorConverter = TwoWayConverter<CameraTransformTarget, AnimationVector4D>(
    convertToVector = { t ->
        AnimationVector4D(t.scaleX, t.scaleY, t.translationX, t.translationY)
    },
    convertFromVector = { v ->
        CameraTransformTarget(
            scaleX = v.v1,
            scaleY = v.v2,
            translationX = v.v3,
            translationY = v.v4
        )
    }
)

private fun computeCameraTransformTarget(
    scaledFaceTrackingResult: FaceTrackingResult,
    viewWidthPx: Float,
    viewHeightPx: Float
): CameraTransformTarget {
    val smoothedFace = scaledFaceTrackingResult.smoothedFace ?: return CameraTransformTarget()
    val expected = scaledFaceTrackingResult.expectedFaceBounds ?: return CameraTransformTarget()

    return if ((scaledFaceTrackingResult is FaceTrackingResult.Starting)
        || (scaledFaceTrackingResult is FaceTrackingResult.Started)
        || (scaledFaceTrackingResult is FaceTrackingResult.FaceFound)) {
        val scale = expected.width() / smoothedFace.bounds.width()
        val faceFitsInView = smoothedFace.bounds.left > 0
                && smoothedFace.bounds.right < viewWidthPx
                && smoothedFace.bounds.top > 0
                && smoothedFace.bounds.bottom < viewHeightPx
        if (scale < 1f && faceFitsInView) {
            CameraTransformTarget(
                scaleX = scale,
                scaleY = scale,
                translationX = (viewWidthPx - viewWidthPx * scale) * 0.5f,
                translationY = (viewHeightPx - viewHeightPx * scale) * 0.5f
            )
        } else {
            CameraTransformTarget()
        }
    } else {
        val matrixValues = FloatArray(9)
        val matrix = Matrix().apply {
            setRectToRect(
                smoothedFace.bounds,
                expected,
                Matrix.ScaleToFit.FILL
            )
        }
        matrix.getValues(matrixValues)
        CameraTransformTarget(
            scaleX = matrixValues[Matrix.MSCALE_X],
            scaleY = matrixValues[Matrix.MSCALE_Y],
            translationX = matrixValues[Matrix.MTRANS_X],
            translationY = matrixValues[Matrix.MTRANS_Y]
        )
    }
}

private fun Float.isCloseTo(other: Float, epsilon: Float): Boolean = abs(this - other) <= epsilon

fun Dp.toFloat(density: Float): Float {
    return density * value
}