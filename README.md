# Face Capture for Android

This library captures images from an Android device and detects faces that can be used for face recognition.

## Requirements

The face capture runs on Android 8.0 (API level 26).

## Installation

Add dependencies in your module's **build.gradle.kts**:

```kotlin
dependencies {
    implementation(platform("com.appliedrec:verid-bom:2025-08-00"))
    implementation("com.appliedrec:face-capture")
    implementation("com.appliedrec:face-detection-retinaface")
    implementation("com.appliedrec:spoof-device-detection-cloud")
}
```
    
## Usage

Capture face using a suspending function.

```kotlin
class MyActivity : ComponentActivity() {

    fun captureFace() = lifecycleScope.launch(Dispatchers.Default) {
        val activity = this@MyActivity
        val appContext = activity.applicationContext
        val result = FaceCapture.captureFaces(activity) {
            // Configure the capture
            // Create a face detection factory function
            createFaceDetection = {
                // In this example we'll use RetinaFace face detection
                FaceDetectionRetinaFace.create(appContext) 
            }
            createFaceTrackingPlugins = {
                listOf(
                    // Add liveness detection plugin using spoof device detection
                    LivenessDetectionPlugin(
                        spoofDetectors = arrayOf(SpoofDeviceDetection(appContext))
                    ) as FaceTrackingPlugin<Any>
                )
            }
        }
        // Check the face capture result
        when (result) {
            // Face capture succeeded
            is FaceCaptureSessionResult.Success -> {
                val capturedFace = result.capturedFaces.first()
                withContext(Dispatchers.Main) {
                    // Update UI
                }
            }
            // Face capture failed
            is FaceCaptureSessionResult.Failure -> {
                val error = result.error
                withContext(Dispatchers.Main) {
                    // Update UI
                }
            }
            // User cancelled the capture
            is FaceCaptureSessionResult.Cancelled -> {}
        }
    }
}
```