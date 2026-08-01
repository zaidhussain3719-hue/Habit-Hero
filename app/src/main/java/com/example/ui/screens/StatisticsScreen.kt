package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FitnessColor
import com.example.ui.theme.HealthColor
import com.example.ui.theme.PersonalColor
import com.example.ui.theme.ReadingColor
import com.example.ui.theme.StudyColor
import com.example.ui.theme.WorkColor
import com.example.ui.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatisticsScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.globalStats.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val logs by viewModel.logsForSelectedDate.collectAsState()

    val today = LocalDate.now()
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    // Mock weekly completion percentages for chart
    val weeklyPercentages = listOf(0.7f, 0.85f, 0.9f, 0.6f, 1.0f, 0.8f, (stats.completionRatePercentage.toFloat() / 100f).coerceIn(0.1f, 1f))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Progress & Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your consistency and streak records",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stats Summary Grid Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Current Streak",
                    value = "${stats.currentStreak} Days",
                    icon = Icons.Default.Whatshot,
                    iconColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Longest Streak",
                    value = "${stats.longestStreak} Days",
                    icon = Icons.Default.Bolt,
                    iconColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Total Check-ins",
                    value = "${stats.totalCheckIns}",
                    icon = Icons.Default.CheckCircle,
                    iconColor = StudyColor,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Success Rate",
                    value = "${stats.completionRatePercentage}%",
                    icon = Icons.Default.MilitaryTech,
                    iconColor = ReadingColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Activity Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Weekly Activity Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Habit check-in rate for this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val barColor = EmeraldPrimary
                    val bgBarColor = MaterialTheme.colorScheme.surfaceVariant

                    // Canvas Bar Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 28.dp.toPx()
                            val cornerRadius = 8.dp.toPx()
                            val spacing = (size.width - (barWidth * 7)) / 8

                            weeklyPercentages.forEachIndexed { index, pct ->
                                val x = spacing + index * (barWidth + spacing)
                                val barHeight = (size.height * pct).coerceAtLeast(10f)
                                val y = size.height - barHeight

                                // Background track
                                drawRoundRect(
                                    color = bgBarColor,
                                    topLeft = Offset(x, 0f),
                                    size = Size(barWidth, size.height),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )

                                // Active progress bar
                                drawRoundRect(
                                    color = if (index == 6) Color(0xFFF59E0B) else barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Calendar Heatmap Grid View
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${today.month.name.lowercase().capitalize()} ${today.year} Monthly Heatmap",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grid of 28-31 days
                    val daysInMonth = today.lengthOfMonth()
                    val todayDayOfMonth = today.dayOfMonth

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (week in 0..4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (dayOfWeek in 1..7) {
                                    val dayNum = week * 7 + dayOfWeek
                                    if (dayNum <= daysInMonth) {
                                        val isCompletedDay = dayNum <= todayDayOfMonth && (dayNum % 2 == 1 || dayNum == todayDayOfMonth)
                                        Surface(
                                            modifier = Modifier.size(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = when {
                                                dayNum == todayDayOfMonth -> Color(0xFFF59E0B)
                                                isCompletedDay -> EmeraldPrimary
                                                dayNum < todayDayOfMonth -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCompletedDay || dayNum == todayDayOfMonth) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(36.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Breakdown Progress Bars
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Category Progress Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    CategoryProgressRow("Health", 0.85f, HealthColor)
                    CategoryProgressRow("Study", 0.70f, StudyColor)
                    CategoryProgressRow("Fitness", 0.90f, FitnessColor)
                    CategoryProgressRow("Reading", 0.60f, ReadingColor)
                    CategoryProgressRow("Work", 0.75f, WorkColor)
                    CategoryProgressRow("Personal", 0.80f, PersonalColor)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryProgressRow(
    category: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}
