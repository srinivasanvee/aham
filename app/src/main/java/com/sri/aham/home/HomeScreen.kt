package com.sri.aham.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sri.aham.navigation.Route

/**
 * Home screen — the app's entry point.
 *
 * Displays a header banner and a 2-column grid of feature cards.
 * Tapping a card navigates to the feature via [onNavigate].
 *
 * ## Adding a new feature card
 *  1. Add a [FeatureCard] entry to [features] with the correct [Route] constant.
 *  2. Register the composable route in `AppNavGraph.kt`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val features = listOf(
        FeatureCard(
            title       = "Mantra Meditation",
            description = "Loop sacred mantras and meditation",
            icon        = Icons.Outlined.SelfImprovement,
            route       = Route.MANTRA,
        ),
        FeatureCard(
            title       = "Pomodoro",
            description = "Focus timer with break reminders",
            icon        = Icons.Outlined.Timer,
            route       = Route.POMODORO,
        ),
        FeatureCard(
            title       = "Assistant",
            description = "Offline AI companion powered by Gemma 4",
            icon        = Icons.Outlined.SmartToy,
            route       = Route.ASSISTANT,
        ),
        // Uncomment as features are implemented:
        // FeatureCard("Journal",      "Daily gratitude & reflection",     Icons.Outlined.EditNote,     Route.JOURNAL),
        // FeatureCard("Breath Timer", "Guided box breathing & 4-7-8",    Icons.Outlined.Air,          Route.BREATH_TIMER),
        // FeatureCard("Affirmations", "Positive daily affirmations",      Icons.Outlined.AutoAwesome,  Route.AFFIRMATIONS),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aham") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
            // Full-width header banner
            item(span = { GridItemSpan(2) }) {
                WelcomeBanner()
            }

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
// Welcome banner
// ---------------------------------------------------------------------------

/**
 * Full-width banner at the top of the grid with a gradient background and
 * a short Sanskrit subtitle.
 */
@Composable
private fun WelcomeBanner() {
    val gradient = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, shape = MaterialTheme.shapes.large)
            .padding(20.dp),
    ) {
        Column {
            Text(
                text       = "अहम् ब्रह्मास्मि - அகம்",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "I am the universe · Your personal companion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Feature card
// ---------------------------------------------------------------------------

/** Describes a feature card on the home screen. */
private data class FeatureCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
)

/** Square tappable card that navigates to a feature. */
@Composable
private fun FeatureCardItem(feature: FeatureCard, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector        = feature.icon,
                contentDescription = null,
                modifier           = Modifier.size(36.dp),
                tint               = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text       = feature.title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}
