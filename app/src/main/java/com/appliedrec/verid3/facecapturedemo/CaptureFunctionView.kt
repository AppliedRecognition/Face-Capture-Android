package com.appliedrec.verid3.facecapturedemo

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.appliedrec.livenessdetection.spoofdevice.SpoofDeviceDetector
import com.appliedrec.verid3.facecapture.FaceCapture
import com.appliedrec.verid3.facecapture.FaceCaptureSessionModuleFactories
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingPlugin
import com.appliedrec.verid3.facecapture.LivenessDetectionPlugin
import com.appliedrec.verid3.facecapture.ui.FaceCaptureConfiguration
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewConfiguration
import com.appliedrec.verid3.facedetection.mp.FaceDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CaptureFunctionView(
    title: String,
    description: String,
    navigationController: NavController
) {
    val context = LocalContext.current as? ComponentActivity ?: return
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val resultViewModel: FaceCaptureResultViewModel = viewModel()
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppBarWithTips(title, navigationController)
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(description, color = MaterialTheme.colorScheme.onBackground)
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = Color.Gray
            )
            Button(onClick = {
                scope.launch {
                    val livenessDetectionPlugin = LivenessDetectionPlugin(arrayOf(
                        SpoofDeviceDetector(context)
                    ))
                    val configuration = FaceCaptureConfiguration(
                        FaceCaptureSessionSettings().apply {
                            this.faceCaptureCount = if (sharedPreferences.enableActiveLiveness) 2 else 1
                        },
                        FaceCaptureViewConfiguration(context = context, useBackCamera = sharedPreferences.useBackCamera),
                        FaceCaptureSessionModuleFactories(
                            { FaceDetection(context) },
                            { listOf(livenessDetectionPlugin as FaceTrackingPlugin<Any>) },
                            { emptyList() }
                        )
                    )
                    val result = FaceCapture.captureFaces(context, configuration)
                    if (result is FaceCaptureSessionResult.Cancelled) {
                        return@launch
                    }
                    val resultId = resultViewModel.saveResult(result)
                    withContext(Dispatchers.Main) {
                        navigationController.navigate("sessionResult/${resultId}")
                    }
                }
            }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                Text("Start capture", color = Color.White)
            }
        }
    }
}