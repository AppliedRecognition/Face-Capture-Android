package com.appliedrec.verid3.v3

import android.content.Context
import androidx.activity.ComponentActivity
import com.appliedrec.livenessdetection.common.ISpoofDetector
import com.appliedrec.livenessdetection.spoofdevice.SpoofDeviceDetector
import com.appliedrec.verid.common.FaceDetection
import com.appliedrec.verid.common.FaceRecognition
import com.appliedrec.verid.common.FaceRecognitionTemplate
import com.appliedrec.verid.facecapture.CapturedFace
import com.appliedrec.verid.facecapture.FaceCapture
import com.appliedrec.verid.facecapture.FaceCaptureSessionModuleFactories
import com.appliedrec.verid.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid.facecapture.FaceTrackingPlugin
import com.appliedrec.verid.facecapture.LivenessDetectionPlugin
import com.appliedrec.verid.facecapture.ui.FaceCaptureConfiguration
import com.appliedrec.verid.facecapture.ui.FaceCaptureViewConfiguration
import com.appliedrec.verid.facedetection.mp.FaceDetection as MediaPipeFaceDetection

class VerID(context: Context) {

    var faceDetection: FaceDetection = MediaPipeFaceDetection(context)
    var faceRecognition: Map<Int, FaceRecognition> = emptyMap()
    var spoofDetection: Array<ISpoofDetector> = arrayOf(SpoofDeviceDetector(context))

    suspend fun captureFaces(activity: ComponentActivity, settings: FaceCaptureSessionSettings?, faceCaptureViewConfiguration: FaceCaptureViewConfiguration?): FaceCaptureSessionResult {
        val sessionSettings = settings ?: FaceCaptureSessionSettings()
        val viewConfiguration = faceCaptureViewConfiguration ?: FaceCaptureViewConfiguration()
        val faceTrackingPlugins: MutableList<FaceTrackingPlugin<Any>> = mutableListOf()
        if (spoofDetection.isNotEmpty()) {
            val livenessDetectionPlugin = LivenessDetectionPlugin(spoofDetection)
            faceTrackingPlugins.add(livenessDetectionPlugin as FaceTrackingPlugin<Any>)
        }
        val configuration = FaceCaptureConfiguration(
            sessionSettings,
            viewConfiguration,
            FaceCaptureSessionModuleFactories(
                { faceDetection },
                { faceTrackingPlugins },
                { emptyList() }
            )
        )
        return FaceCapture.captureFaces(activity, configuration)
    }

    suspend fun createFaceRecognitionTemplates(capturedFace: CapturedFace): Map<Int, FaceRecognitionTemplate> {
        val faceRecognitionTemplates: MutableMap<Int, FaceRecognitionTemplate> = mutableMapOf()
        for ((version, faceRecognition) in faceRecognition) {
            val template = faceRecognition.createFaceRecognitionTemplates(arrayOf(capturedFace.face), capturedFace.image).first()
            faceRecognitionTemplates[version] = template
        }
        return faceRecognitionTemplates
    }
}