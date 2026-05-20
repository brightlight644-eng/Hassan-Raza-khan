package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CustomLightColorScheme = lightColorScheme(
    primary = HighDensityM3Primary,
    onPrimary = HighDensityM3OnPrimary,
    background = HighDensityBg,
    onBackground = HighDensityTextDark,
    surface = PureWhite,
    onSurface = HighDensityTextDark,
    surfaceVariant = RecentActivityBg,
    onSurfaceVariant = HighDensitySubtext,
    secondary = ExpenseBar,
    tertiary = RevenueBar
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // We intentionally enforce our stunning high density light Material 3 theme 
    // defining the visual brand of Small Business Ledger.
    MaterialTheme(
        colorScheme = CustomLightColorScheme,
        typography = Typography,
        content = content
    )
}
