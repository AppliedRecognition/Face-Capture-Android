package com.appliedrec.verid3.facecapture

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.ViewModelProvider
import com.appliedrec.verid3.facecapture.ui.FaceCaptureActivity
import com.appliedrec.verid3.facecapture.ui.FaceCaptureConfiguration
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewConfiguration
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewModel
import com.appliedrec.verid3.facecapture.ui.SharedViewModelStoreOwner
import kotlinx.coroutines.CompletableDeferred

object FaceCapture {
    @JvmStatic
    suspend fun captureFaces(
        context: ComponentActivity,
        configuration: FaceCaptureConfiguration
    ): FaceCaptureSessionResult {
        val deferredResult = CompletableDeferred<FaceCaptureSessionResult>()

        val launcher = context.activityResultRegistry.register(
            "face_capture",
            CaptureFaceContract()
        ) { result ->
            deferredResult.complete(result)
        }

        launcher.launch(configuration)

        return deferredResult.await()
    }

    suspend fun captureFaces(
        context: ComponentActivity,
        configure: suspend FaceCaptureConfiguration.() -> Unit
    ): FaceCaptureSessionResult {
        try {
            val configuration = FaceCaptureConfiguration(
                settings = FaceCaptureSessionSettings(),
                viewConfiguration = FaceCaptureViewConfiguration(context.applicationContext),
                createFaceDetection = { throw IllegalArgumentException("Face detection not specified") }
            )
            configure(configuration)
            return captureFaces(context, configuration)
        } catch (e: Exception) {
            return FaceCaptureSessionResult.Failure(emptyList(), emptyMap(), e)
        }
    }
}

private class CaptureFaceContract : ActivityResultContract<FaceCaptureConfiguration, FaceCaptureSessionResult>() {

    override fun createIntent(context: Context, input: FaceCaptureConfiguration): Intent {
        val model = ViewModelProvider(SharedViewModelStoreOwner)[FaceCaptureViewModel::class.java]
        val configurationId = model.saveFaceCaptureConfiguration(input)
        return Intent(context, FaceCaptureActivity::class.java).apply {
            putExtra("configurationId", configurationId)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): FaceCaptureSessionResult {
        if (resultCode == Activity.RESULT_CANCELED) {
            return FaceCaptureSessionResult.Cancelled()
        }
        val model = ViewModelProvider(SharedViewModelStoreOwner)[FaceCaptureViewModel::class.java]
        val result = if (resultCode == Activity.RESULT_OK) {
            intent?.getIntExtra("resultId", -1)?.let { resultId ->
                model.removeSessionResult(resultId)
            }
        } else {
            null
        }
        return result ?: FaceCaptureSessionResult.Failure(emptyList(), emptyMap(), Exception("Failed to get session result from activity"))
    }
}