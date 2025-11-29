package com.example.sazoolotto.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SoftGold,
    secondary = FireAccent,
    background = PaperBackground,
    surface = PaperSurface,
    onPrimary = PencilDark,
    onSecondary = PencilDark,
    onBackground = PencilDark,
    onSurface = PencilDark,
    outline = PencilLine
)

@Composable
fun SazooLottoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        // shapes = MaterialTheme.shapes 로 해도 되고,
        // 어차피 기본값이라서 아예 빼도 된다.
        content = content
    )
}
