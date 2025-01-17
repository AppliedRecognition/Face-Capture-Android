package com.appliedrec.verid3.facecapture.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.appliedrec.verid3.facecapture.FaceTrackingResult

class FaceOval(private val faceTrackingResult: FaceTrackingResult): Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return if (faceTrackingResult.expectedFaceBounds != null) {
            Outline.Generic(Path().apply {
                addOval(Rect(faceTrackingResult.expectedFaceBounds.left, faceTrackingResult.expectedFaceBounds.top, faceTrackingResult.expectedFaceBounds.right, faceTrackingResult.expectedFaceBounds.bottom))
            })
        } else {
            Outline.Generic(Path().apply{
                Rect(Offset.Zero, size)
            })
        }
    }

}