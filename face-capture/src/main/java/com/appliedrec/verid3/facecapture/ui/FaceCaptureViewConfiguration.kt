package com.appliedrec.verid3.facecapture.ui

import android.content.Context
import com.appliedrec.verid3.facecapture.FaceTrackingResult
import com.appliedrec.verid3.facecapture.R
import kotlinx.coroutines.flow.MutableStateFlow

class FaceCaptureViewConfiguration(
    val context: Context,
    val useBackCamera: Boolean = false,
    textPromptProvider: ((FaceTrackingResult) -> String)? = null,
    var textPrompt: MutableStateFlow<String?> = MutableStateFlow(null),
    var showTextPrompts: Boolean = true,
    var showCancelButton: Boolean = true
) {
    var textPromptProvider: (FaceTrackingResult) -> String = if (textPromptProvider != null) { textPromptProvider } else {
        var countdownFinished = false
        { result ->
            when {
                result is FaceTrackingResult.FaceCaptured || countdownFinished -> {
                    countdownFinished = true
                    context.getString(R.string.face_captured)
                }
                result is FaceTrackingResult.Starting -> {
                    context.getString(R.string.hold_still)
                }
                result is FaceTrackingResult.FaceFixed || result is FaceTrackingResult.FaceAligned -> {
                    context.getString(R.string.hold_still)
                }
                result is FaceTrackingResult.FaceMisaligned -> {
                    context.getString(R.string.face_the_camera)
                }
                else -> {
                    val face = result.smoothedFace ?: result.face
                    val expected = result.expectedFaceBounds
                    if (face != null && expected != null) {
                        val ratio = face.bounds.width() / expected.width()
                        when {
                            ratio < 0.80f -> {
                                context.getString(R.string.prompt_move_closer)
                            }
                            ratio > 1.25f -> {
                                context.getString(R.string.prompt_move_farther)
                            }
                            else -> context.getString(R.string.align_your_face_with_the_oval)
                        }
                    } else {
                        context.getString(R.string.align_your_face_with_the_oval)
                    }
                }
            }
        }
    }
}
