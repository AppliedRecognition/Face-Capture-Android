package com.appliedrec.verid3.facecapturedemo

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.appliedrec.verid3.spoofdevicedetection.cloud.SpoofDeviceDetection
import com.appliedrec.verid3.facecapture.FaceCaptureSession
import com.appliedrec.verid3.facecapture.FaceCaptureSessionModuleFactories
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingPlugin
import com.appliedrec.verid3.facecapture.LivenessDetectionPlugin
import com.appliedrec.verid3.facecapture.ui.FaceCaptureView
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewConfiguration
import com.appliedrec.verid3.facedetection.retinaface.FaceDetectionRetinaFace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalView(
    title: String,
    description: String,
    navigationController: NavController
) {
    val resultViewModel: FaceCaptureResultViewModel = viewModel()
    var showBottomSheet by remember {
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val setup = Setup(context)
    val session = setup.faceCaptureSession
    val result by session.result.collectAsState()

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }

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
                showBottomSheet = true
            }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                Text("Start capture", color = Color.White)
            }
        }
        if (showBottomSheet && result == null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    session.cancel()
                },
                sheetState = sheetState
            ) {
                FaceCaptureView(session = session, modifier = Modifier.fillMaxSize(), configuration = setup.faceCaptureViewConfiguration) {
                    showBottomSheet = false
                    if (it is FaceCaptureSessionResult.Cancelled) {
                        return@FaceCaptureView
                    }
                    val resultId = resultViewModel.saveResult(it)
                    navigationController.navigate("sessionResult/${resultId}")
                }
            }
        } else if (showBottomSheet) {
            showBottomSheet = false
        }
    }
}
