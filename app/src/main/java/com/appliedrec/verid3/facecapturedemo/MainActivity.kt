package com.appliedrec.verid3.facecapturedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appliedrec.verid3.facecapture.ui.TipsView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FaceCaptureAppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "presentationSelection") {
                    composable("presentationSelection") {
                        PresentationSelection(navController)
                    }
                    composable("modalPresentation") {
                        ModalView(Demo.MODAL.title, Demo.MODAL.description, navController)
                    }
                    composable("embeddedPresentation") {
                        EmbeddedView(Demo.EMBEDDED.title, Demo.MODAL.description, navController)
                    }
                    composable("captureFunctionPresentation") {
                        CaptureFunctionView(Demo.CAPTURE_FUNCTION.title, Demo.CAPTURE_FUNCTION.description, navController)
                    }
                    composable("sessionResult/{resultId}") { backStackEntry ->
                        backStackEntry.arguments?.getString("resultId")?.let { resultId ->
                            FaceCaptureResultView(resultId)
                        }
                    }
                    composable("tips") {
                        TipsView()
                    }
                    composable("settings") {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun PresentationSelection(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val demos = arrayOf(Demo.MODAL, Demo.EMBEDDED, Demo.CAPTURE_FUNCTION)
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        AppBar("Face capture") {
            IconButton(onClick = {
                navController.navigate("settings")
            }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            demos.forEach { demo ->
                DemoSection(demo, modifier) {
                    when (it) {
                        Demo.MODAL -> navController.navigate("modalPresentation")
                        Demo.EMBEDDED -> navController.navigate("embeddedPresentation")
                        Demo.CAPTURE_FUNCTION -> navController.navigate("captureFunctionPresentation")
                    }
                }
            }
        }

    }
}

@Composable
fun DemoSection(
    demo: Demo,
    modifier: Modifier,
    onNavigate: (Demo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        onClick = {
            onNavigate(demo)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(demo.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Launch demo", tint = Color.White)
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp
            )
            Text(demo.description)
        }

    }
}

enum class Demo(val title: String, val description: String) {
    MODAL("Modal", "This example shows how to configure and run face capture presented in a modal sheet."),
    EMBEDDED("Embedded", "This example shows how to embed a face capture session view in your layout."),
    CAPTURE_FUNCTION("Capture function", "This example shows how to run a face capture session using a suspending function. The function launches a face capture activity and returns when the activity finishes.")
}