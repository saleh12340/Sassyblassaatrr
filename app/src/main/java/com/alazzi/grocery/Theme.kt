package com.alazzi.grocery

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Grocery Emerald & Golden Amber Palette
val EmeraldPrimary = Color(0xFF0D6E44)
val EmeraldLight = Color(0xFF1E8254)
val EmeraldDark = Color(0xFF084B2E)
val EmeraldContainer = Color(0xFFD3EEDF)
val OnEmeraldContainer = Color(0xFF032717)

val AmberSecondary = Color(0xFFD97706)
val AmberContainer = Color(0xFFFEF3C7)
val OnAmberContainer = Color(0xFF78350F)

val DebtRed = Color(0xFFDC2626)
val DebtRedContainer = Color(0xFFFEE2E2)
val OnDebtRed = Color(0xFF7F1D1D)

val DarkSurface = Color(0xFF0F172A)
val DarkCard = Color(0xFF1E293B)
val LightSurface = Color(0xFFF8FAFC)
val LightCard = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    background = LightSurface,
    onBackground = Color(0xFF0F172A),
    surface = LightCard,
    onSurface = Color(0xFF0F172A),
    error = DebtRed,
    errorContainer = DebtRedContainer,
    onError = Color.White,
    onErrorContainer = OnDebtRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF78350F),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color(0xFFFDE68A),
    background = DarkSurface,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkCard,
    onSurface = Color(0xFFF1F5F9),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onError = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun AlAzziGroceryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
