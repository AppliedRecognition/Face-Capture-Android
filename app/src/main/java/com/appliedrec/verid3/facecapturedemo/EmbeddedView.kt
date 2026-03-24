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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.appliedrec.verid3.facecapture.ui.FaceCaptureView

@Composable
fun EmbeddedView(
    title: String,
    description: String,
    navigationController: NavController,
    resultViewModel: FaceCaptureResultViewModel,
    viewModel: EmbeddedViewModel = viewModel()
) {
    val isCapturing by viewModel.isCapturing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.captureResult.collect { result ->
            val resultId = resultViewModel.saveResult(result)
            navigationController.navigate("sessionResult/$resultId")
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isCapturing) {
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
                Button(onClick = { viewModel.startCapture() }) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Camera",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
                    session = viewModel.session,
                    configuration = viewModel.faceCaptureViewConfiguration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                ) { result ->
                    viewModel.onCaptureResult(result)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Button(onClick = { viewModel.cancelCapture() }) {
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}
