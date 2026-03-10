package com.sri.aham.mantra.data

import com.sri.aham.R
import com.sri.aham.mantra.model.Mantra

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
        Mantra(
            id = "om",
            title = "Om",
            description = "The primordial sound — universal mantra representing Brahman.",
            audioRes = R.raw.mantra_om, // TODO: add mantra_om.mp3 to res/raw/ then set R.raw.mantra_om
        ),
        Mantra(
            id = "gayatri",
            title = "Gayatri Mantra",
            description = "Vedic solar mantra invoking divine light and wisdom.",
            audioRes = R.raw.mantra_gayatri, // TODO: add mantra_gayatri.mp3 to res/raw/ then set R.raw.mantra_gayatri
        ),
        Mantra(
            id = "mrityunjaya",
            title = "Maha Mrityunjaya",
            description = "Lord Shiva's mantra for healing and liberation.",
            audioRes = R.raw.mantra_maha_mrityunjaya, // TODO: add mantra_maha_mrityunjaya.mp3 to res/raw/ then set R.raw.mantra_maha_mrityunjaya
        ),
    )
}
