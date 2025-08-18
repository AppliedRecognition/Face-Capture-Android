package com.appliedrec.verid3.facecapture.ui

import com.appliedrec.verid3.common.FaceDetection
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingPlugin
import com.appliedrec.verid3.facecapture.FaceTrackingResultTransformer

data class FaceCaptureConfiguration(
    var settings: FaceCaptureSessionSettings,
    var viewConfiguration: FaceCaptureViewConfiguration,
    var createFaceDetection: suspend () -> FaceDetection,
    var createFaceTrackingPlugins: suspend () -> List<FaceTrackingPlugin<Any>> = { emptyList() },
    var createFaceTrackingResultTransformers: suspend () -> List<FaceTrackingResultTransformer> = { emptyList() }
)