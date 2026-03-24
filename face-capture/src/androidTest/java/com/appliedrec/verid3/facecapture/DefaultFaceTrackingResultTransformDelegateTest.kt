package com.appliedrec.verid3.facecapture

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.EulerAngle
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.serialization.fromBitmap
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class DefaultFaceTrackingResultTransformDelegateTest {

    // Minimal Parcelable transformer that passes results through and counts calls
    private class RecordingTransformer : FaceTrackingResultTransformer {
        val callCount = AtomicInteger(0)
        var lastInput: FaceTrackingResult? = null

        override fun transformFaceResult(faceTrackingResult: FaceTrackingResult): FaceTrackingResult {
            callCount.incrementAndGet()
            lastInput = faceTrackingResult
            return faceTrackingResult
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {}
        override fun describeContents() = 0
    }

    // Transformer that converts FaceAligned to FaceFound (to test ordering)
    private class AlignedToFoundTransformer : FaceTrackingResultTransformer {
        override fun transformFaceResult(faceTrackingResult: FaceTrackingResult): FaceTrackingResult {
            return if (faceTrackingResult is FaceTrackingResult.FaceAligned) {
                FaceTrackingResult.FaceFound(
                    faceTrackingResult.requestedBearing,
                    faceTrackingResult.expectedFaceBounds!!,
                    faceTrackingResult.input!!,
                    faceTrackingResult.face!!,
                    faceTrackingResult.smoothedFace!!
                )
            } else faceTrackingResult
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {}
        override fun describeContents() = 0
    }

    private fun makeFaceAligned(): FaceTrackingResult.FaceAligned {
        val image = Image.fromBitmap(Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888))
        val input = FaceCaptureSessionImageInput(0u, 5000L, image)
        val face = Face(RectF(150f, 200f, 450f, 600f), EulerAngle(0f, 0f, 0f), 10f, emptyArray(), PointF(250f, 350f), PointF(350f, 350f))
        return FaceTrackingResult.FaceAligned(Bearing.STRAIGHT, RectF(100f, 150f, 500f, 650f), input, face, face)
    }

    private fun makeFaceFound(): FaceTrackingResult.FaceFound {
        val image = Image.fromBitmap(Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888))
        val input = FaceCaptureSessionImageInput(0u, 5000L, image)
        val face = Face(RectF(150f, 200f, 450f, 600f), EulerAngle(0f, 0f, 0f), 10f, emptyArray(), PointF(250f, 350f), PointF(350f, 350f))
        return FaceTrackingResult.FaceFound(Bearing.STRAIGHT, RectF(100f, 150f, 500f, 650f), input, face, face)
    }

    @Test
    fun noTransformersConvertsFaceAlignedToFaceCaptured() {
        val delegate = DefaultFaceTrackingResultTransformDelegate(emptyList())
        val result = delegate.transformFaceResult(makeFaceAligned())
        Assert.assertTrue(result is FaceTrackingResult.FaceCaptured)
    }

    @Test
    fun noTransformersPassesThroughNonAlignedResult() {
        val delegate = DefaultFaceTrackingResultTransformDelegate(emptyList())
        val input = makeFaceFound()
        val result = delegate.transformFaceResult(input)
        Assert.assertTrue(result is FaceTrackingResult.FaceFound)
    }

    @Test
    fun singleTransformerResultIsUsed() {
        // A transformer that always returns FaceFound — so FaceAligned should NOT become FaceCaptured
        val delegate = DefaultFaceTrackingResultTransformDelegate(listOf(AlignedToFoundTransformer()))
        val result = delegate.transformFaceResult(makeFaceAligned())
        Assert.assertTrue(result is FaceTrackingResult.FaceFound)
    }

    @Test
    fun multipleTransformersAppliedInOrder() {
        val recorder = RecordingTransformer()
        // First transformer converts FaceAligned → FaceFound; second records what it receives
        val delegate = DefaultFaceTrackingResultTransformDelegate(listOf(AlignedToFoundTransformer(), recorder))
        delegate.transformFaceResult(makeFaceAligned())
        Assert.assertEquals(1, recorder.callCount.get())
        // The recorder should have received FaceFound (output of first transformer), not FaceAligned
        Assert.assertTrue(recorder.lastInput is FaceTrackingResult.FaceFound)
    }
}
