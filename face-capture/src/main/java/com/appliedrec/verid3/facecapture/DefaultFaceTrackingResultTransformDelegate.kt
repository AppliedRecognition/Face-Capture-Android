package com.appliedrec.verid3.facecapture

internal class DefaultFaceTrackingResultTransformDelegate(
    private val transformers: List<FaceTrackingResultTransformer>
) : SessionFaceTrackingDelegate {

    override fun transformFaceResult(faceTrackingResult: FaceTrackingResult): FaceTrackingResult {
        return if (transformers.isEmpty()) {
            if (faceTrackingResult is FaceTrackingResult.FaceAligned) {
                FaceTrackingResult.FaceCaptured(
                    faceTrackingResult.requestedBearing,
                    faceTrackingResult.expectedFaceBounds!!,
                    faceTrackingResult.input!!,
                    faceTrackingResult.face!!,
                    faceTrackingResult.smoothedFace!!
                )
            } else {
                faceTrackingResult
            }
        } else {
            transformers.fold(faceTrackingResult) { result, transformer ->
                transformer.transformFaceResult(result)
            }
        }
    }
}
