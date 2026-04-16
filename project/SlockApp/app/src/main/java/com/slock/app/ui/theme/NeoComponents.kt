package com.slock.app.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Shadow modifiers
fun Modifier.neoShadow(
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    color: Color = Color.Black
): Modifier = this.drawBehind {
    drawNeoShadow(offsetX.toPx(), offsetY.toPx(), color)
}

fun Modifier.neoShadowSmall(
    color: Color = Color.Black
): Modifier = neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = color)

private fun DrawScope.drawNeoShadow(offsetX: Float, offsetY: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(offsetX, offsetY),
        size = Size(size.width, size.height)
    )
}

// Neo-Brutalism pressable icon button with shadow animation
@Composable
fun NeoPressableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 36.dp,
    backgroundColor: Color = White,
    borderWidth: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 2.dp,
        animationSpec = tween(100),
        label = "shadow"
    )
    val buttonOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = tween(100),
        label = "offset"
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(x = buttonOffset, y = buttonOffset)
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset)
            .background(backgroundColor)
            .border(borderWidth, Black, RectangleShape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// Neo-Brutalism Card
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = White,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .neoShadow(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(3.dp, Black),
        content = { content() }
    )
}

// Neo-Brutalism Primary Button (Yellow, press animation)
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Yellow,
    contentColor: Color = Black
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        animationSpec = tween(100),
        label = "shadow"
    )
    val buttonOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = tween(100),
        label = "offset"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .offset(x = buttonOffset, y = buttonOffset)
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset),
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        border = BorderStroke(3.dp, Black),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 1.sp
            )
        )
    }
}

// Neo-Brutalism Secondary Button (lighter style)
@Composable
fun NeoButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Cyan,
    contentColor: Color = Black
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 2.dp,
        animationSpec = tween(100),
        label = "shadow"
    )
    val buttonOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 0.dp,
        animationSpec = tween(100),
        label = "offset"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .offset(x = buttonOffset, y = buttonOffset)
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(2.dp, Black),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

// Neo-Brutalism Label
@Composable
fun NeoLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 0.5.sp
        ),
        color = Black,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// Neo-Brutalism Text Field
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    focusHighlight: Color = Cyan,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RectangleShape,
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Black
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.neoShadow(4.dp, 4.dp, focusHighlight)
                else Modifier.neoShadowSmall()
            )
            .border(2.dp, Black, RectangleShape)
    )
}

// Neo-Brutalism Divider with text
@Composable
fun NeoDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = Black
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Divider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = Black
        )
    }
}

@Composable
fun NetworkStatusBanner(
    isConnected: Boolean,
    isReconnecting: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isConnected) return
    val bgColor = if (isReconnecting) Color(0xFFFF9F43) else Error
    val text = if (isReconnecting) "正在重新连接..." else "网络连接已断开"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
fun NeoErrorState(
    message: String,
    subtitle: String? = "请检查网络后重试",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "\u26A0", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Black,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            NeoButton(
                text = "重试",
                onClick = onRetry,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
    }
}

// Inline text styling: @mentions (Cyan/30 + bold) and `inline code` (Yellow/40 + mono)
private val inlinePattern = Regex("`[^`]+`|@[\\w.-]+")

fun buildMentionAnnotatedString(content: String): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        inlinePattern.findAll(content).forEach { match ->
            append(content.substring(lastIndex, match.range.first))
            val value = match.value
            if (value.startsWith("`") && value.endsWith("`")) {
                // Inline code
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Yellow.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(value.removeSurrounding("`"))
                }
            } else {
                // @mention
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        background = Cyan.copy(alpha = 0.3f)
                    )
                ) {
                    append(value)
                }
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < content.length) {
            append(content.substring(lastIndex))
        }
    }
}

// Block-level message content renderer: code blocks, quotes, and inline text
private val codeBlockRegex = Regex("```(?:\\w*\\n)?([\\s\\S]*?)```")

