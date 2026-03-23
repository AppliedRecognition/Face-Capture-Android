package com.appliedrec.verid3.facecapturedemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.ui.FaceCaptureConfiguration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CaptureFunctionViewModel(application: Application) : AndroidViewModel(application) {

    private val setup = Setup(application)

    val faceCaptureConfiguration: FaceCaptureConfiguration
        get() = setup.faceCaptureConfiguration

    private val _captureResult = MutableSharedFlow<FaceCaptureSessionResult>()
    val captureResult: SharedFlow<FaceCaptureSessionResult> = _captureResult.asSharedFlow()

    fun onCaptureComplete(result: FaceCaptureSessionResult) {
        if (result !is FaceCaptureSessionResult.Cancelled) {
            viewModelScope.launch { _captureResult.emit(result) }
        }
    }
}
