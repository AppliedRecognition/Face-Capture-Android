package com.appliedrec.verid3.facecapture

import com.appliedrec.verid3.common.FaceDetection

data class FaceCaptureSessionModuleFactories(
    val createFaceDetection: suspend () -> FaceDetection,
    val createFaceTrackingPlugins: suspend () -> List<FaceTrackingPlugin<Any>> = { emptyList() },
    val createFaceTrackingResultTransformers: suspend () -> List<FaceTrackingResultTransformer> = { emptyList() }
) {

    companion object {
        val defaultInstance: FaceCaptureSessionModuleFactories = FaceCaptureSessionModuleFactories(
            createFaceDetection = { TODO() }
        )
    }
}