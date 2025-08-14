package com.appliedrec.verid3.facecapture.ui

import com.appliedrec.verid3.common.FaceDetection
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingPlugin
import com.appliedrec.verid3.facecapture.FaceTrackingResultTransformer

data class FaceCaptureConfiguration(
    val settings: FaceCaptureSessionSettings,
    val viewConfiguration: FaceCaptureViewConfiguration,
    val createFaceDetection: suspend () -> FaceDetection,
    val createFaceTrackingPlugins: suspend () -> List<FaceTrackingPlugin<Any>> = { emptyList() },
    val createFaceTrackingResultTransformers: suspend () -> List<FaceTrackingResultTransformer> = { emptyList() }
)