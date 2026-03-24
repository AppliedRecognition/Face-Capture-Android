package com.appliedrec.verid3.facecapture

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.EulerAngle
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.FaceDetection
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.serialization.fromBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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
            val leftEye = PointF(left + faceWidth * 0.3f, top + faceHeight * 0.3f)
            val rightEye = PointF(left + faceWidth * 0.7f, top + faceHeight * 0.3f)
            listOf(Face(faceBounds, EulerAngle(0f, 0f, 0f), 10f, emptyArray(), leftEye, rightEye))
        }

    }

    private class FinalResultPlugin : FaceTrackingPlugin<String>() {
        override val name: String = "Final result plugin"
        val summaryCalls = AtomicInteger(0)

        override suspend fun processFaceTrackingResult(faceTrackingResult: FaceTrackingResult): String? = null

        override suspend fun createSummaryFromResults(results: List<FaceTrackingPluginResult<String>>): String {
            summaryCalls.incrementAndGet()
            return "Summary"
        }

        override suspend fun createFinalResult(): String? = "final-result"
    }

    @Test
    fun testSession(): Unit = runBlocking {
        val session = FaceCaptureSession(
            FaceCaptureSessionSettings(),
            { MockFaceDetection() }
        )
        session.start()
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

    @Test
    fun testPluginFinalResultIsAddedOnce(): Unit = runBlocking {
        val plugin = FinalResultPlugin()
        val session = FaceCaptureSession(
            FaceCaptureSessionSettings(),
            { MockFaceDetection() },
            { listOf(plugin as FaceTrackingPlugin<Any>) }
        )
        session.start()
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
                        Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
                    )
                    session.submitImageInput(FaceCaptureSessionImageInput(serialNumber, time, image))
                    serialNumber++
                }
            }
        }, 0, 100)

        val result = session.result.first { it != null }
        Assert.assertTrue(result is FaceCaptureSessionResult.Success)

        val pluginResult = (result as FaceCaptureSessionResult.Success).metadata[plugin.name]
        Assert.assertNotNull(pluginResult)
        Assert.assertEquals(1, pluginResult!!.results.size)
        Assert.assertEquals("final-result", pluginResult.results.first().result.getOrNull())
        Assert.assertEquals(1, plugin.summaryCalls.get())
    }

    @Test
    fun testCancelStopsPluginsAndReturnsCancelled(): Unit = runBlocking {
        val plugin = FinalResultPlugin()
        val settings = FaceCaptureSessionSettings().apply {
            faceCaptureCount = 10
        }
        val session = FaceCaptureSession(
            settings,
            { MockFaceDetection() },
            { listOf(plugin as FaceTrackingPlugin<Any>) }
        )
        session.start()
        val feeder = launch(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            var serial: ULong = 0u
            while (session.result.value == null) {
                val image = Image.fromBitmap(Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888))
                session.submitImageInput(
                    FaceCaptureSessionImageInput(serial, System.currentTimeMillis() - start, image)
                )
                serial++
                delay(40)
            }
        }

        delay(150)
        session.cancel()
        val result = session.result.first { it != null }
        feeder.cancel()

        Assert.assertTrue(result is FaceCaptureSessionResult.Cancelled)
        Assert.assertEquals(1, plugin.summaryCalls.get())
    }

    @Test
    fun testDoubleStartIsNoop(): Unit = runBlocking {
        val session = FaceCaptureSession(FaceCaptureSessionSettings(), { MockFaceDetection() })
        session.start()
        session.start() // second call must be a no-op
        val startTime = System.currentTimeMillis()
        var serial: ULong = 0u
        val feeder = launch(Dispatchers.Default) {
            while (session.result.value == null) {
                val time = System.currentTimeMillis() - startTime
                val image = Image.fromBitmap(Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888))
                session.submitImageInput(FaceCaptureSessionImageInput(serial, time, image))
                serial++
                delay(40)
            }
        }
        val result = session.result.first { it != null }
        feeder.cancel()
        Assert.assertTrue(result is FaceCaptureSessionResult.Success)
        Assert.assertEquals(1, (result as FaceCaptureSessionResult.Success).capturedFaces.size)
    }

    @Test
    fun testTimeoutProducesFailure(): Unit = runBlocking {
        val settings = FaceCaptureSessionSettings()
        val session = FaceCaptureSession(settings, { MockFaceDetection() })
        session.start()
        // Keep submitting an image with time past maxDuration until the session processes it
        val feeder = launch(Dispatchers.Default) {
            while (session.result.value == null) {
                val image = Image.fromBitmap(Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888))
                session.submitImageInput(FaceCaptureSessionImageInput(0u, settings.maxDuration + 1L, image))
                delay(10)
            }
        }
        val result = session.result.first { it != null }
        feeder.cancel()
        Assert.assertTrue(result is FaceCaptureSessionResult.Failure)
    }

    @Test
    fun testMultiFaceCaptureSucceeds(): Unit = runBlocking {
        val settings = FaceCaptureSessionSettings().apply { faceCaptureCount = 2 }
        val eval = AngleBearingEvaluation(settings)
        val currentBearing = AtomicReference(Bearing.STRAIGHT)
        val session = FaceCaptureSession(settings, {
            object : FaceDetection {
                override suspend fun detectFacesInImage(image: IImage, limit: Int): List<Face> = coroutineScope {
                    // Return a face whose angle matches whatever bearing is currently requested
                    val angle = eval.angleForBearing(currentBearing.get())
                    val faceWidth = if (image.width > image.height) image.height * 0.8f else image.width * 0.6f
                    val faceHeight = faceWidth * 0.8f
                    val left = image.width / 2 - faceWidth / 2
                    val top = image.height / 2 - faceHeight / 2
                    val leftEye = PointF(left + faceWidth * 0.3f, top + faceHeight * 0.3f)
                    val rightEye = PointF(left + faceWidth * 0.7f, top + faceHeight * 0.3f)
                    listOf(Face(RectF(left, top, left + faceWidth, top + faceHeight), EulerAngle(angle.yaw, angle.pitch, 0f), 10f, emptyArray(), leftEye, rightEye))
                }
            }
        })
        session.start()
        val bearingTracker = launch(Dispatchers.Default) {
            session.faceTrackingResult.collect { result ->
                currentBearing.set(result.requestedBearing)
            }
        }
        val startTime = System.currentTimeMillis()
        var serial: ULong = 0u
        val feeder = launch(Dispatchers.Default) {
            while (session.result.value == null) {
                val time = System.currentTimeMillis() - startTime
                val image = Image.fromBitmap(Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888))
                session.submitImageInput(FaceCaptureSessionImageInput(serial, time, image))
                serial++
                delay(40)
            }
        }
        val result = session.result.first { it != null }
        feeder.cancel()
        bearingTracker.cancel()
        Assert.assertTrue(result is FaceCaptureSessionResult.Success)
        Assert.assertEquals(2, result!!.capturedFaces.size)
    }

    @Test
    fun testCancelledEquality() {
        Assert.assertEquals(FaceCaptureSessionResult.Cancelled, FaceCaptureSessionResult.Cancelled)
        Assert.assertEquals(
            FaceCaptureSessionResult.Cancelled.hashCode(),
            FaceCaptureSessionResult.Cancelled.hashCode()
        )
    }
}
