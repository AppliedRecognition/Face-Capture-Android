package com.appliedrec.verid3.facecapture

import com.appliedrec.verid3.common.Face
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializerOrNull

val defaultFaceCaptureSessionResultExportJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun FaceCaptureSessionResult.toExportJson(
    json: Json = defaultFaceCaptureSessionResultExportJson
): String = json.encodeToString(toExportModel())

fun FaceCaptureSessionResult.toExportModel(): FaceCaptureSessionResultExport {
    return when (this) {
        is FaceCaptureSessionResult.Success -> FaceCaptureSessionResultExport.SuccessExport(
            capturedFaces = capturedFaces.map { it.toExportModel() },
            metadata = metadata.mapValues { (_, taskResults) -> taskResults.toExportModel() }
        )

        is FaceCaptureSessionResult.Failure -> FaceCaptureSessionResultExport.FailureExport(
            capturedFaces = capturedFaces.map { it.toExportModel() },
            metadata = metadata.mapValues { (_, taskResults) -> taskResults.toExportModel() },
            errorMessage = error.message ?: error.toString(),
            errorType = error::class.qualifiedName
        )

        is FaceCaptureSessionResult.Cancelled -> FaceCaptureSessionResultExport.CancelledExport()
    }
}

private fun CapturedFace.toExportModel(): CapturedFaceExport {
    return CapturedFaceExport(
        imageWidth = image.width,
        imageHeight = image.height,
        bearing = bearing.name,
        face = face.toExportModel()
    )
}

private fun Face.toExportModel(): FaceExport {
    return FaceExport(
        bounds = RectExport(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom
        ),
        angle = EulerAngleExport(
            yaw = angle.yaw,
            pitch = angle.pitch,
            roll = angle.roll
        ),
        quality = quality,
        landmarks = landmarks.map { PointExport(it.x, it.y) },
        leftEye = PointExport(leftEye.x, leftEye.y),
        rightEye = PointExport(rightEye.x, rightEye.y),
        noseTip = noseTip?.let { PointExport(it.x, it.y) },
        mouthCentre = mouthCentre?.let { PointExport(it.x, it.y) }
    )
}

private fun TaskResults<*>.toExportModel(): TaskResultsExport {
    return TaskResultsExport(
        summary = summary,
        results = results.map { it.toExportModel() }
    )
}

private fun FaceTrackingPluginResult<*>.toExportModel(): FaceTrackingPluginResultExport {
    val value = result.getOrNull()
    val exception = result.exceptionOrNull()
    return if (exception != null) {
        FaceTrackingPluginResultExport(
            serialNumber = serialNumber.toString(),
            time = time,
            status = "failure",
            errorMessage = exception.message ?: exception.toString(),
            errorType = exception::class.qualifiedName
        )
    } else {
        FaceTrackingPluginResultExport(
            serialNumber = serialNumber.toString(),
            time = time,
            status = "success",
            value = value.toPortableJsonOrNull()
        )
    }
}

private fun Any?.toPortableJsonOrNull(): JsonElement? {
    return when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Short -> JsonPrimitive(this.toInt())
        is Byte -> JsonPrimitive(this.toInt())
        is UInt -> JsonPrimitive(this.toString())
        is ULong -> JsonPrimitive(this.toString())
        is UShort -> JsonPrimitive(this.toString())
        is UByte -> JsonPrimitive(this.toString())
        is Float -> if (this.isFinite()) JsonPrimitive(this) else JsonPrimitive(this.toString())
        is Double -> if (this.isFinite()) JsonPrimitive(this) else JsonPrimitive(this.toString())
        is Char -> JsonPrimitive(this.toString())
        is Enum<*> -> JsonPrimitive(this.name)
        is Unit -> null

        is List<*> -> {
            val elements = mutableListOf<JsonElement>()
            for (item in this) {
                elements.add(item?.toPortableJsonOrNull() ?: JsonNull)
            }
            JsonArray(elements)
        }

        is Map<*, *> -> {
            val entries = mutableMapOf<String, JsonElement>()
            for ((key, value) in this) {
                val stringKey = key as? String ?: return null
                entries[stringKey] = value?.toPortableJsonOrNull() ?: JsonNull
            }
            JsonObject(entries)
        }

        else -> toPortableJsonViaGeneratedSerializerOrNull()
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
private fun Any.toPortableJsonViaGeneratedSerializerOrNull(): JsonElement? {
    val serializer = this::class.serializerOrNull() as? KSerializer<Any> ?: return null
    return runCatching {
        defaultFaceCaptureSessionResultExportJson.encodeToJsonElement(serializer, this)
    }.getOrNull()
}
