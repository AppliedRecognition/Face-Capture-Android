package com.appliedrec.verid3.facecapturedemo

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.appliedrec.verid3.facecapture.ui.FaceCaptureView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalView(
    title: String,
    description: String,
    navigationController: NavController,
    resultViewModel: FaceCaptureResultViewModel,
    viewModel: ModalViewModel = viewModel()
) {
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) sheetState.expand() else sheetState.hide()
    }

    LaunchedEffect(Unit) {
        viewModel.captureResult.collect { result ->
            val resultId = resultViewModel.saveResult(result)
            navigationController.navigate("sessionResult/$resultId")
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AppBarWithTips(title, navigationController)
        Column(modifier = Modifier.padding(16.dp)) {
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
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissSheet() },
                sheetState = sheetState
            ) {
                FaceCaptureView(
                    session = viewModel.session,
                    modifier = Modifier.fillMaxSize(),
                    configuration = viewModel.faceCaptureViewConfiguration
                ) { result ->
                    viewModel.onCaptureResult(result)
                }
            }
        }
    }
}
