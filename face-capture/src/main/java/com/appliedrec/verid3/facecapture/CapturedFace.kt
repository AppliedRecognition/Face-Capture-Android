package com.appliedrec.verid3.facecapture

import android.graphics.Bitmap
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.serialization.toBitmap
import kotlin.math.max
import kotlin.math.min

data class CapturedFace(val image: IImage, val face: Face, val bearing: Bearing) {

    val faceImage: Bitmap by lazy {
        val left = max(0, this.face.bounds.left.toInt())
        val top = max(0, this.face.bounds.top.toInt())
        val right = min(image.width, this.face.bounds.right.toInt())
        val bottom = min(image.height, this.face.bounds.bottom.toInt())
        Bitmap.createBitmap(this.image.toBitmap(), left, top, right - left, bottom - top)
    }
}
