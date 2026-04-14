package com.sri.aham.mantra.model

import androidx.annotation.RawRes

enum class MantraCategory { MANTRA, GUIDED, SLOKHA }

/**
 * Represents a single mantra, guided meditation, or slokha audio track.
 *
 * @param id           Unique identifier used for selection state and list keys.
 * @param title        Display name shown in the player and list.
 * @param description  Short description / significance of the track.
 * @param category     Tab this track belongs to — [MantraCategory].
 * @param audioRes     Raw resource ID of the bundled audio file (res/raw/).
 *                     Null when the audio file has not yet been added to the project.
 */
data class Mantra(
    val id: String,
    val title: String,
    val description: String,
    val category: MantraCategory = MantraCategory.MANTRA,
    @RawRes val audioRes: Int? = null,
)
