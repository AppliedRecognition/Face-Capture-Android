package com.appliedrec.verid3.facecapturedemo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appliedrec.verid3.facecapture.FaceCaptureSession
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewConfiguration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmbeddedViewModel(application: Application) : AndroidViewModel(application) {

    private val setup = Setup(application)

    var session: FaceCaptureSession by mutableStateOf(setup.faceCaptureSession)
        private set

    val faceCaptureViewConfiguration: FaceCaptureViewConfiguration
        get() = setup.faceCaptureViewConfiguration

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _captureResult = MutableSharedFlow<FaceCaptureSessionResult>()
    val captureResult: SharedFlow<FaceCaptureSessionResult> = _captureResult.asSharedFlow()

    fun startCapture() {
        session = setup.faceCaptureSession
        _isCapturing.value = true
    }

    fun onCaptureResult(result: FaceCaptureSessionResult) {
        _isCapturing.value = false
        if (result !is FaceCaptureSessionResult.Cancelled) {
            viewModelScope.launch { _captureResult.emit(result) }
        }
    }

    fun cancelCapture() {
        session.cancel()
        _isCapturing.value = false
    }

    override fun onCleared() {
        super.onCleared()
        session.cancel()
    }
}
