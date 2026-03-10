package com.sri.aham

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sri.aham.navigation.AppNavGraph
import com.sri.aham.ui.theme.AhamTheme

/**
 * Single-activity host for the entire app.
 *
 * All screen navigation is managed by [AppNavGraph] using Jetpack Navigation Compose.
 * This activity intentionally contains no business logic — it only bootstraps the
 * Compose content and applies the app theme.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AhamTheme {
                AppNavGraph()
            }
        }
    }
}
