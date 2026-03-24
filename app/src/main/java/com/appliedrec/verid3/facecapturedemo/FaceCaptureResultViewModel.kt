package com.appliedrec.verid3.facecapturedemo

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appliedrec.verid3.common.serialization.toBitmap
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.toExportJson
import com.appliedrec.videorecordingplugin.VideoRecordingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed class ShareState {
    data object Idle : ShareState()
    data object Preparing : ShareState()
    data class Ready(val uri: Uri) : ShareState()
    data class Error(val message: String) : ShareState()
}

class FaceCaptureResultViewModel(application: Application) : AndroidViewModel(application) {

    private val results: MutableMap<String, FaceCaptureSessionResult> = mutableMapOf()

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    fun saveResult(result: FaceCaptureSessionResult): String {
        val id = result.hashCode().toString()
        results[id] = result
        return id
    }

    fun getResult(id: String): FaceCaptureSessionResult? = results[id]

    fun deleteResult(id: String): FaceCaptureSessionResult? = results.remove(id)

    fun shareResult(result: FaceCaptureSessionResult) {
        viewModelScope.launch {
            _shareState.value = ShareState.Preparing
            try {
                val uri = withContext(Dispatchers.IO) {
                    createShareZipUri(result)
                }
                _shareState.value = ShareState.Ready(uri)
            } catch (e: Exception) {
                _shareState.value = ShareState.Error("Could not prepare share bundle")
            }
        }
    }

    fun clearShareState() {
        _shareState.value = ShareState.Idle
    }

    private fun createShareZipUri(result: FaceCaptureSessionResult): Uri {
        val context = getApplication<Application>()
        val json = result.toExportJson()
        val faceImages = result.capturedFaces.map { it.image.toBitmap() }
        val videoFilePath = result.metadata.values.firstNotNullOfOrNull {
            (it.results.lastOrNull()?.result?.getOrNull() as? VideoRecordingResult)?.filePath
        }

        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        sharedDir.listFiles()?.forEach { it.delete() }

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
                FileInputStream(videoFile).use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
    }
}
