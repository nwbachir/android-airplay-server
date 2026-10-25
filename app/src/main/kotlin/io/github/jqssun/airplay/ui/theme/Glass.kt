package io.github.jqssun.airplay.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Couleurs de l'effet « Liquid Glass » (fond animé + verre translucide). */
@Immutable
class GlassPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val fillTop: Color,
    val fillBottom: Color,
    val highlight: Color,
    val rimLight: Color,
    val rimDark: Color,
    val indicator: Color,
)

val DarkGlass = GlassPalette(
    bgTop = Color(0xFF0A0F1F),
    bgBottom = Color(0xFF1A0F33),
    blobA = Color(0xFF2F6BFF),
    blobB = Color(0xFFB04BFF),
    blobC = Color(0xFF12CFC0),
    fillTop = Color.White.copy(alpha = 0.16f),
    fillBottom = Color.White.copy(alpha = 0.06f),
    highlight = Color.White.copy(alpha = 0.14f),
    rimLight = Color.White.copy(alpha = 0.60f),
    rimDark = Color.White.copy(alpha = 0.12f),
    indicator = Color.White.copy(alpha = 0.22f),
)

val LightGlass = GlassPalette(
    bgTop = Color(0xFFE6EFFF),
    bgBottom = Color(0xFFF5E8FF),
    blobA = Color(0xFF7FB0FF),
    blobB = Color(0xFFD59BFF),
    blobC = Color(0xFF8FEFE0),
    fillTop = Color.White.copy(alpha = 0.70f),
    fillBottom = Color.White.copy(alpha = 0.38f),
    highlight = Color.White.copy(alpha = 0.55f),
    rimLight = Color.White.copy(alpha = 0.95f),
    rimDark = Color.Black.copy(alpha = 0.08f),
    indicator = Color.White.copy(alpha = 0.85f),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }

/** Fond dégradé avec des « blobs » lumineux qui dérivent lentement derrière le verre. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = LocalGlass.current
    val transition = rememberInfiniteTransition(label = "glass-bg")
    val a = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    val b = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
            // a/b are read in the draw phase only: no recomposition while animating
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = maxOf(w, h) * 0.65f
                fun blob(c: Color, cx: Float, cy: Float) {
                    val center = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c.copy(alpha = 0.60f), c.copy(alpha = 0f)),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )
                }
                blob(p.blobA, w * (0.10f + 0.40f * a.value), h * (0.10f + 0.15f * b.value))
                blob(p.blobB, w * (0.95f - 0.35f * b.value), h * (0.45f + 0.15f * a.value))
                blob(p.blobC, w * (0.20f + 0.35f * b.value), h * (0.95f - 0.20f * a.value))
            },
        content = content
    )
}

private fun Color.scaleAlpha(k: Float) = copy(alpha = (alpha * k).coerceAtMost(1f))

/**
 * Surface « verre » : remplissage translucide en dégradé, reflet spéculaire en haut
 * et bordure lumineuse en biais (clair en haut à gauche, discret en bas à droite).
 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp), strong: Boolean = false): Modifier = composed {
    val p = LocalGlass.current
    val k = if (strong) 1.4f else 1f
    val rim = remember(p) {
        Brush.linearGradient(listOf(p.rimLight, p.rimLight.copy(alpha = 0f), p.rimDark))
    }
    this
        .clip(shape)
        .drawBehind {
            drawRect(Brush.verticalGradient(listOf(p.fillTop.scaleAlpha(k), p.fillBottom.scaleAlpha(k))))
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(p.highlight, p.highlight.copy(alpha = 0f)),
                    endY = size.height * 0.45f
                )
            )
        }
        .border(1.dp, rim, shape)
}
