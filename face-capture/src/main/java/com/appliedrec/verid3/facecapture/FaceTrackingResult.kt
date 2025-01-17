package com.appliedrec.verid3.facecapture

import android.graphics.Matrix
import android.graphics.RectF
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.Face

sealed class FaceTrackingResult(val requestedBearing: Bearing, val expectedFaceBounds: RectF? = null, val input: FaceCaptureSessionImageInput? = null, val face: Face? = null, val smoothedFace: Face? = null) {
    class Created(requestedBearing: Bearing): FaceTrackingResult(requestedBearing)
    class Waiting(requestedBearing: Bearing, expectedFaceBounds: RectF): FaceTrackingResult(requestedBearing, expectedFaceBounds)
    class Started(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput): FaceTrackingResult(requestedBearing, expectedFaceBounds, input)
    class Paused(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput): FaceTrackingResult(requestedBearing, expectedFaceBounds, input)
    class FaceFound(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput, face: Face, smoothedFace: Face): FaceTrackingResult(requestedBearing, expectedFaceBounds, input, face, smoothedFace)
    class FaceFixed(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput, face: Face, smoothedFace: Face): FaceTrackingResult(requestedBearing, expectedFaceBounds, input, face, smoothedFace)
    class FaceAligned(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput, face: Face, smoothedFace: Face): FaceTrackingResult(requestedBearing, expectedFaceBounds, input, face, smoothedFace)
    class FaceMisaligned(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput, face: Face, smoothedFace: Face): FaceTrackingResult(requestedBearing, expectedFaceBounds, input, face, smoothedFace)
    class FaceCaptured(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput, face: Face, smoothedFace: Face): FaceTrackingResult(requestedBearing, expectedFaceBounds, input, face, smoothedFace)
}

val FaceTrackingResult.capturedFace: CapturedFace?
    get() {
        return if (this is FaceTrackingResult.FaceCaptured) {
            CapturedFace(input!!.image, face!!, requestedBearing)
        } else {
            null
        }
    }

fun FaceTrackingResult.scaledToViewSize(width: Float, height: Float, expectedFaceBounds: RectF, mirrored: Boolean): FaceTrackingResult {
    val matrix = if (input != null) {
//        val imageAspectRatio: Float = input.image.width.toFloat() / input.image.height.toFloat()
//        val viewAspectRatio = width / height
//        val scale = if (imageAspectRatio > viewAspectRatio) {
//            height / input.image.height.toFloat()
//        } else {
//            width / input.image.width.toFloat()
//        }
//        val newWidth = input.image.width * scale
//        val newHeight = input.image.height * scale
//        val left = (width - newWidth) / 2
//        val top = (height - newHeight) / 2
//        val targetRect = RectF(left, top, width, height)
//        Matrix().apply {
//            setRectToRect(
//                RectF(0f, 0f, input.image.width.toFloat(), input.image.height.toFloat()),
//                targetRect,
//                Matrix.ScaleToFit.FILL
//            )
//            if (mirrored) {
//                postScale(-1f, 1f, width / 2f, height / 2f)
//            }
//        }




        val imageAspectRatio: Float = input.image.width.toFloat() / input.image.height.toFloat()
        val viewAspectRatio = width / height
        if (imageAspectRatio > viewAspectRatio) {
            val newHeight = width / imageAspectRatio
            val top = (height - newHeight) / 2
            Matrix().apply {
                setRectToRect(
                    RectF(0f, 0f, input.image.width.toFloat(), input.image.height.toFloat()),
                    RectF(0f, top, width, top + newHeight),
                    Matrix.ScaleToFit.FILL
                )
                if (mirrored) {
                    postScale(-1f, 1f, width / 2f, height / 2f)
                }
            }
        } else {
            val newWidth = height * imageAspectRatio
            val left = (width - newWidth) / 2
            Matrix().apply {
                setRectToRect(
                    RectF(0f, 0f, input.image.width.toFloat(), input.image.height.toFloat()),
                    RectF(left, 0f, left + newWidth, height),
                    Matrix.ScaleToFit.FILL
                )
                if (mirrored) {
                    postScale(-1f, 1f, width / 2f, height / 2f)
                }
            }
        }
    } else {
        Matrix()
    }
    val scaledFace = this.face?.applyingMatrix(matrix)
    val scaledSmoothedFace = this.smoothedFace?.applyingMatrix(matrix)
    return when (this) {
        is FaceTrackingResult.Created -> FaceTrackingResult.Created(requestedBearing)
        is FaceTrackingResult.Waiting -> FaceTrackingResult.Waiting(requestedBearing, expectedFaceBounds)
        is FaceTrackingResult.Started -> FaceTrackingResult.Started(requestedBearing, expectedFaceBounds, input!!)
        is FaceTrackingResult.Paused -> FaceTrackingResult.Paused(requestedBearing, expectedFaceBounds, input!!)
        is FaceTrackingResult.FaceFound -> FaceTrackingResult.FaceFound(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceFixed -> FaceTrackingResult.FaceFixed(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceAligned -> FaceTrackingResult.FaceAligned(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceMisaligned -> FaceTrackingResult.FaceMisaligned(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceCaptured -> FaceTrackingResult.FaceCaptured(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
    }
}