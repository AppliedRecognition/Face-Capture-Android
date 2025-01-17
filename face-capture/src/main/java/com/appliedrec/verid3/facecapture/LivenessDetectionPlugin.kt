package com.appliedrec.verid3.facecapture

import com.appliedrec.livenessdetection.common.ISpoofDetector
import com.appliedrec.livenessdetection.common.isImageSpoofed
import com.appliedrec.verid3.common.serialization.toBitmap
import java.util.concurrent.atomic.AtomicBoolean

class LivenessDetectionPlugin(val spoofDetectors: Array<ISpoofDetector>): FaceTrackingPlugin<Unit>() {
    override val name: String = "Passive liveness"
    var maxPositiveFrameCount: Int = 3
    private val isClosed = AtomicBoolean(false)

    override suspend fun processFaceTrackingResult(faceTrackingResult: FaceTrackingResult): Unit? {
        if (isClosed.get()) {
            return null
        }
        val image = faceTrackingResult.input?.image?.toBitmap()
        val face = faceTrackingResult.face
        if (image == null || face == null) {
            return null
        }
        val isSpoofed = this.spoofDetectors.map { spoofDetector ->
            spoofDetector.isImageSpoofed(image, face.bounds)
        }.any { it }
        if (isSpoofed) {
            throw Exception("Liveness test failed")
        }
        return Unit
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