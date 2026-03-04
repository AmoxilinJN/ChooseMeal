package com.choosemeal.app.ui.screen

import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.domain.model.DecisionMode
import com.choosemeal.app.domain.model.DecisionResult
import com.choosemeal.app.domain.model.FlavorFilter
import com.choosemeal.app.domain.model.MealOption
import com.choosemeal.app.domain.model.PriceRangeFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RandomDecisionScreen(
    modifier: Modifier = Modifier,
    cafeterias: List<CafeteriaEntity>,
    floors: List<FloorEntity>,
    options: List<MealOption>,
    selectedCafeteriaId: Long?,
    selectedFloorId: Long?,
    selectedPriceRangeFilter: PriceRangeFilter,
    selectedFlavorFilter: FlavorFilter,
    decisionResult: DecisionResult?,
    isRolling: Boolean,
    animationToken: Long,
    animationsEnabled: Boolean,
    hapticsEnabled: Boolean,
    onSelectCafeteria: (Long?) -> Unit,
    onSelectFloor: (Long?) -> Unit,
    onSelectPriceRange: (PriceRangeFilter) -> Unit,
    onSelectFlavor: (FlavorFilter) -> Unit,
    onSpin: () -> Unit,
    onDrawPick: (MealOption) -> Unit,
) {
    var mode by remember { mutableStateOf(DecisionMode.SPIN) }
    var revealedResult by remember { mutableStateOf<DecisionResult?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(decisionResult?.timestamp, isRolling) {
        val latest = decisionResult ?: return@LaunchedEffect
        if (latest.mode == DecisionMode.SPIN && isRolling) {
            return@LaunchedEffect
        }
        revealedResult = latest
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Meal Decision",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "在校园范围内快速做决定，减少纠结成本。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                shape = RoundedCornerShape(24.dp),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.FilterList, contentDescription = null)
                    Text("随机范围", style = MaterialTheme.typography.titleMedium)
                }
                FilterDropDown(
                    title = "食堂",
                    selectedText = cafeterias.firstOrNull { it.id == selectedCafeteriaId }?.name ?: "全部食堂",
                    entries = listOf(null to "全部食堂") + cafeterias.map { it.id to it.name },
                    onSelect = onSelectCafeteria,
                )
                FilterDropDown(
                    title = "楼层",
                    selectedText = floors.firstOrNull { it.id == selectedFloorId }?.name ?: "全部楼层",
                    entries = listOf(null to "全部楼层") + floors.map { it.id to it.name },
                    onSelect = onSelectFloor,
                )
                FilterDropDown(
                    title = "预期价格",
                    selectedText = selectedPriceRangeFilter.label,
                    entries = PriceRangeFilter.entries.map { it to it.label },
                    onSelect = onSelectPriceRange,
                )
                FilterDropDown(
                    title = "口味偏好",
                    selectedText = selectedFlavorFilter.label,
                    entries = FlavorFilter.entries.map { it to it.label },
                    onSelect = onSelectFlavor,
                )
                Text(
                    text = "提示：在伙食标签中可写入“￥18、微辣”等信息，以提升筛选准确度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = mode == DecisionMode.SPIN,
                onClick = { mode = DecisionMode.SPIN },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Outlined.Casino, contentDescription = null) },
            ) { Text("转盘") }
            SegmentedButton(
                selected = mode == DecisionMode.DRAW,
                onClick = { mode = DecisionMode.DRAW },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
            ) { Text("候选") }
        }

        if (mode == DecisionMode.SPIN) {
            SpinPanel(
                options = options,
                decisionResult = decisionResult,
                animationToken = animationToken,
                animationsEnabled = animationsEnabled,
                onSpin = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSpin()
                },
                isRolling = isRolling,
            )
        } else {
            CandidatePanel(
                options = options,
                onPick = { option ->
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDrawPick(option)
                },
                animationsEnabled = animationsEnabled,
            )
        }

        ResultPanel(decisionResult = revealedResult)
    }
}

