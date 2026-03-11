package com.sri.aham.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Static fallback color schemes (used when dynamic color is unavailable)
// ---------------------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary           = Indigo80,
    secondary         = IndigoGrey80,
    tertiary          = Sage80,
    primaryContainer  = IndigoContainer80,
    secondaryContainer = SaffronContainer80,
)

private val LightColorScheme = lightColorScheme(
    primary           = Indigo40,
    secondary         = IndigoGrey40,
    tertiary          = Sage40,
    primaryContainer  = IndigoContainer,
    secondaryContainer = SaffronContainer,
    onPrimaryContainer = Indigo10,
    onSecondaryContainer = Saffron10,
)

/**
 * Root theme for the Aham app.
 *
 * - On Android 12+ (API 31+) with [dynamicColor] = true: uses the user's wallpaper-derived
 *   Material You palette for a fully personalised look.
 * - Otherwise: falls back to the spiritual Indigo / Saffron palette defined in [Color.kt].
 *
 * The status-bar icon colour is adjusted automatically to contrast with the background.
 */
@Composable
fun AhamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Keep status-bar icons legible on the chosen background.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = Typography,
        content      = content,
    )
}
