package com.appliedrec.verid3.facecapture.ui

import com.appliedrec.verid3.facecapture.FaceCaptureSessionModuleFactories
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings

data class FaceCaptureConfiguration(
    val settings: FaceCaptureSessionSettings,
    val viewConfiguration: FaceCaptureViewConfiguration,
    val faceCaptureSessionModuleFactories: FaceCaptureSessionModuleFactories
)