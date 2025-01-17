package com.appliedrec.verid3.facecapture.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.serialization.fromBitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ImageConversionAnalyzer(private val imageFlow: MutableSharedFlow<IImage>) : ImageAnalysis.Analyzer {

    val image: SharedFlow<IImage>
        get() = imageFlow.asSharedFlow()

    override fun analyze(image: ImageProxy) {
        try {
            var bitmap = image.toBitmap()
            // Crop bitmap to crop rect
            val cropRect = image.cropRect
            bitmap = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
            imageFlow.tryEmit(Image.fromBitmap(bitmap))
        } catch (e: Exception) {
            Log.e("Ver-ID", "Failed to convert or emit bitmap", e)
        } finally {
            image.close()
        }
    }
}