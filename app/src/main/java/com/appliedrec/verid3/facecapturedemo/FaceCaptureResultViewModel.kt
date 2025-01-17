package com.appliedrec.verid3.facecapturedemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult

class FaceCaptureResultViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val results: MutableMap<String, FaceCaptureSessionResult> = mutableMapOf()
    }

    fun saveResult(result: FaceCaptureSessionResult): String {
        val id = result.hashCode().toString()
        results[id] = result
        return id
    }

    fun getResult(id: String): FaceCaptureSessionResult? {
        return results[id]
    }

    fun deleteResult(id: String): FaceCaptureSessionResult? {
        return results.remove(id)
    }
}