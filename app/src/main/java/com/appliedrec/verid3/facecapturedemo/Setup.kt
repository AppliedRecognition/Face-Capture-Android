package com.appliedrec.verid3.facecapturedemo

import android.content.Context
import com.appliedrec.verid3.facecapture.FaceCaptureSession
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingPlugin
import com.appliedrec.verid3.facecapture.LivenessDetectionPlugin
import com.appliedrec.verid3.facecapture.ui.FaceCaptureConfiguration
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewConfiguration
import com.appliedrec.verid3.facedetection.retinaface.FaceDetectionRetinaFace
import com.appliedrec.verid3.spoofdevicedetection.local.SpoofDeviceDetection

class Setup(val context: Context) {

    val faceCaptureSession: FaceCaptureSession
        get() = FaceCaptureSession(
            faceCaptureSessionSettings,
            createFaceDetection,
            createFaceTrackingPlugins
        )

    val faceCaptureViewConfiguration: FaceCaptureViewConfiguration
        get() {
            val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            return FaceCaptureViewConfiguration(context = context, useBackCamera = sharedPreferences.useBackCamera)
        }

    val createFaceDetection: suspend () -> FaceDetectionRetinaFace = {
        FaceDetectionRetinaFace.create(context)
    }

    val createFaceTrackingPlugins: suspend () -> List<FaceTrackingPlugin<Any>> = {
        val livenessDetectionPlugin = LivenessDetectionPlugin(arrayOf(
            SpoofDeviceDetection(
                context
            )
        ))
        listOf(livenessDetectionPlugin as FaceTrackingPlugin<Any>)
    }

    val faceCaptureSessionSettings: FaceCaptureSessionSettings
        get() {
            val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            return FaceCaptureSessionSettings().apply {
                this.faceCaptureCount = if (sharedPreferences.enableActiveLiveness) 2 else 1
            }
        }

    val faceCaptureConfiguration: FaceCaptureConfiguration
        get() = FaceCaptureConfiguration(
            faceCaptureSessionSettings,
            faceCaptureViewConfiguration,
            createFaceDetection,
            createFaceTrackingPlugins
        )
}