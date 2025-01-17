package com.appliedrec.verid3.facecapture

import android.graphics.PointF
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.EulerAngle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

class AngleBearingEvaluation(private val sessionSettings: FaceCaptureSessionSettings) {

    enum class Axis {
        YAW, PITCH
    }

    fun angleForBearing(bearing: Bearing): EulerAngle<Float> {
        val pitchDistance = this.thresholdAngleForAxis(Axis.PITCH)
        val yawDistance = this.thresholdAngleForAxis(Axis.YAW)
        val pitch = when (bearing) {
            Bearing.UP, Bearing.LEFT_UP, Bearing.RIGHT_UP -> 0 - pitchDistance
            Bearing.DOWN, Bearing.LEFT_DOWN, Bearing.RIGHT_DOWN -> pitchDistance
            else -> 0f
        }
        val yaw = when (bearing) {
            Bearing.LEFT, Bearing.LEFT_DOWN, Bearing.LEFT_UP -> 0 - yawDistance
            Bearing.RIGHT, Bearing.RIGHT_DOWN, Bearing.RIGHT_UP -> yawDistance
            else -> 0f
        }
        return EulerAngle(yaw, pitch, 0f)
    }

    /// Whether the angle can be considered to be matching the given bearing
    ///
    /// - Parameters:
    ///   - angle: Angle to evaluate against the bearing
    ///   - bearing: Bearing the angle should match
    /// - Returns: `true` if the angle can be considered to be matching the bearing
    fun angleMatchesBearing(angle: EulerAngle<Float>, bearing: Bearing): Boolean {
        val minAngle = this.minAngleForBearing(bearing)
        val maxAngle = this.maxAngleForBearing(bearing)
        return angle.pitch > minAngle.pitch && angle.pitch < maxAngle.pitch && angle.yaw > minAngle.yaw && angle.yaw < maxAngle.yaw
    }

    /// Offset from the an angle to the given bearing.
    /// Used, for example, to calculate the arrow showing the user where to move
    ///
    /// - Parameters:
    ///   - from: Angle from which to calculate the offset to the bearing
    ///   - bearing: Bearing to which the offset should be calculated
    /// - Returns: Angle that represents the difference (offset) between the given angle to the bearing angle
    fun offsetFromAngleToBearing(from: EulerAngle<Float>, bearing: Bearing): EulerAngle<Float> {
        var yaw = 0f
        var pitch = 0f
        if (!this.angleMatchesBearing(from, bearing)) {
            val bearingAngle = this.angleForBearing(bearing)
            yaw = (bearingAngle.yaw - from.yaw) / (this.thresholdAngleForAxis(Axis.YAW) + this.thresholdAngleToleranceForAxis(Axis.YAW))
            pitch = (from.pitch - bearingAngle.pitch) / (this.thresholdAngleForAxis(Axis.PITCH) + this.thresholdAngleToleranceForAxis(Axis.PITCH))
        }
        return EulerAngle(yaw, pitch, 0f)
    }

    fun isPointToRightOfPlaneBetweenPoint(pt: PointF, start: PointF, end: PointF): Boolean {
        val d = (pt.x - start.x) * (end.y - start.y) - (pt.y - start.y) * (end.x - start.x)
        return d <= 0
    }

    fun isPointInsideCircleCentredInPointWithRadius(pt: PointF, centre: PointF, radius: Float): Boolean {
        return hypot(pt.x-centre.x, pt.y-centre.y) <= radius
    }

