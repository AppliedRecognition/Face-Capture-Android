package com.appliedrec.verid3.facecapture

import android.os.Parcelable

interface FaceTrackingResultTransformer : Parcelable {

    fun transformFaceResult(faceTrackingResult: FaceTrackingResult): FaceTrackingResult
}