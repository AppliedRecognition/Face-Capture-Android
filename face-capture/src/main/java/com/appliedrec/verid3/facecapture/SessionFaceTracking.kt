package com.appliedrec.verid3.facecapture

import android.graphics.PointF
import android.graphics.RectF
import android.util.Size
import androidx.core.util.SizeFCompat
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.EulerAngle
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.FaceDetection

internal class SessionFaceTracking(private val faceDetection: FaceDetection, private val settings: FaceCaptureSessionSettings) {

    private data class AlignedFace(val face: Face, var isAligned: Boolean = false, var isFixed: Boolean = false)

    private var requestedBearing: Bearing = Bearing.STRAIGHT
        set(value) {
            previousBearing = field
            field = value
        }
    private var previousBearing: Bearing? = null
    private val angleBearingEvaluation: AngleBearingEvaluation = AngleBearingEvaluation(this.settings)
    private val faces: TimeConstrainedCircularBuffer<AlignedFace> = TimeConstrainedCircularBuffer(500)
    private var hasBeenAligned: Boolean = false
    var isFaceWithBoundsFixedInImageSize: (RectF, RectF, Size) -> Boolean = { bounds, expectedBounds, imageSize ->
        val maxRect = RectF(0f, 0f, imageSize.width.toFloat(), imageSize.height.toFloat())
        val minRect = RectF(expectedBounds)
        minRect.inset(expectedBounds.width()*0.4f, expectedBounds.height()*0.4f)
        bounds.contains(minRect) && maxRect.contains(bounds)
    }
    var alignTime: Long? = null
    var angleHistory: MutableList<EulerAngle<Float>> = mutableListOf()
    var hasFaceBeenFixed: Boolean = false
    var delegate: SessionFaceTrackingDelegate? = null
    var smoothingBufferSize: Int = 10
    var launched: Boolean = false
    var started: Boolean = false

    suspend fun trackFace(imageCapture: FaceCaptureSessionImageInput): FaceTrackingResult {
        val imageSize = SizeFCompat(imageCapture.image.width.toFloat(), imageCapture.image.height.toFloat())
        val expectedFaceBounds = this.settings.expectedFaceBoundsInSize(imageSize.width, imageSize.height)
        if (!launched) {
            launched = true
            return FaceTrackingResult.Launched(this.requestedBearing, expectedFaceBounds)
        }
        val face = this.faceDetection.detectFacesInImage(imageCapture.image, 1).firstOrNull()?.normalizingBounds()
        if (face != null) {
            face.faceAspectRatio = settings.faceAspectRatio
            val alignedFace = AlignedFace(face)
            this.faces.append(alignedFace)
            val smoothedFace = this.smoothedFace!!
            if (imageCapture.time < settings.countdownSeconds * 1000) {
                return FaceTrackingResult.Starting(
                    this.requestedBearing,
                    expectedFaceBounds,
                    imageCapture,
                    smoothedFace
                )
            } else if (!started) {
                started = true
                return FaceTrackingResult.Started(
                    this.requestedBearing,
                    expectedFaceBounds,
                    imageCapture,
                    smoothedFace
                )
            }
            val angleMatchesBearing = this.angleBearingEvaluation.angleMatchesBearing(
                smoothedFace.angle,
                this.requestedBearing
            )
            this.faces.last!!.isAligned = angleMatchesBearing
            this.faces.last!!.isFixed =
                this.isFaceWithBoundsFixedInImageSize(smoothedFace.bounds, expectedFaceBounds, Size(imageCapture.image.width, imageCapture.image.height))
            if (this.settings.faceCaptureCount > 1) {
                this.angleHistory.add(smoothedFace.angle)
                this.previousBearing?.let { previousBearing ->
                    if (previousBearing != this.requestedBearing) {
                        var movedOpposite = false
                        for (angle in this.angleHistory) {
                            if (!this.angleBearingEvaluation.isAngleBetweenBearings(
                                    angle,
                                    previousBearing,
                                    this.requestedBearing
                                )
                            ) {
                                movedOpposite = true
                                break
                            }
                        }
                        if (movedOpposite) {
                            error("Face moved in the opposite direction")
                        }
                    }
                }
            }
        } else if (imageCapture.time < settings.countdownSeconds * 1000) {
            return FaceTrackingResult.Starting(
                this.requestedBearing,
                expectedFaceBounds,
                imageCapture
            )
        } else {
            this.angleHistory.clear()
        }
        var result: FaceTrackingResult = FaceTrackingResult.Started(
            this.requestedBearing,
            expectedFaceBounds,
            imageCapture
        )
        if (!started) {
            started = true
            return result
        }
        if (!this.hasFaceBeenFixed && this.faces.hasRemovedElements && this.faces.allSatisfy { it.isFixed }) {
            this.hasFaceBeenFixed = true
            return FaceTrackingResult.FaceFixed(
                this.requestedBearing,
                expectedFaceBounds,
                imageCapture,
                this.faces.last!!.face,
                smoothedFace!!
            )
        }
        if (this.hasFaceBeenFixed && this.faces.hasRemovedElements) {
            if (this.faces.allSatisfy { it.isAligned }) {
                val now = System.currentTimeMillis()
                if (this.alignTime != null && now - this.alignTime!! < this.settings.pauseDuration) {
                    result = FaceTrackingResult.Paused(
                        this.requestedBearing,
                        expectedFaceBounds,
                        imageCapture
                    )
                } else {
                    if (this.delegate != null) {
                        result = this.delegate!!.transformFaceResult(
                            FaceTrackingResult.FaceAligned(
                                this.requestedBearing,
                                expectedFaceBounds,
                                imageCapture,
                                this.faces.last!!.face,
                                smoothedFace!!
                            )
                        )
                    } else {
                        result = FaceTrackingResult.FaceCaptured(
                            this.requestedBearing,
                            expectedFaceBounds,
                            imageCapture,
                            this.faces.last!!.face,
                            smoothedFace!!
                        )
                    }
                    if (result is FaceTrackingResult.FaceCaptured) {
                        this.alignTime = now
                        this.faces.clear()
                        if (this.settings.faceCaptureCount > 0 && this.settings.availableBearings.size > 1) {
                            val bearings = this.settings.availableBearings.toMutableSet()
                            bearings.remove(this.requestedBearing)
                            this.requestedBearing = bearings.random()
                        }
                    } else {
                        result = FaceTrackingResult.FaceAligned(
                            this.requestedBearing,
                            expectedFaceBounds,
                            imageCapture,
                            this.faces.last!!.face,
                            smoothedFace!!
                        )
                    }
                }
            } else {
                result = FaceTrackingResult.FaceMisaligned(
                    this.requestedBearing,
                    expectedFaceBounds,
                    imageCapture,
                    this.faces.last!!.face,
                    smoothedFace!!
                )
            }
            return result
        }
        if (this.faces.isEmpty && this.hasFaceBeenFixed) {
            throw Exception("Active liveness failed")
        }
        if (!this.faces.isEmpty) {
            return FaceTrackingResult.FaceFound(this.requestedBearing, expectedFaceBounds, imageCapture, this.faces.last!!.face, smoothedFace!!)
        }
        return result
    }

