package com.appliedrec.verid3.facecapture

class FaceTrackingPluginException(val causes: Map<String,Set<Throwable>>) : Exception() {
    override val message: String?
        get() {
            if (causes.isEmpty()) {
                return "Face tracking plugin failed"
            }
            if (causes.size == 1) {
                return "Face tracking plugin failed: ${causes.keys.first()}"
            }
            return "Face tracking plugins failed: ${causes.keys.joinToString(", ")}"
        }

    constructor(plugins: List<FaceTrackingPlugin<*>>) : this(plugins.associate { it.name to it.results.mapNotNull { it.result.exceptionOrNull() }.toSet() })
}