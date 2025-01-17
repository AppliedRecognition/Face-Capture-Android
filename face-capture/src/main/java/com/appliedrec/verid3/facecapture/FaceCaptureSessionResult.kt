package com.appliedrec.verid3.facecapture

sealed class FaceCaptureSessionResult {
    abstract val capturedFaces: List<CapturedFace>
    abstract val metadata: Map<String, TaskResults<*>>
    data class Success(override val capturedFaces: List<CapturedFace>, override val metadata: Map<String, TaskResults<out Any?>>) : FaceCaptureSessionResult()
    data class Failure(override val capturedFaces: List<CapturedFace>, override val metadata: Map<String, TaskResults<out Any?>>, val error: Throwable) : FaceCaptureSessionResult()
    class Cancelled: FaceCaptureSessionResult() {
        override val capturedFaces: List<CapturedFace> = emptyList()
        override val metadata: Map<String, TaskResults<out Any?>> = emptyMap()
    }
}

