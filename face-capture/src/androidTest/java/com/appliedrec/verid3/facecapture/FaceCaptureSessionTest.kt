package com.appliedrec.verid3.facecapture

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appliedrec.verid3.common.EulerAngle
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.FaceDetection
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.serialization.fromBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Timer
import java.util.TimerTask

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class FaceCaptureSessionTest {

    private class MockFaceDetection: FaceDetection {

        override suspend fun detectFacesInImage(image: IImage, limit: Int): List<Face> = coroutineScope {
            val faceWidth = if (image.width > image.height) image.height * 0.8f else image.width * 0.6f
            val faceHeight = faceWidth * 0.8f
            val left = image.width / 2 - faceWidth / 2
            val top = image.height / 2 - faceHeight / 2
            val faceBounds = RectF(left, top, left + faceWidth, top + faceHeight)
            listOf(Face(faceBounds, EulerAngle(0f, 0f, 0f), 10f, emptyArray(), PointF(), PointF()))
        }

    }

    @Test
    fun testSession(): Unit = runBlocking {
        val session = FaceCaptureSession(
            FaceCaptureSessionSettings(),
            { MockFaceDetection() }
        )
        val startTime = System.currentTimeMillis()
        var serialNumber: ULong = 0u
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            val scope = CoroutineScope(Dispatchers.Default)
            override fun run() {
                scope.launch {
                    if (session.result.value != null) {
                        timer.cancel()
                        return@launch
                    }
                    val time = System.currentTimeMillis() - startTime
                    val image = Image.fromBitmap(
                        Bitmap.createBitmap(
                            600,
                            800,
                            Bitmap.Config.ARGB_8888
                        )
                    )
                    val input = FaceCaptureSessionImageInput(serialNumber, time, image)
                    session.submitImageInput(input)
                    serialNumber++
                }
            }
        }, 0, 100)
        val result = session.result.first { it != null }
        Assert.assertNotNull(result)
        Assert.assertTrue(result is FaceCaptureSessionResult.Success)
        Assert.assertEquals(1, result!!.capturedFaces.size)
    }
}