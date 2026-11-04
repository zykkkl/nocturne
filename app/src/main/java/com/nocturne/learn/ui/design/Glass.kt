package com.nocturne.learn.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nocturne.learn.ui.theme.NocturneColors
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val SpringEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private val NocturneGlassStyle = HazeStyle(
    backgroundColor = Color.Transparent,
    tint = HazeTint(Color.White.copy(alpha = 0.03f)),
    blurRadius = 40.dp,
    noiseFactor = 0.02f,
    fallbackTint = HazeTint(Color.White.copy(alpha = 0.06f)),
)

/**
 * 无色白玻璃：5%~12% 表面 + 40dp 背景模糊 + 方向性边框。
 * 颜色全部来自身后的夜景，面板自身不上色。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val hazeState = LocalNightHaze.current
    Column(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = Color(0x66030712),
                spotColor = Color(0x66030712),
            )
            .clip(shape)
            .hazeEffect(state = hazeState, style = NocturneGlassStyle)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color.White.copy(alpha = 0.025f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.08f),
                    ),
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun ChampagneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "champagne-press",
    )
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = NocturneColors.Champagne.copy(alpha = 0.35f),
                spotColor = NocturneColors.Champagne.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(NocturneColors.ChampagneLight, NocturneColors.Champagne),
                ),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .heightIn(min = 54.dp)
            .padding(horizontal = 24.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = NocturneColors.Ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = NocturneColors.TextSecondary,
        )
    }
}

@Composable
fun NocturneChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    val background = if (selected) {
        NocturneColors.Champagne.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }
    val borderColor = if (selected) {
        NocturneColors.Champagne.copy(alpha = 0.40f)
    } else {
        Color.White.copy(alpha = 0.15f)
    }
    val textColor = if (selected) NocturneColors.ChampagneLight else NocturneColors.TextSecondary
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}

@Composable
fun NocturneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint) } },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(18.dp),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.04f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
            focusedBorderColor = NocturneColors.Champagne.copy(alpha = 0.60f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            cursorColor = NocturneColors.Champagne,
            focusedLabelColor = NocturneColors.ChampagneLight,
            unfocusedLabelColor = NocturneColors.TextTertiary,
            focusedPlaceholderColor = NocturneColors.TextTertiary,
            unfocusedPlaceholderColor = NocturneColors.TextTertiary,
        ),
    )
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 96.dp,
    strokeWidth: Dp = 7.dp,
    caption: String? = null,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = SpringEasing),
        label = "ring-progress",
    )
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.001f) {
                drawArc(
                    brush = Brush.linearGradient(
                        listOf(NocturneColors.Champagne, NocturneColors.Moon),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animated * 100).toInt()}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary,
                )
            }
        }
    }
}

/**
 * 贾维斯光球：呼吸中的一点香槟金。
 */
@Composable
fun JarvisOrb(
    modifier: Modifier = Modifier,
    diameter: Dp = 54.dp,
    active: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "orb-pulse",
    )
    Canvas(modifier = modifier.size(diameter)) {
        val radius = size.minDimension / 2f
        val center = center
        if (active) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        NocturneColors.Champagne.copy(alpha = 0.55f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * 0.7f * pulse,
                ),
                radius = radius * 0.7f * pulse,
            )
            drawCircle(
                color = NocturneColors.Champagne.copy(alpha = 0.32f),
                radius = radius * 0.95f,
                style = Stroke(width = 1.dp.toPx()),
            )
        } else {
            drawCircle(color = Color.White.copy(alpha = 0.18f), radius = radius * 0.5f)
        }
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = radius * 0.10f)
    }
}

@Composable
fun RowDot(color: Color, diameter: Dp = 6.dp) {
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        RowDot(NocturneColors.Champagne.copy(alpha = 0.8f), 5.dp)
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = NocturneColors.TextTertiary,
        )
    }
}