    fun isAngleBetweenBearings(angle: EulerAngle<Float>, fromBearing: Bearing, toBearing: Bearing): Boolean {
        if (this.angleMatchesBearing(angle, fromBearing) || this.angleMatchesBearing(angle, toBearing)) {
            return true
        }
        val fromAngle = this.angleForBearing(fromBearing)
        val toAngle = this.angleForBearing(toBearing)

        val start = PointF(fromAngle.yaw, fromAngle.pitch)
        val end = PointF(toAngle.yaw, toAngle.pitch)
        val pt = PointF(angle.yaw, angle.pitch)
        val radius = max(thresholdAngleForAxis(Axis.PITCH), thresholdAngleForAxis(Axis.YAW))
        val angleRad = atan2(end.y-start.y, end.x-start.x) + PI.toFloat() * 0.5f

        val cosRad = cos(angleRad) * radius
        val sinRad = sin(angleRad) * radius
        val startRight = PointF(start.x + cosRad, start.y + sinRad)
        val startLeft = PointF(start.x - cosRad, start.y - sinRad)
        val endRight = PointF(end.x + cosRad, end.y + sinRad)
        val endLeft = PointF(end.x - cosRad, end.y - sinRad)
        return !isPointToRightOfPlaneBetweenPoint(pt, startRight, endRight)
                && isPointToRightOfPlaneBetweenPoint(pt, startLeft, endLeft)
                && (isPointToRightOfPlaneBetweenPoint(pt, startRight, startLeft) || isPointInsideCircleCentredInPointWithRadius(pt, start, radius))
    }

    private fun minAngleForBearing(bearing: Bearing): EulerAngle<Float> {
        val pitchDistance = this.thresholdAngleForAxis(Axis.PITCH)
        val pitchTolerance = this.thresholdAngleToleranceForAxis(Axis.PITCH)
        val yawDistance = this.thresholdAngleForAxis(Axis.YAW)
        val yawTolerance = this.thresholdAngleToleranceForAxis(Axis.YAW)
        val pitch = when (bearing) {
            Bearing.UP, Bearing.LEFT_UP, Bearing.RIGHT_UP -> 0 - Float.MAX_VALUE
            Bearing.DOWN, Bearing.LEFT_DOWN, Bearing.RIGHT_DOWN -> pitchDistance - pitchTolerance
            else -> 0 - pitchDistance + pitchTolerance
        }
        val yaw = when (bearing) {
            Bearing.LEFT, Bearing.LEFT_DOWN, Bearing.LEFT_UP -> 0 - Float.MAX_VALUE
            Bearing.RIGHT, Bearing.RIGHT_DOWN, Bearing.RIGHT_UP -> yawDistance - yawTolerance
            else -> 0 - yawDistance + yawTolerance
        }
        return EulerAngle(yaw, pitch, 0f)
    }

    private fun maxAngleForBearing(bearing: Bearing): EulerAngle<Float> {
        val pitchDistance = this.thresholdAngleForAxis(Axis.PITCH)
        val pitchTolerance = this.thresholdAngleToleranceForAxis(Axis.PITCH)
        val yawDistance = this.thresholdAngleForAxis(Axis.YAW)
        val yawTolerance = this.thresholdAngleToleranceForAxis(Axis.YAW)
        val pitch = when (bearing) {
            Bearing.UP, Bearing.LEFT_UP, Bearing.RIGHT_UP -> 0 - pitchDistance + pitchTolerance
            Bearing.DOWN, Bearing.LEFT_DOWN, Bearing.RIGHT_DOWN -> Float.MAX_VALUE
            else -> pitchDistance - pitchTolerance
        }
        val yaw = when (bearing) {
            Bearing.LEFT, Bearing.LEFT_DOWN, Bearing.LEFT_UP -> 0 - yawDistance + yawTolerance
            Bearing.RIGHT, Bearing.RIGHT_DOWN, Bearing.RIGHT_UP -> Float.MAX_VALUE
            else -> yawDistance - yawTolerance
        }
        return EulerAngle(yaw, pitch, 0f)
    }

    fun thresholdAngleForAxis(axis: Axis): Float {
        return if (axis == Axis.PITCH) this.sessionSettings.pitchThreshold else this.sessionSettings.yawThreshold
    }

    fun thresholdAngleToleranceForAxis(axis: Axis): Float {
        return if (axis == Axis.PITCH) this.sessionSettings.pitchThresholdTolerance else this.sessionSettings.yawThresholdTolerance
    }
}