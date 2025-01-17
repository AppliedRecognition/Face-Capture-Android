package com.appliedrec.verid3.facecapture

data class TaskResults<T>(val summary: String, val results: List<FaceTrackingPluginResult<T>>)
