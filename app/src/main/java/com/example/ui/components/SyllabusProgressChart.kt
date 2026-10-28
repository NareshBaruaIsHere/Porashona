package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectWithTopics
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data model representing a chart data item with D3-style domain values.
 */
data class ChartBarData(
    val id: String,
    val label: String,
    val valuePercent: Float, // 0f to 100f
    val completedCount: Int,
    val totalCount: Int,
    val color: Color
)

enum class ChartType {
    BAR,
    RADIAL
}

/**
 * Recharts/D3-inspired progress chart component for visualizing syllabus completion
 * across different subjects with reference lines, linear scale axes, and animated bars.
 */
@Composable
fun SyllabusProgressChart(
    subjects: List<SubjectWithTopics>,
    onSubjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var chartType by remember { mutableStateOf(ChartType.BAR) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    val chartData = remember(subjects) {
        subjects.map { item ->
            val color = try {
                Color(android.graphics.Color.parseColor(item.subject.colorTag))
            } catch (e: Exception) {
                Color(0xFF0284C7)
            }
            ChartBarData(
                id = item.subject.id,
                label = item.subject.name,
                valuePercent = (item.progress * 100f).coerceIn(0f, 100f),
                completedCount = item.completedTopics,
                totalCount = item.totalTopics,
                color = color
            )
        }
    }

    val averageCompletion = remember(chartData) {
        if (chartData.isNotEmpty()) {
            chartData.map { it.valuePercent }.average().toFloat()
        } else 0f
    }

    val totalTopics = remember(chartData) { chartData.sumOf { it.totalCount } }
    val completedTopics = remember(chartData) { chartData.sumOf { it.completedCount } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("syllabus_progress_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & Chart Type Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Syllabus Progress Chart",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Subject completion comparison",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp)
                    )
                }

                // Switch between Bar & Radial (Recharts style)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (chartType == ChartType.BAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { chartType = ChartType.BAR }
                            .testTag("chart_type_bar")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Bar Chart",
                                tint = if (chartType == ChartType.BAR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (chartType == ChartType.RADIAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { chartType = ChartType.RADIAL }
                            .testTag("chart_type_radial")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DonutLarge,
                                contentDescription = "Radial Chart",
                                tint = if (chartType == ChartType.RADIAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No subjects tracked yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                when (chartType) {
                    ChartType.BAR -> {
                        // D3 Linear Scaled Comparative Bar Chart
                        D3ComparativeBarChart(
                            data = chartData,
                            average = averageCompletion,
                            selectedId = selectedSubjectId,
                            onBarSelect = { id ->
                                selectedSubjectId = if (selectedSubjectId == id) null else id
                                onSubjectClick(id)
                            }
                        )
                    }

                    ChartType.RADIAL -> {
                        // D3-style Radial Distribution Chart
                        D3RadialProgressChart(
                            completed = completedTopics,
                            total = totalTopics,
                            data = chartData
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chart Legend & Reference Line Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp, 3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Class Avg: ${averageCompletion.toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "$completedTopics of $totalTopics topics done",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * D3-style Bar Chart layout with linear scale interpolation, grid lines at [0%, 25%, 50%, 75%, 100%],
 * and a Recharts-style dashed `<ReferenceLine>` for the class average.
 */
@Composable
private fun D3ComparativeBarChart(
    data: List<ChartBarData>,
    average: Float,
    selectedId: String?,
    onBarSelect: (String) -> Unit
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val avgLineColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // D3 X-Axis Scale Labels (0%, 25%, 50%, 75%, 100%)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 100.dp, end = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0%", "25%", "50%", "75%", "100%").forEach { tick ->
                Text(
                    text = tick,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Subject Bars
        data.forEach { item ->
            val isSelected = selectedId == item.id
            val currentAnimatedValue = item.valuePercent * animationProgress.value

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBarSelect(item.id) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-Axis Subject Label
                Row(
                    modifier = Modifier.width(96.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Canvas Bar Area with D3 Grid Lines & Animated Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                        val barHeight = size.height * 0.7f
                        val topOffset = (size.height - barHeight) / 2

                        // 1. Draw subtle background track
                        drawRoundRect(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.35f),
                            topLeft = Offset(0f, topOffset),
                            size = Size(size.width, barHeight),
                            cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                        )

                        // 2. Draw D3 Grid Line ticks at 25%, 50%, 75%
                        listOf(0.25f, 0.50f, 0.75f).forEach { fraction ->
                            val x = size.width * fraction
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // 3. Draw Animated Data Bar
                        val currentWidth = (currentAnimatedValue / 100f) * size.width
                        if (currentWidth > 0f) {
                            drawRoundRect(
                                color = item.color,
                                topLeft = Offset(0f, topOffset),
                                size = Size(currentWidth, barHeight),
                                cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                            )
                        }

                        // 4. Recharts-style ReferenceLine (Average)
                        if (average in 1f..99f) {
                            val avgX = (average / 100f) * size.width
                            drawLine(
                                color = avgLineColor,
                                start = Offset(avgX, 0f),
                                end = Offset(avgX, size.height),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Percentage label on the right
                Text(
                    text = "${item.valuePercent.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = item.color,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * D3-style Radial Progress Chart visualizing completed topics vs remaining topics.
 */
@Composable
private fun D3RadialProgressChart(
    completed: Int,
    total: Int,
    data: List<ChartBarData>
) {
    val overallPercent = if (total > 0) (completed.toFloat() / total.toFloat()) else 0f
    val animatedPercent = remember { Animatable(0f) }

    LaunchedEffect(completed, total) {
        animatedPercent.snapTo(0f)
        animatedPercent.animateTo(
            targetValue = overallPercent,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Donut Arc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(130.dp)
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                val strokeWidthPx = 14.dp.toPx()
                val arcSize = size.width - strokeWidthPx
                val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Progress sweep arc
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedPercent.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedPercent.value * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "SYLLABUS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Summary Breakdown Legend
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Column {
                    Text(
                        text = "Completed: $completed",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Topics mastered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val remaining = (total - completed).coerceAtLeast(0)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(trackColor)
                )
                Column {
                    Text(
                        text = "Remaining: $remaining",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Topics to study",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
