package com.appliedrec.verid3.facecapturedemo

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult

@Composable
fun FaceCaptureResultView(
    resultId: String,
    resultViewModel: FaceCaptureResultViewModel
) {
    val context = LocalContext.current
    val shareState by resultViewModel.shareState.collectAsState()
    val result = remember(resultId) { resultViewModel.deleteResult(resultId) }

    LaunchedEffect(shareState) {
        when (val state = shareState) {
            is ShareState.Ready -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, state.uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Face capture result")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share face capture bundle"))
                resultViewModel.clearShareState()
            }
            is ShareState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                resultViewModel.clearShareState()
            }
            else -> {}
        }
    }

    result?.let { resultValue ->
        Column(modifier = Modifier.fillMaxWidth()) {
            AppBar(
                title = titleFromResult(resultValue),
                actions = {
                    if (resultValue !is FaceCaptureSessionResult.Cancelled) {
                        IconButton(
                            enabled = shareState is ShareState.Idle,
                            onClick = { resultViewModel.shareResult(resultValue) }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
            if (shareState is ShareState.Preparing) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = { Text("Preparing to share") },
                    text = {
                        Row {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Creating zip bundle...")
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                resultValue.capturedFaces.firstOrNull()?.let { capturedFace ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FaceImage(
                            imageBitmap = capturedFace.faceImage.asImageBitmap(),
                            width = 200.dp
                        )
                    }
                }
                if (resultValue is FaceCaptureSessionResult.Failure) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            resultValue.error.message ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                resultValue.metadata.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(key, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
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
