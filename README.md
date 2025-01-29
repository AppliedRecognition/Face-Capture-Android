# Face Capture for Android

This library captures images from an Android device and detects faces that can be used for face recognition.

## Requirements

The face capture runs on Android 8.0 (API level 26).

## Installation

Please [contact Applied Recognition](mailto:support@appliedrecognition.com) to obtain credentials to access the package manager repositories.

1. Set the following environment variables:

    ```
    export GITHUB_USER=<user name obtained from ARC>
    export GITHUB_TOKEN=<token obtained from ARC>
    ```
2. Add the following to your project's **settings.gradle.kts**:

    ```kotlin
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral()
            maven {
                url = uri("https://maven.pkg.github.com/AppliedRecognition/Ver-ID-3D-Android-Libraries")
                credentials {
                    username = settings.extra["gpr.user"] as String?
                    password = settings.extra["gpr.token"] as String?
                }
            }
        }
    }
    ```
3. Add dependencies in your module's **build.gradle.kts**:

    ```kotlin
    dependencies {
        implementation("com.appliedrec.verid3:face-capture:1.0.0")
        implementation("com.appliedrec.verid3:face-detection-mp:1.0.0")
        implementation("com.appliedrec.verid.livenessdetection:spoof-device-detection:1.0.1")
        implementation("com.appliedrec.verid.livenessdetection:spoof-device-detection-models:1.0.1")
    }
    ```
    
## Usage

This example shows how to present the face capture in a modal sheet.

```kotlin
fun ModalView() {
    var showBottomSheet by remember {
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val livenessDetectionPlugin = LivenessDetectionPlugin(arrayOf(SpoofDeviceDetector(context)))
    val session = FaceCaptureSession(
        FaceCaptureSessionSettings(), 
        FaceCaptureSessionModuleFactories(
            { FaceDetection(context) },
            { listOf(livenessDetectionPlugin as FaceTrackingPlugin<Any>) },
            { emptyList() }
        ))
    val result by session.result.collectAsState()

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Button(onClick = {
            showBottomSheet = true
        }) {
            Text("Start capture")
        }
    }
    if (showBottomSheet && result == null) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                session.cancel()
            },
            sheetState = sheetState
        ) {
            FaceCaptureView(
                session = session, 
                modifier = Modifier.fillMaxSize(), 
                configuration = FaceCaptureViewConfiguration(context)
            ) {
                showBottomSheet = false
                when (result) {
                    is FaceCaptureSessionResult.Success -> {
                        System.out.println("Session succeeded, collected ${result.capturedFaces.size} face(s)")
                    }
                    is FaceCaptureSessionResult.Failure -> {
                        System.out.println("Session failed: ${result.error}")
                    }
                    else -> {
                        System.out.println("Session cancelled")
                    }
                }
            }
        }
    } else if (showBottomSheet) {
        showBottomSheet = false
    }
}
```