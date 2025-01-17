package com.appliedrec.verid3.facecapture

data class FaceTrackingPluginResult<T>(val serialNumber: ULong, val time: Long, val result: Result<T>)
