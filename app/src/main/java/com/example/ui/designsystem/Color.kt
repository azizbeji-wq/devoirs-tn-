package com.example.ui.designsystem

import androidx.compose.ui.graphics.Color

// Couleurs Fondamentales
val PrimaryLight = Color(0xFF124E78)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD6E7F5)
val OnPrimaryContainerLight = Color(0xFF07233D)

val SecondaryLight = Color(0xFF4E5B6B)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD0DBE8)
val OnSecondaryContainerLight = Color(0xFF162230)

val BackgroundLight = Color(0xFFFAF7F2)
val OnBackgroundLight = Color(0xFF1A1C1E)
val SurfaceLight = Color(0xFFFFFDF9)
val OnSurfaceLight = Color(0xFF1A1C1E)
val SurfaceVariantLight = Color(0xFFF0EBE1)
val OnSurfaceVariantLight = Color(0xFF4E5B6B) // WCAG AA compliant (4.5:1 on SurfaceLight)
val OutlineLight = Color(0xFF74777F)
val OutlineVariantLight = Color(0xFFC4C6D0)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// Thème Sombre (Dark Theme)
val PrimaryDark = Color(0xFF8CC8FF)
val OnPrimaryDark = Color(0xFF003355)
val PrimaryContainerDark = Color(0xFF004A7A)
val OnPrimaryContainerDark = Color(0xFFD6E7F5)

val SecondaryDark = Color(0xFFB4C8D8)
val OnSecondaryDark = Color(0xFF1E3242)
val SecondaryContainerDark = Color(0xFF35495A)
val OnSecondaryContainerDark = Color(0xFFD0DBE8)

val BackgroundDark = Color(0xFF0D141E)
val OnBackgroundDark = Color(0xFFE2E2E6)
val SurfaceDark = Color(0xFF141E2B)
val OnSurfaceDark = Color(0xFFE2E2E6)
val SurfaceVariantDark = Color(0xFF1E2B3D)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
val OutlineDark = Color(0xFF8E9099)
val OutlineVariantDark = Color(0xFF44474E)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

/**
 * Couleurs sémantiques personnalisées (Succès, Avertissement, Info)
 */
object SemanticColors {
    val SuccessLight = Color(0xFF1F7A5A)
    val SuccessDark = Color(0xFF6EE0B4)
    val WarningLight = Color(0xFFB3740D)
    val WarningDark = Color(0xFFF5C26B)
    val InfoLight = Color(0xFF124E78) // Same as primary
    val InfoDark = Color(0xFF8CC8FF)
}
