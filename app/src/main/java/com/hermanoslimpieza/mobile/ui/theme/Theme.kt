package com.hermanoslimpieza.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = Color(0xFF005BCF),
    onPrimary = Color.White,
    secondary = Color(0xFF1677FF),
    surface = Color(0xFFF8FAFD),
    background = Color(0xFFF4F6F9),
    error = Color(0xFFB3261E)
)

@Composable
fun HermanosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        content = content
    )
}
