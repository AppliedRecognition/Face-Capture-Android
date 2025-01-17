package com.appliedrec.verid3.facecapturedemo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val faceCaptureAppTypography = Typography(
    bodyLarge = TextStyle(
        fontSize = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 20.sp
    ),
    titleLarge = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold
    ),
)

private val LightColourScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    background = Color(0xFFF2F2F2),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

@Composable
fun FaceCaptureAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colourScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColourScheme
    }
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colourScheme.background
        )
    }
    MaterialTheme(
        colorScheme = colourScheme,
        typography = faceCaptureAppTypography,
        content = content
    )
}