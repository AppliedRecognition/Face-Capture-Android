package com.appliedrec.verid3.facecapture.ui

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.appliedrec.verid3.facecapture.AngleBearingEvaluation
import com.appliedrec.verid3.facecapture.FaceTrackingResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun FaceArrow(faceTrackingResult: FaceTrackingResult, angleBearingEvaluation: AngleBearingEvaluation, isMirrored: Boolean = false) {
    if (faceTrackingResult is FaceTrackingResult.FaceMisaligned) {
        val offsetAngle = angleBearingEvaluation.offsetFromAngleToBearing(faceTrackingResult.smoothedFace!!.angle, faceTrackingResult.requestedBearing)
        val angle = atan2(0f-offsetAngle.pitch, offsetAngle.yaw)
        val distance = hypot(offsetAngle.yaw, 0f-offsetAngle.pitch) * 2
        val faceBounds = faceTrackingResult.expectedFaceBounds!!
        val arrowLength = faceBounds.width() / 5f
        val stemLength = min(max(arrowLength * distance, arrowLength * 0.75f), arrowLength * 1.7f)
        val arrowAngle = Math.toRadians(40.0).toFloat()
        val arrowTip = PointF(faceBounds.centerX() + cos(angle) * arrowLength / 2f, faceBounds.centerY() + sin(angle) * arrowLength / 2f)
        val arrowPoint1 = PointF(arrowTip.x + cos(angle + Math.PI.toFloat() - arrowAngle) * arrowLength * 0.6f, arrowTip.y + sin(angle + Math.PI.toFloat() - arrowAngle) * arrowLength * 0.6f)
        val arrowPoint2 = PointF(arrowTip.x + cos(angle + Math.PI.toFloat() + arrowAngle) * arrowLength * 0.6f, arrowTip.y + sin(angle + Math.PI.toFloat() + arrowAngle) * arrowLength * 0.6f)
        val arrowStart = PointF(arrowTip.x + cos(angle + Math.PI.toFloat()) * stemLength, arrowTip.y + sin(angle + Math.PI.toFloat()) * stemLength)
        val path = Path().apply {
            moveTo(arrowPoint1.x, arrowPoint1.y)
            lineTo(arrowTip.x, arrowTip.y)
            lineTo(arrowPoint2.x, arrowPoint2.y)
            moveTo(arrowTip.x, arrowTip.y)
            lineTo(arrowStart.x, arrowStart.y)
        }
        val strokeWidth: Float = 10.dp.toFloat()
        val scaleX = if (isMirrored) -1f else 1f
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scaleX)) {
            for (i in generateSequence(strokeWidth) { it + 2f }.takeWhile { it < strokeWidth + 10f }) {
                drawPath(
                    path,
                    color = Color.Black.copy(alpha = 0.025f),
                    style = Stroke(
                        width = i,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            drawPath(path, color = Color.White, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}