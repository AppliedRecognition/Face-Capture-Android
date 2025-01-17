package com.appliedrec.verid3.facecapturedemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult

@Composable
fun FaceCaptureResultView(resultId: String)  {

    val resultViewModel: FaceCaptureResultViewModel = viewModel()
    resultViewModel.deleteResult(resultId)?.let { result ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AppBar(titleFromResult(result))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                result.capturedFaces.firstOrNull()?.let { capturedFace ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FaceImage(
                            imageBitmap = capturedFace.faceImage.asImageBitmap(),
                            width = 200.dp
                        )
                    }
                }
                if (result is FaceCaptureSessionResult.Failure) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            result.error.message ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                result.metadata.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(key, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(value.summary, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}

fun titleFromResult(result: FaceCaptureSessionResult): String {
    return when (result) {
        is FaceCaptureSessionResult.Success -> "Succeeded"
        is FaceCaptureSessionResult.Failure -> "Failed"
        is FaceCaptureSessionResult.Cancelled -> "Cancelled"
    }
}

@Composable
fun FaceImage(imageBitmap: ImageBitmap, width: Dp) {
    Image(
        bitmap = imageBitmap,
        contentDescription = "Captured face",
        modifier = Modifier
            .width(200.dp)
            .aspectRatio(imageBitmap.width.toFloat() / imageBitmap.height.toFloat())
            .clip(RoundedCornerShape(16.dp))
    )
}