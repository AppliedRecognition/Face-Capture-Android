package com.appliedrec.verid3.facecapture.ui

import android.content.Context
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingResult
import com.appliedrec.verid3.facecapture.R
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Provides string messages displayed during face capture.
 *
 * Developers can override any method in this class to provide dynamic, runtime,
 * or custom-translated strings without modifying the tracking logic.
 *
 * Alternatively, standard Android resource qualifiers (such as res/values-de/, res/values-ja/)
 * can be added to the consuming application to override or extend localized strings automatically.
 */
open class FaceCapturePromptStrings {
    open fun holdStill(context: Context): String = context.getString(R.string.hold_still)
    open fun moveCloser(context: Context): String = context.getString(R.string.prompt_move_closer)
    open fun moveFarther(context: Context): String = context.getString(R.string.prompt_move_farther)
    open fun faceTheCamera(context: Context): String = context.getString(R.string.face_the_camera)
    open fun alignFaceWithOval(context: Context): String = context.getString(R.string.align_your_face_with_the_oval)
    open fun faceCaptured(context: Context): String = context.getString(R.string.face_captured)
    open fun preparingFaceDetection(context: Context): String = context.getString(R.string.preparing_face_detection)
}

class FaceCaptureViewConfiguration(
    val context: Context,
    val useBackCamera: Boolean = false,
    textPromptProvider: ((FaceTrackingResult) -> String)? = null,
    val promptStrings: FaceCapturePromptStrings = FaceCapturePromptStrings(),
    var textPrompt: MutableStateFlow<String?> = MutableStateFlow(null),
    var showTextPrompts: Boolean = true,
    var showCancelButton: Boolean = true
) {
    val customTextPromptProvider: ((FaceTrackingResult) -> String)? = textPromptProvider

    fun createDefaultTextPromptProvider(sessionSettings: FaceCaptureSessionSettings): (FaceTrackingResult) -> String {
        var countdownFinished = false
        val minRatio = sessionSettings.minFaceWidthRatio
        val maxRatio = sessionSettings.maxFaceWidthRatio
        return { result ->
            when {
                result is FaceTrackingResult.FaceCaptured || countdownFinished -> {
                    countdownFinished = true
                    promptStrings.faceCaptured(context)
                }
                result is FaceTrackingResult.Starting -> {
                    promptStrings.holdStill(context)
                }
                result is FaceTrackingResult.FaceFixed || result is FaceTrackingResult.FaceAligned -> {
                    promptStrings.holdStill(context)
                }
                result is FaceTrackingResult.FaceMisaligned -> {
                    promptStrings.faceTheCamera(context)
                }
                else -> {
                    val face = result.smoothedFace ?: result.face
                    val expected = result.expectedFaceBounds
                    if (face != null && expected != null) {
                        val ratio = face.bounds.width() / expected.width()
                        when {
                            ratio < minRatio -> {
                                promptStrings.moveCloser(context)
                            }
                            ratio > maxRatio -> {
                                promptStrings.moveFarther(context)
                            }
                            else -> promptStrings.alignFaceWithOval(context)
                        }
                    } else {
                        when (result) {
                            is FaceTrackingResult.Created -> {
                                promptStrings.preparingFaceDetection(context)
                            }
                            else -> {
                                promptStrings.alignFaceWithOval(context)
                            }
                        }
                    }
                }
            }
        }
    }

    var textPromptProvider: (FaceTrackingResult) -> String = customTextPromptProvider ?: { result ->
        createDefaultTextPromptProvider(FaceCaptureSessionSettings())(result)
    }
}
