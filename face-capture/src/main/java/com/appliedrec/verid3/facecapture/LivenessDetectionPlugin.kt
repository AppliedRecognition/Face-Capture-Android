package com.appliedrec.verid3.facecapture

import com.appliedrec.livenessdetection.common.ISpoofDetector
import com.appliedrec.livenessdetection.common.isImageSpoofed
import com.appliedrec.verid3.common.serialization.toBitmap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicBoolean

class LivenessDetectionPlugin(val spoofDetectors: Array<ISpoofDetector>): FaceTrackingPlugin<Unit>() {
    override val name: String = "Passive liveness"
    var maxPositiveFrameCount: Int = 3
    private val isClosed = AtomicBoolean(false)

    override suspend fun processFaceTrackingResult(faceTrackingResult: FaceTrackingResult): Unit? = coroutineScope {
        if (isClosed.get()) {
            return@coroutineScope null
        }
        val image = faceTrackingResult.input?.image?.toBitmap()
        val face = faceTrackingResult.face
        if (image == null || face == null) {
            return@coroutineScope null
        }
        val isSpoofed = spoofDetectors.map { spoofDetector ->
            async { spoofDetector.isImageSpoofed(image!!, face.bounds) }
        }.awaitAll().any { it }
        if (isSpoofed) {
            throw Exception("Liveness test failed")
        }
        Unit
    }

    override suspend fun createSummaryFromResults(results: List<FaceTrackingPluginResult<Unit>>): String {
        if (isClosed.compareAndSet(false, true)) {
            spoofDetectors.forEach { it.close() }
        }
        if (results.isEmpty()) {
            return "Liveness check not performed"
        }
        if (results.count { it.result.isFailure } > maxPositiveFrameCount) {
            return "Liveness test failed"
        } else {
            return "Liveness test passed"
        }
    }
}