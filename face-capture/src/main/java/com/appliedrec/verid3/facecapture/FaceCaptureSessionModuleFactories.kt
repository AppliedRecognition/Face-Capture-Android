package com.appliedrec.verid3.facecapture

import com.appliedrec.verid3.common.FaceDetection

data class FaceCaptureSessionModuleFactories(
    val createFaceDetection: () -> FaceDetection,
    val createFaceTrackingPlugins: () -> List<FaceTrackingPlugin<Any>> = { emptyList() },
    val createFaceTrackingResultTransformers: () -> List<FaceTrackingResultTransformer> = { emptyList() }
) {

    companion object {
        val defaultInstance: FaceCaptureSessionModuleFactories = FaceCaptureSessionModuleFactories(
            createFaceDetection = { TODO() }
        )
    }
}