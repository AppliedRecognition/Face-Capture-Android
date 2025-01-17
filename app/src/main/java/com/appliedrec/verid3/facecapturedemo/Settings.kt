package com.appliedrec.verid3.facecapturedemo

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

class SettingsViewModel(context: Context) : ViewModel() {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var useBackCamera = mutableStateOf(sharedPreferences.useBackCamera)
        private set

    fun setUseBackCamera(value: Boolean) {
        useBackCamera.value = value
        sharedPreferences.useBackCamera = value
    }

    var enableActiveLiveness = mutableStateOf(sharedPreferences.enableActiveLiveness)
        private set

    fun setEnableActiveLiveness(value: Boolean) {
        enableActiveLiveness.value = value
        sharedPreferences.enableActiveLiveness = value
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(
    LocalContext.current))) {
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
                    checked = viewModel.useBackCamera.value,
                    onCheckedChange = {
                        viewModel.setUseBackCamera(it)
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable active liveness")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.enableActiveLiveness.value,
                    onCheckedChange = {
                        viewModel.setEnableActiveLiveness(it)
                    })
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