@Composable
private fun SpinPanel(
    options: List<MealOption>,
    decisionResult: DecisionResult?,
    animationToken: Long,
    animationsEnabled: Boolean,
    onSpin: () -> Unit,
    isRolling: Boolean,
) {
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var lastHandledToken by remember { mutableLongStateOf(animationToken) }
    val wheelOptions = remember(options) { options }
    val optionNames = wheelOptions.map { it.mealName }.ifEmpty { listOf("暂无可选") }

    LaunchedEffect(animationToken, optionNames.size) {
        if (animationToken == 0L || animationToken == lastHandledToken) return@LaunchedEffect
        lastHandledToken = animationToken

        val result = decisionResult ?: return@LaunchedEffect
        if (result.mode != DecisionMode.SPIN) return@LaunchedEffect
        if (wheelOptions.isEmpty()) return@LaunchedEffect

        val index = wheelOptions.indexOfFirst {
            it.mealName == result.meal &&
                it.floorName == result.floor &&
                it.cafeteriaName == result.cafeteria
        }.takeIf { it >= 0 }
            ?: wheelOptions.indexOfFirst { it.mealName == result.meal }.takeIf { it >= 0 }
            ?: 0

        val sweep = 360f / optionNames.size
        val desiredMod = (360f - ((index + 0.5f) * sweep % 360f)) % 360f
        val currentMod = ((targetRotation % 360f) + 360f) % 360f
        val alignDelta = (desiredMod - currentMod + 360f) % 360f
        targetRotation += 5 * 360f + alignDelta
    }

    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = if (animationsEnabled) 1650 else 1,
            easing = FastOutSlowInEasing,
        ),
        label = "wheel_rotation",
    )

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val buttonColor by animateColorAsState(
        targetValue = if (isRolling) primary.copy(alpha = 0.88f) else primary,
        label = "wheel_center_button_color",
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            shape = RoundedCornerShape(24.dp),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Decision Wheel", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier.size(308.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .size(298.dp)
                        .rotate(animatedRotation),
                ) {
                    val segmentCount = optionNames.size
                    val sweep = 360f / segmentCount
                    val radius = size.minDimension / 2f
                    val prizeRadius = radius * 0.90f
                    val labelRadius = prizeRadius * 0.70f
                    val blockOuterWidth = radius * 0.10f

                    drawBlocks(
                        radius = radius,
                        outerWidth = blockOuterWidth,
                        blockBg = primaryContainer.copy(alpha = 0.38f),
                        blockStroke = primary,
                        blockInner = surface,
                    )

                    val colors = listOf(
                        Color(0xFFE9E0FA),
                        Color(0xFFDCCEFF),
                        Color(0xFFD2C0FB),
                        Color(0xFFEAD9FF),
                        Color(0xFFCBB8F6),
                        Color(0xFFE3D5F8),
                    )

                    optionNames.forEachIndexed { index, rawLabel ->
                        val startAngle = -90f + index * sweep
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = center.x - prizeRadius,
                                y = center.y - prizeRadius,
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                width = prizeRadius * 2f,
                                height = prizeRadius * 2f,
                            ),
                        )

                        val edgeAngle = Math.toRadians(startAngle.toDouble())
                        val edgeX = center.x + cos(edgeAngle).toFloat() * prizeRadius
                        val edgeY = center.y + sin(edgeAngle).toFloat() * prizeRadius
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = center,
                            end = androidx.compose.ui.geometry.Offset(edgeX, edgeY),
                            strokeWidth = 2f,
                        )

                        val label = compactWheelLabel(rawLabel, segmentCount)
                        val textSize = wheelTextSize(segmentCount)
                        val angle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                        val x = center.x + cos(angle).toFloat() * labelRadius
                        val y = center.y + sin(angle).toFloat() * labelRadius

                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            y + textSize * 0.32f,
                            Paint().apply {
                                color = android.graphics.Color.rgb(
                                    (onSurface.red * 255).toInt(),
                                    (onSurface.green * 255).toInt(),
                                    (onSurface.blue * 255).toInt(),
                                )
                                this.textSize = textSize
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                                isFakeBoldText = true
                            },
                        )
                    }

                    drawCircle(
                        color = surface,
                        radius = radius * 0.34f,
                        center = center,
                    )
                }

                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(30.dp)
                        .height(26.dp),
                ) {
                    val p = Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path = p, color = primary)
                }

                Box(
                    modifier = Modifier.size(108.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .width(26.dp)
                            .height(18.dp),
                    ) {
                        val pointer = Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(0f, size.height)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(pointer, color = buttonColor)
                    }
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(buttonColor, CircleShape)
                            .border(2.dp, surface, CircleShape)
                            .clickable(enabled = options.isNotEmpty() && !isRolling) { onSpin() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isRolling) "转动中" else "开始",
                            color = onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                    }
                }
            }

            Text(
                text = "当前候选 ${wheelOptions.size} 项",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun CandidatePanel(
    options: List<MealOption>,
    animationsEnabled: Boolean,
    onPick: (MealOption) -> Unit,
) {
    var candidates by remember(options) { mutableStateOf(pickCandidateBatch(options, 3)) }
    var pickedMealId by remember(options) { mutableLongStateOf(-1L) }

    fun regenerate() {
        candidates = pickCandidateBatch(options, 3)
        pickedMealId = -1L
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            shape = RoundedCornerShape(24.dp),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Style, contentDescription = null)
                    Text("候选精选", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = { regenerate() }, enabled = options.isNotEmpty()) {
                    Text("换一组")
                }
            }

            if (candidates.isEmpty()) {
                Text("当前筛选条件无可选项", color = MaterialTheme.colorScheme.outline)
            } else {
                candidates.forEach { candidate ->
                    val picked = pickedMealId == candidate.mealId
                    val scale by animateFloatAsState(
                        targetValue = if (picked) 1.02f else 1f,
                        animationSpec = tween(if (animationsEnabled) 220 else 1, easing = FastOutSlowInEasing),
                        label = "candidate_scale_${candidate.mealId}",
                    )
                    val container by animateColorAsState(
                        targetValue = if (picked) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                        } else {
                            Color(0xFFF8F6FC)
                        },
                        label = "candidate_color_${candidate.mealId}",
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .border(
                                width = if (picked) 1.4.dp else 1.dp,
                                color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .clickable(enabled = pickedMealId == -1L) {
                                pickedMealId = candidate.mealId
                                onPick(candidate)
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = container),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(candidate.mealName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${candidate.cafeteriaName} · ${candidate.floorName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Text(
                                text = if (picked) "已选" else "点击选择",
                                color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(decisionResult: DecisionResult?) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            shape = RoundedCornerShape(24.dp),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("本次结果", style = MaterialTheme.typography.titleMedium)
            if (decisionResult == null) {
                Text("暂无结果，先点“开始”或在候选里选择。", color = MaterialTheme.colorScheme.outline)
            } else {
                Text("食堂：${decisionResult.cafeteria}")
                Text("楼层：${decisionResult.floor}")
                Text("伙食：${decisionResult.meal}", fontWeight = FontWeight.Bold)
                val time = remember(decisionResult.timestamp) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(decisionResult.timestamp))
                }
                Text("时间：$time", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T> FilterDropDown(
    title: String,
    selectedText: String,
    entries: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            TextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(14.dp),
            ) {
                entries.forEach { (id, label) ->
                    val isSelected = label == selectedText
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Text(
                                    text = "当前",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(id)
                        },
                    )
                }
            }
        }
    }
}

private fun compactWheelLabel(name: String, segmentCount: Int): String {
    val maxChars = when {
        segmentCount <= 4 -> 8
        segmentCount <= 8 -> 5
        segmentCount <= 16 -> 3
        else -> 2
    }
    return if (name.length <= maxChars) name else name.take(maxChars)
}

private fun wheelTextSize(segmentCount: Int): Float {
    return when {
        segmentCount <= 4 -> 36f
        segmentCount <= 8 -> 28f
        segmentCount <= 16 -> 21f
        else -> 16f
    }
}

private fun DrawScope.drawBlocks(
    radius: Float,
    outerWidth: Float,
    blockBg: Color,
    blockStroke: Color,
    blockInner: Color,
) {
    drawCircle(
        color = blockBg,
        radius = radius,
        center = center,
    )
    drawCircle(
        color = blockStroke,
        radius = radius - outerWidth * 0.40f,
        center = center,
        style = Stroke(width = outerWidth),
    )
    drawCircle(
        color = blockInner,
        radius = radius - outerWidth * 1.45f,
        center = center,
    )
}

private fun pickCandidateBatch(options: List<MealOption>, count: Int): List<MealOption> {
    return options
        .distinctBy { it.mealId }
        .shuffled()
        .take(count.coerceAtLeast(1))
}
