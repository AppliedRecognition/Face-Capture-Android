package com.appliedrec.verid3.facecapture.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.appliedrec.verid3.facecapture.FaceCaptureSession
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.ui.ui.theme.FaceCaptureTheme

open class FaceCaptureActivity : ComponentActivity() {

    private val faceCaptureViewModel: FaceCaptureViewModel by lazy {
        ViewModelProvider(SharedViewModelStoreOwner)[FaceCaptureViewModel::class.java]
    }
    private var configuration: FaceCaptureConfiguration? = null
    private lateinit var session: FaceCaptureSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configurationId: Int
        configurationId = savedInstanceState?.getInt("configurationId", -1) ?: intent.getIntExtra("configurationId", -1)
        configuration = faceCaptureViewModel.removeFaceCaptureConfiguration(configurationId)
        if (configuration == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        session = FaceCaptureSession(
            configuration!!.settings,
            configuration!!.createFaceDetection,
            configuration!!.createFaceTrackingPlugins,
            configuration!!.createFaceTrackingResultTransformers
        )
        setContent {
            FaceCaptureTheme {
                FaceCaptureView(session = session, configuration = configuration!!.viewConfiguration) { result ->
                    if (result is FaceCaptureSessionResult.Cancelled) {
                        setResult(RESULT_CANCELED)
                        finish()
                        return@FaceCaptureView
                    }
                    val resultId = faceCaptureViewModel.saveSessionResult(result)
                    val resultIntent = Intent().apply {
                        putExtra("resultId", resultId)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        configuration?.let {
            val configurationId = faceCaptureViewModel.saveFaceCaptureConfiguration(it)
            outState.putInt("configurationId", configurationId)
        }
    }

    override fun finish() {
        configuration?.let { faceCaptureViewModel.removeFaceCaptureConfiguration(it.hashCode()) }
        configuration = null
        session.cancel()
        super.finish()
    }
}

object SharedViewModelStoreOwner : ViewModelStoreOwner {

    override val viewModelStore = ViewModelStore()
}

class FaceCaptureViewModel : ViewModel() {

    private val configurations: MutableMap<Int, FaceCaptureConfiguration> = mutableMapOf()
    private val sessionResults: MutableMap<Int, FaceCaptureSessionResult> = mutableMapOf()

    fun saveFaceCaptureConfiguration(configuration: FaceCaptureConfiguration): Int {
        val id = configuration.hashCode()
        configurations[id] = configuration
        return id
    }

    fun removeFaceCaptureConfiguration(id: Int): FaceCaptureConfiguration? {
        return configurations.remove(id)
    }

    fun saveSessionResult(result: FaceCaptureSessionResult): Int {
        val id = result.hashCode()
        sessionResults[id] = result
        return id
    }

    fun removeSessionResult(id: Int): FaceCaptureSessionResult? {
        return sessionResults.remove(id)
    }
}