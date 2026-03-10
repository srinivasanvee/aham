package com.sri.aham.mantra.model

import androidx.annotation.RawRes

/**
 * Represents a single mantra or meditation audio track.
 *
 * @param id           Unique identifier used for selection state and list keys.
 * @param title        Display name shown in the player and list.
 * @param description  Short description / significance of the mantra.
 * @param audioRes     Raw resource ID of the bundled audio file (res/raw/).
 *                     Null when the audio file has not yet been added to the project;
 *                     tapping Play on a null-resource mantra is a no-op.
 *
 * To add audio:
 *  1. Drop an .mp3 or .ogg file into app/src/main/res/raw/
 *  2. Set audioRes to the generated R.raw.<filename> constant in MantraRepository.
 */
data class Mantra(
    val id: String,
    val title: String,
    val description: String,
    @RawRes val audioRes: Int? = null,
)
