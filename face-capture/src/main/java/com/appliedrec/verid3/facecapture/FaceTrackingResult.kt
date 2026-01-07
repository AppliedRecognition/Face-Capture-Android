package com.appliedrec.verid3.facecapture

import android.graphics.Matrix
import android.graphics.RectF
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.Face

sealed class FaceTrackingResult(val requestedBearing: Bearing, val expectedFaceBounds: RectF? = null, val input: FaceCaptureSessionImageInput? = null, val face: Face? = null, val smoothedFace: Face? = null) {
    class Created(requestedBearing: Bearing): FaceTrackingResult(requestedBearing)
    class Waiting(requestedBearing: Bearing, expectedFaceBounds: RectF): FaceTrackingResult(requestedBearing, expectedFaceBounds)
    class Launched(requestedBearing: Bearing, expectedFaceBounds: RectF): FaceTrackingResult(requestedBearing, expectedFaceBounds)
    class Starting(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput, smoothedFace: Face? = null) : FaceTrackingResult(requestedBearing, expectedFaceBounds, input, smoothedFace = smoothedFace)
    class Started(requestedBearing: Bearing, expectedFaceBounds: RectF, input: FaceCaptureSessionImageInput, smoothedFace: Face? = null): FaceTrackingResult(requestedBearing, expectedFaceBounds, input, smoothedFace = smoothedFace)
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
        is FaceTrackingResult.Launched -> FaceTrackingResult.Launched(requestedBearing, expectedFaceBounds)
        is FaceTrackingResult.Starting -> FaceTrackingResult.Starting(requestedBearing, expectedFaceBounds, input!!, scaledSmoothedFace)
        is FaceTrackingResult.Started -> FaceTrackingResult.Started(requestedBearing, expectedFaceBounds, input!!, scaledSmoothedFace)
        is FaceTrackingResult.Paused -> FaceTrackingResult.Paused(requestedBearing, expectedFaceBounds, input!!)
        is FaceTrackingResult.FaceFound -> FaceTrackingResult.FaceFound(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceFixed -> FaceTrackingResult.FaceFixed(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceAligned -> FaceTrackingResult.FaceAligned(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceMisaligned -> FaceTrackingResult.FaceMisaligned(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
        is FaceTrackingResult.FaceCaptured -> FaceTrackingResult.FaceCaptured(requestedBearing, expectedFaceBounds, input!!, scaledFace!!, scaledSmoothedFace!!)
    }
}

fun FaceTrackingResult.copyWithExpectedBounds(expectedFaceBounds: RectF): FaceTrackingResult {
    return when (this) {
        is FaceTrackingResult.Created -> FaceTrackingResult.Created(requestedBearing)
        is FaceTrackingResult.Waiting -> FaceTrackingResult.Waiting(requestedBearing, expectedFaceBounds)
        is FaceTrackingResult.Launched -> FaceTrackingResult.Launched(requestedBearing, expectedFaceBounds)
        is FaceTrackingResult.Starting -> FaceTrackingResult.Starting(requestedBearing, expectedFaceBounds, input!!, this.smoothedFace)
        is FaceTrackingResult.Started -> FaceTrackingResult.Started(requestedBearing, expectedFaceBounds, input!!, this.smoothedFace)
        is FaceTrackingResult.Paused -> FaceTrackingResult.Paused(requestedBearing, expectedFaceBounds, input!!)
        is FaceTrackingResult.FaceFound -> FaceTrackingResult.FaceFound(requestedBearing, expectedFaceBounds, input!!, this.face!!, this.smoothedFace!!)
        is FaceTrackingResult.FaceFixed -> FaceTrackingResult.FaceFixed(requestedBearing, expectedFaceBounds, input!!, this.face!!, this.smoothedFace!!)
        is FaceTrackingResult.FaceAligned -> FaceTrackingResult.FaceAligned(requestedBearing, expectedFaceBounds, input!!, this.face!!, this.smoothedFace!!)
        is FaceTrackingResult.FaceMisaligned -> FaceTrackingResult.FaceMisaligned(requestedBearing, expectedFaceBounds, input!!, this.face!!, this.smoothedFace!!)
        is FaceTrackingResult.FaceCaptured -> FaceTrackingResult.FaceCaptured(requestedBearing, expectedFaceBounds, input!!, this.face!!, this.smoothedFace!!)
    }
}