package com.appliedrec.verid3.facecapture

internal interface SessionFaceTrackingDelegate {

    fun transformFaceResult(faceTrackingResult: FaceTrackingResult): FaceTrackingResult
}