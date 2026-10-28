package com.appliedrec.verid3.facecapture

import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import androidx.core.util.SizeFCompat
import com.appliedrec.verid3.common.Bearing

class FaceCaptureSessionSettings() : Parcelable {

    val maxDuration: Long = 30000
    val availableBearings: Set<Bearing> = setOf(Bearing.STRAIGHT, Bearing.LEFT, Bearing.RIGHT)
    val pauseDuration: Long = 1500
    var faceCaptureCount: Int = 1
    var yawThreshold: Float = 20f
    var pitchThreshold: Float = 15f
    var yawThresholdTolerance: Float = 0f
    var pitchThresholdTolerance: Float = 0f
    var faceAspectRatio: Float = 4f/5f
    var expectedFaceBoundsWidth: Float = 0.55f
    var expectedFaceBoundsHeight: Float = 0.7f
    var countdownSeconds: Int = 3
    var minFaceWidthRatio: Float = 0.80f
    var maxFaceWidthRatio: Float = 1.25f

    constructor(parcel: Parcel) : this() {
        faceCaptureCount = parcel.readInt()
        yawThreshold = parcel.readFloat()
        pitchThreshold = parcel.readFloat()
        yawThresholdTolerance = parcel.readFloat()
        pitchThresholdTolerance = parcel.readFloat()
        faceAspectRatio = parcel.readFloat()
        expectedFaceBoundsWidth = parcel.readFloat()
        expectedFaceBoundsHeight = parcel.readFloat()
        countdownSeconds = parcel.readInt()
        minFaceWidthRatio = parcel.readFloat()
        maxFaceWidthRatio = parcel.readFloat()
    }

    fun expectedFaceBoundsInSize(width: Float, height: Float): RectF {
        val size: SizeFCompat = if (width/height > this.faceAspectRatio) {
            val newHeight = height * this.expectedFaceBoundsHeight
            val newWidth = newHeight * this.faceAspectRatio
            SizeFCompat(newWidth, newHeight)
        } else {
            val newWidth = width * this.expectedFaceBoundsWidth
            val newHeight = newWidth / this.faceAspectRatio
            SizeFCompat(newWidth, newHeight)
        }
        return RectF(
            width / 2 - size.width / 2,
            height / 2 - size.height / 2,
            width / 2 - size.width / 2 + size.width,
            height / 2 - size.height / 2 + size.height
        )
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(faceCaptureCount)
        parcel.writeFloat(yawThreshold)
        parcel.writeFloat(pitchThreshold)
        parcel.writeFloat(yawThresholdTolerance)
        parcel.writeFloat(pitchThresholdTolerance)
        parcel.writeFloat(faceAspectRatio)
        parcel.writeFloat(expectedFaceBoundsWidth)
        parcel.writeFloat(expectedFaceBoundsHeight)
        parcel.writeInt(countdownSeconds)
        parcel.writeFloat(minFaceWidthRatio)
        parcel.writeFloat(maxFaceWidthRatio)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FaceCaptureSessionSettings> {
        override fun createFromParcel(parcel: Parcel): FaceCaptureSessionSettings {
            return FaceCaptureSessionSettings(parcel)
        }

        override fun newArray(size: Int): Array<FaceCaptureSessionSettings?> {
            return arrayOfNulls(size)
        }
    }
}
