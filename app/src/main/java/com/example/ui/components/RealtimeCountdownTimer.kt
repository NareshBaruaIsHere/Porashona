package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.readableOnSurface
import com.example.util.CountdownStatus
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Real-time composable countdown timer that calculates the time remaining until a user-defined
 * exam date and updates second-by-second in real-time.
 */
@Composable
fun RealtimeCountdownTimer(
    examDateMillis: Long,
    modifier: Modifier = Modifier,
    startTime: String = "",
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val displayAccent = accentColor.readableOnSurface(isDark)

    val liveCountdown by produceState(
        initialValue = DateUtils.calculateLiveCountdown(examDateMillis, startTime = startTime),
        key1 = examDateMillis,
        key2 = startTime
    ) {
        while (isActive) {
            value = DateUtils.calculateLiveCountdown(examDateMillis, startTime = startTime)
            delay(1000L)
        }
    }

    if (liveCountdown.isPassed) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier
                .fillMaxWidth()
                .testTag("countdown_passed")
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exam Completed",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("realtime_countdown_timer"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (liveCountdown.isToday) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDC2626),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "EXAM TODAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            } else if (liveCountdown.status == CountdownStatus.URGENT) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEA580C).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.45f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "FINAL COUNTDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color(0xFFEA580C),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // 4 Responsive Digital Units (DAYS, HRS, MINS, SECS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownUnitBox(
                    value = liveCountdown.days,
                    label = "DAYS",
                    accentColor = displayAccent,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                CountdownUnitBox(
                    value = liveCountdown.hours,
                    label = "HRS",
                    accentColor = displayAccent,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                CountdownUnitBox(
                    value = liveCountdown.minutes,
                    label = "MINS",
                    accentColor = displayAccent,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                CountdownUnitBox(
                    value = liveCountdown.seconds,
                    label = "SECS",
                    accentColor = displayAccent,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CountdownUnitBox(
    value: Long,
    label: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    slideInVertically { height -> height } togetherWith
                        slideOutVertically { height -> -height }
                },
                label = "countdown_unit_$label"
            ) { targetValue ->
                Text(
                    text = String.format(java.util.Locale.US, "%02d", targetValue),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact real-time ticker string (e.g. "12d 04h 32m 15s") updating every second.
 */
@Composable
fun CompactRealtimeCountdown(
    examDateMillis: Long,
    modifier: Modifier = Modifier,
    startTime: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val liveCountdown by produceState(
        initialValue = DateUtils.calculateLiveCountdown(examDateMillis, startTime = startTime),
        key1 = examDateMillis,
        key2 = startTime
    ) {
        while (isActive) {
            value = DateUtils.calculateLiveCountdown(examDateMillis, startTime = startTime)
            delay(1000L)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag("compact_realtime_countdown")
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = liveCountdown.formattedCompact,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
