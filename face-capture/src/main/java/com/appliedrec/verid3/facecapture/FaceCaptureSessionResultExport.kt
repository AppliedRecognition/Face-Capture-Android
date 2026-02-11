package com.appliedrec.verid3.facecapture

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed class FaceCaptureSessionResultExport {
    abstract val status: String

    @Serializable
    @SerialName("success")
    data class SuccessExport(
        override val status: String = "success",
        val capturedFaces: List<CapturedFaceExport>,
        val metadata: Map<String, TaskResultsExport>
    ) : FaceCaptureSessionResultExport()

    @Serializable
    @SerialName("failure")
    data class FailureExport(
        override val status: String = "failure",
        val capturedFaces: List<CapturedFaceExport>,
        val metadata: Map<String, TaskResultsExport>,
        val errorMessage: String,
        val errorType: String? = null
    ) : FaceCaptureSessionResultExport()

    @Serializable
    @SerialName("cancelled")
    data class CancelledExport(
        override val status: String = "cancelled",
        val capturedFaces: List<CapturedFaceExport> = emptyList(),
        val metadata: Map<String, TaskResultsExport> = emptyMap()
    ) : FaceCaptureSessionResultExport()
}

@Serializable
data class CapturedFaceExport(
    val imageWidth: Int,
    val imageHeight: Int,
    val bearing: String,
    val face: FaceExport
)

@Serializable
data class FaceExport(
    val bounds: RectExport,
    val angle: EulerAngleExport,
    val quality: Float,
    val landmarks: List<PointExport>,
    val leftEye: PointExport,
    val rightEye: PointExport,
    val noseTip: PointExport? = null,
    val mouthCentre: PointExport? = null
)

@Serializable
data class RectExport(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class EulerAngleExport(
    val yaw: Float,
    val pitch: Float,
    val roll: Float
)

@Serializable
data class PointExport(
    val x: Float,
    val y: Float
)

@Serializable
data class TaskResultsExport(
    val summary: String,
    val results: List<FaceTrackingPluginResultExport>
)

@Serializable
data class FaceTrackingPluginResultExport(
    val serialNumber: String,
    val time: Long,
    val status: String,
    val value: JsonElement? = null,
    val errorMessage: String? = null,
    val errorType: String? = null
)
