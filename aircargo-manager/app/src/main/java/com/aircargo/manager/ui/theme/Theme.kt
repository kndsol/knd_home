package com.aircargo.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF14304F)
val NavyCard = Color(0xFF244766)
val NavyText = Color(0xFFB9C8DA)
val Accent = Color(0xFF2E6BD6)
val MenuSelected = Color(0xFFE3EBF6)
val BgGray = Color(0xFFF3F5F8)
val Prototype = Color(0xFF2ECC71)

private val Colors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    background = BgGray,
    surface = Color.White,
    surfaceVariant = Color(0xFFEDF1F6),
)

@Composable
fun AirCargoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
