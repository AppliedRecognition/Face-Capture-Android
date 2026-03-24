package com.appliedrec.verid3.facecapturedemo

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _useBackCamera = MutableStateFlow(sharedPreferences.useBackCamera)
    val useBackCamera: StateFlow<Boolean> = _useBackCamera.asStateFlow()

    private val _enableActiveLiveness = MutableStateFlow(sharedPreferences.enableActiveLiveness)
    val enableActiveLiveness: StateFlow<Boolean> = _enableActiveLiveness.asStateFlow()

    fun setUseBackCamera(value: Boolean) {
        _useBackCamera.value = value
        sharedPreferences.useBackCamera = value
    }

    fun setEnableActiveLiveness(value: Boolean) {
        _enableActiveLiveness.value = value
        sharedPreferences.enableActiveLiveness = value
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val useBackCamera by viewModel.useBackCamera.collectAsState()
    val enableActiveLiveness by viewModel.enableActiveLiveness.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        AppBar(title = "Settings")
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use back camera")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = useBackCamera,
                    onCheckedChange = { viewModel.setUseBackCamera(it) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable active liveness")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = enableActiveLiveness,
                    onCheckedChange = { viewModel.setEnableActiveLiveness(it) }
                )
            }
        }
    }
}

var SharedPreferences.useBackCamera: Boolean
    get() = this.getBoolean("use_back_camera", false)
    set(value) = this.edit().putBoolean("use_back_camera", value).apply()

var SharedPreferences.enableActiveLiveness: Boolean
    get() = this.getBoolean("enable_active_liveness", false)
    set(value) = this.edit().putBoolean("enable_active_liveness", value).apply()
