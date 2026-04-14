package com.sri.aham.mantra.data

import com.sri.aham.R
import com.sri.aham.mantra.model.Mantra
import com.sri.aham.mantra.model.MantraCategory

/**
 * Catalogue of bundled mantra audio tracks.
 *
 * ## Adding a new mantra
 *  1. Drop the `.mp3` / `.ogg` file into `app/src/main/res/raw/`.
 *     The filename must be lowercase with underscores (e.g. `mantra_om.mp3`).
 *  2. Import `com.sri.aham.R` and set `audioRes = R.raw.<filename>` in the entry below.
 *  3. Sync Gradle — the resource will be available at runtime.
 *
 * Entries with `audioRes = null` appear in the list but produce no sound when selected.
 */
object MantraRepository {

    fun getAll(): List<Mantra> = listOf(

        // ── Mantra ───────────────────────────────────────────────────────────
        Mantra(
            id = "om",
            title = "Om",
            description = "The primordial sound — universal mantra representing Brahman.",
            category = MantraCategory.MANTRA,
            audioRes = R.raw.mantra_om,
        ),
        Mantra(
            id = "gayatri",
            title = "Gayatri Mantra",
            description = "Vedic solar mantra invoking divine light and wisdom.",
            category = MantraCategory.MANTRA,
            audioRes = R.raw.mantra_gayatri,
        ),
        Mantra(
            id = "mrityunjaya",
            title = "Maha Mrityunjaya",
            description = "Lord Shiva's mantra for healing and liberation.",
            category = MantraCategory.MANTRA,
            audioRes = R.raw.mantra_maha_mrityunjaya,
        ),

        // ── Guided ───────────────────────────────────────────────────────────
        Mantra(
            id = "guided_anapana_10_mins",
            title = "Anapana — 10 Minutes",
            description = "Awareness of natural breath — ideal for beginners and daily practice.",
            category = MantraCategory.GUIDED,
            audioRes = R.raw.guided_anapana_10_mins,
        ),
        Mantra(
            id = "guided_vipassana_1_hour",
            title = "Vipassana — 1 Hour",
            description = "Full-length Vipassana body-sensation meditation session.",
            category = MantraCategory.GUIDED,
            audioRes = R.raw.guided_vipassana_1_hour,
        ),

        // ── Slokha ───────────────────────────────────────────────────────────
        Mantra(
            id = "slokha_vakratunda_mahakaya",
            title = "Vakratunda Mahakaya",
            description = "Invocation of Lord Ganesha for auspicious beginnings.",
            category = MantraCategory.SLOKHA,
            audioRes = R.raw.slokha_vakratunda_mahakaya,
        ),
        Mantra(
            id = "slokha_om_mani",
            title = "Om Mani Padme Hum",
            description = "Tibetan Buddhist mantra invoking compassion and enlightenment.",
            category = MantraCategory.SLOKHA,
            audioRes = R.raw.slokha_om_mani,
        ),
    )
}
