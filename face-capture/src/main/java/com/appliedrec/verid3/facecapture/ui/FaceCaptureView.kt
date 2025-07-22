package com.appliedrec.verid3.facecapture.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
    var serialNumber: ULong = 0u
    var startTime: Long? = null
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
                val elapsed = now - startTime!!
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
        if (faceTrackingResult is FaceTrackingResult.Started) {
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
            val expectedFaceBounds =
                session.settings.expectedFaceBoundsInSize(maxWidth.toFloat(), maxHeight.toFloat())
            val scaledFaceTrackingResult = faceTrackingResult.scaledToViewSize(
                maxWidth.toFloat(),
                maxHeight.toFloat(),
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
            if (faceTrackingResult is FaceTrackingResult.Started && secondsRemainingToStart > 0) {
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
    val targetScaleX = remember { mutableFloatStateOf(1f) }
    val targetScaleY = remember { mutableFloatStateOf(1f) }
    val targetTranslationX = remember { mutableFloatStateOf(0f) }
    val targetTranslationY = remember { mutableFloatStateOf(0f) }
    val animatedScaleX by animateFloatAsState(
        targetValue = targetScaleX.floatValue,
        label = "scaleX")
    val animatedScaleY by animateFloatAsState(
        targetValue = targetScaleY.floatValue,
        label = "scaleY")
    val animatedTranslationX by animateFloatAsState(
        targetValue = targetTranslationX.floatValue,
        label = "translationX"
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = targetTranslationY.floatValue,
        label = "translationY"
    )

    return {
        when (scaledFaceTrackingResult) {
            is FaceTrackingResult.FaceAligned, is FaceTrackingResult.FaceFixed, is FaceTrackingResult.FaceMisaligned, is FaceTrackingResult.FaceCaptured -> {
                if (maxWidth != 0.dp && maxHeight != 0.dp && scaledFaceTrackingResult.expectedFaceBounds != null && scaledFaceTrackingResult.smoothedFace != null) {
                    val faceBounds = sessionSettings.expectedFaceBoundsInSize(maxWidth.toFloat(), maxHeight.toFloat())
                    val matrix = Matrix().apply {
                        setRectToRect(
                            scaledFaceTrackingResult.smoothedFace.bounds,
                            faceBounds,
                            Matrix.ScaleToFit.FILL
                        )
                    }
                    val matrixValues = FloatArray(9)
                    matrix.getValues(matrixValues)
                    transformOrigin = TransformOrigin(0f, 0f)
                    targetScaleX.floatValue = matrixValues[Matrix.MSCALE_X]
                    targetScaleY.floatValue = matrixValues[Matrix.MSCALE_Y]
                    targetTranslationX.floatValue = matrixValues[Matrix.MTRANS_X]
                    targetTranslationY.floatValue = matrixValues[Matrix.MTRANS_Y]
                } else {
                    targetScaleX.floatValue = 1f
                    targetScaleY.floatValue = 1f
                    targetTranslationX.floatValue = 0f
                    targetTranslationY.floatValue = 0f
                }
            }
            else -> {
                targetScaleX.floatValue = 1f
                targetScaleY.floatValue = 1f
                targetTranslationX.floatValue = 0f
                targetTranslationY.floatValue = 0f
            }
        }
        scaleX = animatedScaleX
        scaleY = animatedScaleY
        translationX = animatedTranslationX
        translationY = animatedTranslationY
    }
}

fun Dp.toFloat(): Float {
    return Resources.getSystem().displayMetrics.density * value
}