package com.hermanoslimpieza.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandBlue = Color(0xFF13287E)
val BrandBlueDark = Color(0xFF09174E)
val BrandBlueSoft = Color(0xFFE8ECFF)
val BrandYellow = Color(0xFFFFF323)
val Ink = Color(0xFF111827)
val Muted = Color(0xFF667085)
val AppBackground = Color(0xFFF5F6FA)
val IncomingBubble = Color(0xFFFFFFFF)
val OutgoingBubble = BrandBlue

private val Scheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandYellow,
    onSecondary = BrandBlueDark,
    secondaryContainer = Color(0xFFFFF9A6),
    onSecondaryContainer = BrandBlueDark,
    background = AppBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0F2F7),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD7DBE5),
    outlineVariant = Color(0xFFE9ECF3),
    error = Color(0xFFB42318)
)

@Composable
fun HermanosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