@Composable
fun NeoMessageContent(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF222222)
) {
    // Split content by code blocks
    val segments = mutableListOf<Pair<String, String>>() // type to content
    var lastIndex = 0

    codeBlockRegex.findAll(content).forEach { match ->
        val before = content.substring(lastIndex, match.range.first)
        if (before.isNotBlank()) segments.add("text" to before.trim())
        segments.add("code" to match.groupValues[1].trimEnd())
        lastIndex = match.range.last + 1
    }
    val remaining = content.substring(lastIndex)
    if (remaining.isNotBlank()) segments.add("text" to remaining.trim())

    if (segments.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        segments.forEach { (type, text) ->
            when (type) {
                "code" -> {
                    // Code block: black bg, white mono text
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Black)
                            .border(2.dp, Black, RectangleShape)
                            .padding(12.dp)
                    )
                }
                else -> {
                    // Parse lines for quotes
                    val lines = text.split("\n")
                    var i = 0
                    while (i < lines.size) {
                        val line = lines[i]
                        if (line.trimStart().startsWith(">")) {
                            // Quote block: collect consecutive quote lines
                            val quoteLines = mutableListOf<String>()
                            while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                                quoteLines.add(lines[i].trimStart().removePrefix(">").trimStart())
                                i++
                            }
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(Black)
                                )
                                Text(
                                    text = buildMentionAnnotatedString(quoteLines.joinToString("\n")),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 13.sp
                                    ),
                                    color = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        } else {
                            // Normal text lines (collect until next quote or end)
                            val normalLines = mutableListOf<String>()
                            while (i < lines.size && !lines[i].trimStart().startsWith(">")) {
                                normalLines.add(lines[i])
                                i++
                            }
                            Text(
                                text = buildMentionAnnotatedString(normalLines.joinToString("\n")),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Skeleton shimmer animation base
@Composable
private fun skeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    return alpha
}

@Composable
private fun SkeletonBox(
    modifier: Modifier = Modifier,
    alpha: Float = skeletonAlpha()
) {
    Box(
        modifier = modifier
            .background(Black.copy(alpha = 0.1f * (alpha / 0.3f)))
    )
}

// Channel list skeleton
@Composable
fun NeoSkeletonChannelList(count: Int = 5) {
    val alpha = skeletonAlpha()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(100.dp)
                .height(12.dp)
                .padding(vertical = 2.dp),
            alpha = alpha
        )
        repeat(count) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Black.copy(alpha = 0.1f), RectangleShape)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonBox(modifier = Modifier.size(36.dp), alpha = alpha)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp), alpha = alpha)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp), alpha = alpha)
                }
            }
        }
    }
}

// Message list skeleton
@Composable
fun NeoSkeletonMessageList(count: Int = 6) {
    val alpha = skeletonAlpha()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(count) { i ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBox(modifier = Modifier.size(36.dp), alpha = alpha)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBox(modifier = Modifier.width(80.dp).height(14.dp), alpha = alpha)
                        SkeletonBox(modifier = Modifier.width(40.dp).height(10.dp), alpha = alpha)
                    }
                    SkeletonBox(
                        modifier = Modifier.fillMaxWidth(if (i % 2 == 0) 0.9f else 0.7f).height(12.dp),
                        alpha = alpha
                    )
                    if (i % 3 == 0) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp), alpha = alpha)
                    }
                }
            }
        }
    }
}

// Card list skeleton (members, agents, tasks, threads)
@Composable
fun NeoSkeletonCardList(count: Int = 4) {
    val alpha = skeletonAlpha()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(3.dp, 3.dp, Black.copy(alpha = 0.05f))
                    .border(2.dp, Black.copy(alpha = 0.1f), RectangleShape)
                    .padding(12.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonBox(modifier = Modifier.size(40.dp), alpha = alpha)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp), alpha = alpha)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp), alpha = alpha)
                }
                SkeletonBox(modifier = Modifier.width(50.dp).height(18.dp), alpha = alpha)
            }
        }
    }
}
