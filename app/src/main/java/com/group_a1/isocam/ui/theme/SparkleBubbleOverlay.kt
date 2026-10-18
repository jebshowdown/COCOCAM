package com.group_a1.isocam.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private data class BubbleSeed(
    val xRatio: Float,
    val yRatio: Float,
    val radiusDp: Float,
    val phase: Float,
    val floatAmpDp: Float
)

private data class SparkleSeed(
    val xRatio: Float,
    val yRatio: Float,
    val sizeDp: Float,
    val phase: Float
)

@Composable
fun SparkleBubbleOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isAnimDisabled = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    val progress = if (isAnimDisabled) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "SparkleBubbleFloat")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        ).value
    }

    val bubbleSeeds = remember {
        listOf(
            BubbleSeed(0.10f, 0.20f, 12f, 0.0f, 10f),
            BubbleSeed(0.25f, 0.65f, 18f, 1.2f, 14f),
            BubbleSeed(0.40f, 0.30f, 10f, 2.5f, 8f),
            BubbleSeed(0.55f, 0.80f, 22f, 0.8f, 16f),
            BubbleSeed(0.70f, 0.25f, 14f, 3.1f, 12f),
            BubbleSeed(0.88f, 0.60f, 16f, 1.9f, 10f),
            BubbleSeed(0.15f, 0.75f, 8f, 4.2f, 6f),
            BubbleSeed(0.32f, 0.15f, 20f, 2.0f, 15f),
            BubbleSeed(0.48f, 0.50f, 11f, 5.0f, 9f),
            BubbleSeed(0.62f, 0.18f, 15f, 0.5f, 11f),
            BubbleSeed(0.78f, 0.85f, 9f, 3.8f, 7f),
            BubbleSeed(0.92f, 0.35f, 13f, 1.0f, 12f)
        )
    }

    val sparkleSeeds = remember {
        listOf(
            SparkleSeed(0.18f, 0.35f, 14f, 0.0f),
            SparkleSeed(0.35f, 0.78f, 18f, 1.5f),
            SparkleSeed(0.52f, 0.22f, 12f, 3.0f),
            SparkleSeed(0.68f, 0.65f, 16f, 0.8f),
            SparkleSeed(0.82f, 0.18f, 20f, 2.2f),
            SparkleSeed(0.95f, 0.72f, 10f, 4.0f)
        )
    }

    val sparklePath = remember { Path() }
    val sparkleColor = LocalIsoCamExtras.current.sparkle

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Draw 12 Bubbles
        bubbleSeeds.forEach { b ->
            val radius = b.radiusDp.dp.toPx()
            val floatAmp = b.floatAmpDp.dp.toPx()
            val yOffset = sin((progress * 2 * PI + b.phase).toDouble()).toFloat() * floatAmp

            val cx = b.xRatio * w
            val cy = (b.yRatio * h + yOffset).coerceIn(radius, h - radius)
            val center = Offset(cx, cy)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color(0xFF4FD8E8).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center - Offset(radius * 0.3f, radius * 0.3f),
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = radius * 0.22f,
                center = center - Offset(radius * 0.35f, radius * 0.35f)
            )
        }

        // 2. Draw 6 Four-Point Sparkles
        sparkleSeeds.forEach { s ->
            val sizePx = s.sizeDp.dp.toPx()
            val phaseVal = (progress * 2 * PI + s.phase).toDouble()
            val alphaMod = ((sin(phaseVal) * 0.35 + 0.65) * sparkleColor.alpha).toFloat().coerceIn(0f, 1f)
            val scaleMod = ((sin(phaseVal) * 0.2 + 0.8)).toFloat()

            val cx = s.xRatio * w
            val cy = s.yRatio * h
            val half = (sizePx * scaleMod) / 2f

            sparklePath.reset()
            sparklePath.moveTo(cx, cy - half)
            sparklePath.quadraticTo(cx, cy, cx + half, cy)
            sparklePath.quadraticTo(cx, cy, cx, cy + half)
            sparklePath.quadraticTo(cx, cy, cx - half, cy)
            sparklePath.quadraticTo(cx, cy, cx, cy - half)
            sparklePath.close()

            drawPath(
                path = sparklePath,
                color = sparkleColor.copy(alpha = alphaMod)
            )
        }
    }
}
