package com.sri.aham.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sri.aham.navigation.Route

/**
 * Home screen — the entry point of the app.
 *
 * Displays a grid of feature cards. Tapping a card calls [onNavigate] with the
 * corresponding [Route] constant, which [AppNavGraph][com.sri.aham.navigation.AppNavGraph]
 * resolves to the correct destination screen.
 *
 * ## Adding a new feature
 *  1. Add a [FeatureCard] entry to the [features] list below with the new [Route] constant.
 *  2. Register the route in [AppNavGraph][com.sri.aham.navigation.AppNavGraph].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val features = listOf(
        FeatureCard(
            title = "Mantra\nPlayer",
            description = "Loop sacred mantras & meditation audio",
            route = Route.MANTRA,
        ),
        // Future features — uncomment and wire up as they are implemented:
        // FeatureCard("Journal",      "Daily gratitude & reflection",        Route.JOURNAL),
        // FeatureCard("Breath Timer", "Guided box breathing & 4-7-8",       Route.BREATH_TIMER),
        // FeatureCard("Pomodoro",     "Focus timer with break reminders",    Route.POMODORO),
        // FeatureCard("Affirmations", "Positive affirmations widget",        Route.AFFIRMATIONS),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aham") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(features, key = { it.route }) { feature ->
                FeatureCardItem(
                    feature = feature,
                    onClick = { onNavigate(feature.route) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Internal models & composables
// ---------------------------------------------------------------------------

/** Describes a feature card displayed on [HomeScreen]. */
private data class FeatureCard(
    val title: String,
    val description: String,
    val route: String,
)

/** Square card tile that navigates to a feature when tapped. */
@Composable
private fun FeatureCardItem(feature: FeatureCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}
