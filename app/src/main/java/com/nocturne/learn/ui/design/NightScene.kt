package com.nocturne.learn.ui.design

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.nocturne.learn.ui.theme.NocturneColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

val LocalNightHaze = staticCompositionLocalOf<HazeState> {
    error("NightScene 未提供 HazeState")
}

/**
 * 场景先于玻璃：深墨夜景 + 三个柔和光源光斑 + 2%~3% 噪点。
 * 光斑会极慢地漂移，玻璃后面的世界才是"活"的。
 */
@Composable
fun NightScene(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeState = remember { HazeState() }
    val transition = rememberInfiniteTransition(label = "night")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    val noise = remember { noiseTexture() }
    val noiseBrush = remember(noise) {
        ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
    }

    CompositionLocalProvider(LocalNightHaze provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NocturneColors.Night950),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeSource(hazeState),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawNight(drift)
                }
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(brush = noiseBrush, alpha = 0.85f)
                }
            }
            content()
        }
    }
}

private fun DrawScope.drawNight(drift: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                NocturneColors.Night950,
                NocturneColors.Night900,
                NocturneColors.Night950,
            ),
        ),
    )

    val maxDim = size.maxDimension

    // 左上：月光钢蓝
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x2A7C9CC4),
                Color(0x007C9CC4),
            ),
            center = Offset(
                x = size.width * (0.10f + 0.04f * drift),
                y = size.height * 0.02f,
            ),
            radius = maxDim * 0.56f,
        ),
    )

    // 右下：深钢蓝
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x2E33517A),
                Color(0x0033517A),
            ),
            center = Offset(
                x = size.width * 0.96f,
                y = size.height * (0.82f - 0.04f * drift),
            ),
            radius = maxDim * 0.58f,
        ),
    )

    // 香槟金微光：全屏最多一处
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x1FE4B863),
                Color(0x00E4B863),
            ),
            center = Offset(
                x = size.width * 0.82f,
                y = size.height * (0.10f + 0.02f * drift),
            ),
            radius = size.width * 0.34f,
        ),
    )
}

private fun noiseTexture(): androidx.compose.ui.graphics.ImageBitmap {
    val size = 180
    val pixels = IntArray(size * size)
    val random = java.util.Random(20261104L)
    for (i in pixels.indices) {
        // 单像素随机透明度，叠加后约 2%~3% 颗粒
        val alpha = random.nextInt(10)
        pixels[i] = (alpha shl 24) or 0x00FFFFFF
    }
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}
