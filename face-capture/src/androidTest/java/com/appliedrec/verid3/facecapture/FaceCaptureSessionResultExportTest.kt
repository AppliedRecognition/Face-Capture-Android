package com.appliedrec.verid3.facecapture

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appliedrec.verid3.common.Bearing
import com.appliedrec.verid3.common.EulerAngle
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.Image
import com.appliedrec.verid3.common.serialization.fromBitmap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaceCaptureSessionResultExportTest {

    private data class NonSerializable(val value: Int)

    @Test
    fun successWithoutMetadataHasStableShape() {
        val result = FaceCaptureSessionResult.Success(
            capturedFaces = listOf(createCapturedFace()),
            metadata = emptyMap()
        )

        val json = result.toExportJson()
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("success", root["status"]?.jsonPrimitive?.content)
        assertEquals(1, root["capturedFaces"]?.jsonArray?.size)
        assertEquals(0, root["metadata"]?.jsonObject?.size)

        val face = root["capturedFaces"]!!.jsonArray.first().jsonObject
        assertEquals("STRAIGHT", face["bearing"]?.jsonPrimitive?.content)
        assertEquals(600, face["imageWidth"]?.jsonPrimitive?.int)
        assertEquals(800, face["imageHeight"]?.jsonPrimitive?.int)

        val facePayload = face["face"]!!.jsonObject
        assertEquals(10.0, facePayload["quality"]?.jsonPrimitive?.double)
    }

    @Test
    fun metadataSuccessStringValueIsSerialized() {
        val result = FaceCaptureSessionResult.Success(
            capturedFaces = emptyList(),
            metadata = mapOf(
                "plugin" to TaskResults(
                    summary = "ok",
                    results = listOf(
                        FaceTrackingPluginResult(
                            serialNumber = 15u,
                            time = 100L,
                            result = Result.success("done")
                        )
                    )
                )
            )
        )

        val root = Json.parseToJsonElement(result.toExportJson()).jsonObject
        val pluginResults = root["metadata"]!!.jsonObject["plugin"]!!.jsonObject["results"]!!.jsonArray
        val first = pluginResults.first().jsonObject

        assertEquals("success", first["status"]?.jsonPrimitive?.content)
        assertEquals("15", first["serialNumber"]?.jsonPrimitive?.content)
        assertEquals("done", first["value"]?.jsonPrimitive?.content)
    }

    @Test
    fun metadataSuccessUnitOmitsValue() {
        val result = FaceCaptureSessionResult.Success(
            capturedFaces = emptyList(),
            metadata = mapOf(
                "plugin" to TaskResults(
                    summary = "ok",
                    results = listOf(
                        FaceTrackingPluginResult(
                            serialNumber = 1u,
                            time = 100L,
                            result = Result.success(Unit)
                        )
                    )
                )
            )
        )

        val root = Json.parseToJsonElement(result.toExportJson()).jsonObject
        val first = root["metadata"]!!.jsonObject["plugin"]!!.jsonObject["results"]!!.jsonArray.first().jsonObject

        assertEquals("success", first["status"]?.jsonPrimitive?.content)
        assertFalse(first.containsKey("value"))
    }

    @Test
    fun metadataFailureIncludesErrorMessage() {
        val result = FaceCaptureSessionResult.Success(
            capturedFaces = emptyList(),
            metadata = mapOf(
                "plugin" to TaskResults(
                    summary = "failed",
                    results = listOf(
                        FaceTrackingPluginResult(
                            serialNumber = 2u,
                            time = 101L,
                            result = Result.failure(Exception("x"))
                        )
                    )
                )
            )
        )

        val root = Json.parseToJsonElement(result.toExportJson()).jsonObject
        val first = root["metadata"]!!.jsonObject["plugin"]!!.jsonObject["results"]!!.jsonArray.first().jsonObject

        assertEquals("failure", first["status"]?.jsonPrimitive?.content)
        assertEquals("x", first["errorMessage"]?.jsonPrimitive?.content)
        assertFalse(first.containsKey("value"))
    }

    @Test
    fun unsupportedMetadataValueFallsBackWithoutCrash() {
        val result = FaceCaptureSessionResult.Success(
            capturedFaces = emptyList(),
            metadata = mapOf(
                "plugin" to TaskResults(
                    summary = "fallback",
                    results = listOf(
                        FaceTrackingPluginResult(
                            serialNumber = 3u,
                            time = 102L,
                            result = Result.success(NonSerializable(7))
                        )
                    )
                )
            )
        )

        val root = Json.parseToJsonElement(result.toExportJson()).jsonObject
        val taskResults = root["metadata"]!!.jsonObject["plugin"]!!.jsonObject
        val first = taskResults["results"]!!.jsonArray.first().jsonObject

        assertEquals("fallback", taskResults["summary"]?.jsonPrimitive?.content)
        assertEquals("success", first["status"]?.jsonPrimitive?.content)
        assertFalse(first.containsKey("value"))
    }

    @Test
    fun failureResultExportsThrowableAsStringFields() {
        val result = FaceCaptureSessionResult.Failure(
            capturedFaces = emptyList(),
            metadata = emptyMap(),
            error = IllegalStateException("y")
        )

        val root = Json.parseToJsonElement(result.toExportJson()).jsonObject

        assertEquals("failure", root["status"]?.jsonPrimitive?.content)
        assertEquals("y", root["errorMessage"]?.jsonPrimitive?.content)
        assertNotNull(root["errorType"]?.jsonPrimitive?.content)
    }

    @Test
    fun cancelledResultExportsStableShape() {
        val result = FaceCaptureSessionResult.Cancelled

        val root = Json.parseToJsonElement(result.toExportJson()).jsonObject

        assertEquals("cancelled", root["status"]?.jsonPrimitive?.content)
        assertEquals(0, root["capturedFaces"]?.jsonArray?.size)
        assertEquals(0, root["metadata"]?.jsonObject?.size)
    }

    @Test
    fun exportedJsonCanBeParsedAsGenericJsonObject() {
        val result = FaceCaptureSessionResult.Success(
            capturedFaces = emptyList(),
            metadata = mapOf(
                "plugin" to TaskResults(
                    summary = "ok",
                    results = listOf(
                        FaceTrackingPluginResult(
                            serialNumber = 4u,
                            time = 103L,
                            result = Result.success(
                                buildJsonObject {
                                    put("portable", true)
                                }
                            )
                        )
                    )
                )
            )
        )

        val root = Json.parseToJsonElement(result.toExportJson())
        assertTrue(root is JsonObject)
        val portable = root.jsonObject["metadata"]!!
            .jsonObject["plugin"]!!
            .jsonObject["results"]!!
            .jsonArray.first()
            .jsonObject["value"]!!
            .jsonObject["portable"]!!
            .jsonPrimitive
            .boolean
        assertTrue(portable)
    }

    private fun createCapturedFace(): CapturedFace {
        val image = Image.fromBitmap(
            Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        )
        val face = Face(
            RectF(100f, 120f, 450f, 560f),
            EulerAngle(1f, 2f, 3f),
            10f,
            emptyArray(),
            PointF(200f, 250f),
            PointF(350f, 250f)
        )
        return CapturedFace(image, face, Bearing.STRAIGHT)
    }
}
