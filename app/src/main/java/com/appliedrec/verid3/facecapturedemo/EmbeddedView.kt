package com.appliedrec.verid3.facecapturedemo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.ui.FaceCaptureView

@Composable
fun EmbeddedView(
    title: String,
    description: String,
    navigationController: NavController
) {
    val resultViewModel: FaceCaptureResultViewModel = viewModel()
    val context = LocalContext.current
    val setup = Setup(context)
    val session = setup.faceCaptureSession
    var isCapturingFace by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isCapturingFace) {
            AppBarWithTips(title, navigationController)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(description, color = MaterialTheme.colorScheme.onBackground)
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Button(onClick = {
                    isCapturingFace = true
                }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                    Text("Start capture", color = Color.White)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                FaceCaptureView(
                    session = session,
                    configuration = setup.faceCaptureViewConfiguration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                ) {
                    isCapturingFace = false
                    if (it is FaceCaptureSessionResult.Cancelled) {
                        return@FaceCaptureView
                    }
                    val resultId = resultViewModel.saveResult(it)
                    navigationController.navigate("sessionResult/${resultId}")
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Button(onClick = {
                    isCapturingFace = false
                    session.cancel()
                }) {
                    Icon(Icons.Filled.Cancel, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}