package com.sri.aham.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Meditation-themed palette — used as fallback when dynamic color is off
// (Android < 12, or user has dynamic color disabled)
//
// Inspired by the colours of a traditional puja:
//   • Deep Violet / Indigo  → spiritual depth, focus
//   • Warm Saffron / Amber  → sacred fire, energy
//   • Sage Green (tertiary) → calm, healing
// ---------------------------------------------------------------------------

// Light scheme
val Indigo40        = Color(0xFF4B3CA7)   // primary
val IndigoGrey40    = Color(0xFF625D8A)   // secondary
val Sage40          = Color(0xFF2B6E5F)   // tertiary

val Indigo10        = Color(0xFF14005C)
val IndigoContainer = Color(0xFFE6DEFF)   // primaryContainer
val Saffron10       = Color(0xFF3B1700)
val SaffronContainer = Color(0xFFFFDBC8)  // secondaryContainer

// Dark scheme
val Indigo80        = Color(0xFFCBBEFF)
val IndigoGrey80    = Color(0xFFCBC3F1)
val Sage80          = Color(0xFF93D9C8)

val IndigoContainer80 = Color(0xFF332387)
val SaffronContainer80 = Color(0xFF6B3000)
