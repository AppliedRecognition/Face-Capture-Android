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
        { result ->
            when (result) {
                is FaceTrackingResult.Created -> context.getString(R.string.preparing_face_detection)
                is FaceTrackingResult.FaceFixed -> context.getString(R.string.great_hold_it)
                is FaceTrackingResult.FaceAligned -> context.getString(R.string.great_hold_it)
                is FaceTrackingResult.FaceMisaligned -> context.getString(R.string.turn_to_follow_the_arrow)
                else -> context.getString(R.string.align_your_face_with_the_oval)
            }
        }
    }
}