    fun reset() {
        this.faces.clear()
        this.angleHistory.clear()
        this.hasBeenAligned = false
        this.hasFaceBeenFixed = false
        this.alignTime = null
        this.requestedBearing = Bearing.STRAIGHT
        this.previousBearing = null
        this.launched = false
        this.started = false
    }

    private val smoothedFace: Face?
        get() {
            if (this.faces.isEmpty) {
                return null
            }
            val tail = this.faces.suffix(this.smoothingBufferSize)
            val left = tail.map { it.face.bounds.left }.average()
            val top = tail.map { it.face.bounds.top }.average()
            val right = tail.map { it.face.bounds.right }.average()
            val bottom = tail.map { it.face.bounds.bottom }.average()
            val yaw = tail.map { it.face.angle.yaw }.average()
            val pitch = tail.map { it.face.angle.pitch }.average()
            val roll = tail.map { it.face.angle.roll }.average()
            val minLandmarkCount = tail.minOfOrNull { it.face.landmarks.size } ?: 0
            val landmarks = tail.map { it.face.landmarks }
            val averageLandmarks = Array(minLandmarkCount) { i ->
                val x = landmarks.map { it[i].x }.average()
                val y = landmarks.map { it[i].y }.average()
                PointF(x.toFloat(), y.toFloat())
            }
            val averageLeftEye = PointF(tail.map { it.face.leftEye.x }.average().toFloat(), tail.map { it.face.leftEye.y }.average().toFloat())
            val averageRightEye = PointF(tail.map { it.face.rightEye.x }.average().toFloat(), tail.map { it.face.rightEye.y }.average().toFloat())
            val averageNoseTipX = tail.mapNotNull { it.face.noseTip?.x }.average()
            val averageNoseTipY = tail.mapNotNull { it.face.noseTip?.y }.average()
            val averageNoseTip = if (averageNoseTipX.isNaN() || averageNoseTipY.isNaN()) null else PointF(averageNoseTipX.toFloat(), averageNoseTipY.toFloat())
            val averageMouthCentreX = tail.mapNotNull { it.face.mouthCentre?.x }.average()
            val averageMouthCentreY = tail.mapNotNull { it.face.mouthCentre?.y }.average()
            val averageMouthCentre = if (averageMouthCentreX.isNaN() || averageMouthCentreY.isNaN()) null else PointF(averageMouthCentreX.toFloat(), averageMouthCentreY.toFloat())
            val quality = tail.map { it.face.quality }.average()
            return Face(
                RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()),
                EulerAngle(yaw.toFloat(), pitch.toFloat(), roll.toFloat()),
                quality.toFloat(),
                averageLandmarks,
                averageLeftEye,
                averageRightEye,
                averageNoseTip,
                averageMouthCentre
            )
        }
}