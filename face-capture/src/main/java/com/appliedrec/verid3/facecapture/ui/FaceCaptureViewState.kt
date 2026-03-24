package com.appliedrec.verid3.facecapture.ui

import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.facecapture.FaceCaptureSession
import com.appliedrec.verid3.facecapture.FaceCaptureSessionImageInput
import com.appliedrec.verid3.facecapture.submitImageInput

internal class FaceCaptureViewState {
    var serialNumber: ULong = 0uL
        private set
    var startTime: Long? = null
        private set

    suspend fun processImage(image: IImage, session: FaceCaptureSession) {
        val now = System.currentTimeMillis()
        if (startTime == null) startTime = now
        val elapsed = now - startTime!!
        session.submitImageInput(FaceCaptureSessionImageInput(serialNumber++, elapsed, image))
    }
}
