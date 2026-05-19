package com.example.jeevan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val JeevanColorScheme = lightColorScheme(
    primary            = JeevanRed,
    onPrimary          = Color.White,
    primaryContainer   = JeevanRedSoft,
    onPrimaryContainer = JeevanRedDark,

    secondary          = JeevanGreen,
    onSecondary        = Color.White,
    secondaryContainer = JeevanGreenSoft,
    onSecondaryContainer = JeevanGreen,

    background         = JeevanBackground,
    onBackground       = JeevanOnBackground,

    surface            = JeevanSurface,
    onSurface          = JeevanOnSurface,
    surfaceVariant     = JeevanSurface2,
    onSurfaceVariant   = JeevanSubtext,

    outline            = JeevanBorder,
    outlineVariant     = JeevanBorder,

    error              = JeevanRed,
    onError            = Color.White,
)

@Composable
fun JeevanTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = JeevanBackground,
            darkIcons = true
        )
    }

    MaterialTheme(
        colorScheme = JeevanColorScheme,
        typography  = JeevanTypography,
        content     = content
    )
}
