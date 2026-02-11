package com.appliedrec.verid3.facecapturedemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appliedrec.verid3.common.serialization.toBitmap
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.toExportJson
import com.appliedrec.videorecordingplugin.VideoRecordingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun FaceCaptureResultView(resultId: String)  {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPreparingShare by remember { mutableStateOf(false) }
    val resultViewModel: FaceCaptureResultViewModel = viewModel()
    val result = remember(resultId) { resultViewModel.deleteResult(resultId) }
    result?.let { resultValue ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AppBar(
                title = titleFromResult(resultValue),
                actions = {
                    if (resultValue !is FaceCaptureSessionResult.Cancelled) {
                        IconButton(
                            enabled = !isPreparingShare,
                            onClick = {
                                val json = resultValue.toExportJson()
                                val faceImages = resultValue.capturedFaces.map { it.image.toBitmap() }
                                val videoFilePath =
                                    resultValue.metadata.values.firstNotNullOfOrNull { (it.results.lastOrNull()?.result?.getOrNull() as? VideoRecordingResult)?.filePath }
                                onShareResultClicked(
                                    context = context,
                                    coroutineScope = coroutineScope,
                                    json = json,
                                    faceImages = faceImages,
                                    videoFilePath = videoFilePath,
                                    onPreparingChange = { isPreparingShare = it }
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
            if (isPreparingShare) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

private fun onShareResultClicked(
    context: Context,
    coroutineScope: CoroutineScope,
    json: String,
    faceImages: List<Bitmap>,
    videoFilePath: String?,
    onPreparingChange: (Boolean) -> Unit
) {
    coroutineScope.launch {
        onPreparingChange(true)
        try {
            val zipUri = withContext(Dispatchers.IO) {
                createShareZipUri(
                    context = context,
                    json = json,
                    faceImages = faceImages,
                    videoFilePath = videoFilePath
                )
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, zipUri)
                putExtra(Intent.EXTRA_SUBJECT, "Face capture result")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val chooser = Intent.createChooser(shareIntent, "Share face capture bundle")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not prepare share bundle", Toast.LENGTH_SHORT).show()
        } finally {
            onPreparingChange(false)
        }
    }
}

private fun createShareZipUri(
    context: android.content.Context,
    json: String,
    faceImages: List<Bitmap>,
    videoFilePath: String?
): Uri {
    val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
    sharedDir.listFiles()?.forEach { stale -> stale.delete() }

    val zipFile = File(sharedDir, "face-capture-${UUID.randomUUID()}.zip")
    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
        faceImages.forEachIndexed { index, bitmap ->
            zipOut.putNextEntry(ZipEntry("face-${index + 1}.jpg"))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, zipOut)
            zipOut.closeEntry()
        }

        zipOut.putNextEntry(ZipEntry("result.json"))
        zipOut.write(json.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        val videoFile = videoFilePath?.let { File(it) }
        if (videoFile != null && videoFile.exists() && videoFile.isFile) {
            zipOut.putNextEntry(ZipEntry(videoFile.name))
            FileInputStream(videoFile).use { input ->
                input.copyTo(zipOut)
            }
            zipOut.closeEntry()
        }
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        zipFile
    )
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
