package com.example.gymtracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracking.model.WorkoutSession
import com.example.gymtracking.ui.theme.TrophyGold
import com.example.gymtracking.ui.theme.getCategoryColor
import com.example.gymtracking.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun StatsScreen(viewModel: WorkoutViewModel) {
    val sessions = viewModel.sessions.toList()
    
    if (sessions.isEmpty()) {
        EmptyStateView(Icons.Default.Info, "No data to analyze.\nKeep lifting to see your progress graphs!")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { HeroInsightsBanner(sessions, viewModel) }
        item { MuscleGroupRadarChart(sessions) }
        item { TrainingHeatmap(sessions) }
        item { SmartCoachInsights(sessions) }
        item { MonthVsMonthCard(sessions) }
        item { ProgressiveOverloadTable(sessions, viewModel) }
        item { NeglectedMuscleGroups(sessions) }
        item { PersonalRecordsWall(sessions, viewModel) }
        item { MostTrainedChart(sessions) }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════
// HERO STATS BANNER
// ═══════════════════════════════════════════════════════════════

@Composable
fun HeroInsightsBanner(sessions: List<WorkoutSession>, viewModel: WorkoutViewModel) {
    val totalSessions = sessions.size
    
    val weeksActive = sessions.map { 
        it.date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) 
    }.distinct().sortedDescending()
    
    var streak = 0
    var currentWeek = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    
    for (week in weeksActive) {
        if (week == currentWeek || week == currentWeek.minusWeeks(1) && streak == 0) {
            streak++
            currentWeek = week.minusWeeks(1)
        } else {
            break
        }
    }

    val oneMonthAgo = LocalDate.now().minusDays(30)
    var prsThisMonth = 0
    val bestWeights = mutableMapOf<String, Float>()
    
    sessions.sortedBy { it.date }.forEach { session ->
        session.exercises.forEach { ex ->
            val maxWeight = ex.sets.maxOfOrNull { it.weight } ?: 0f
            val currentBest = bestWeights[ex.name] ?: 0f
            if (maxWeight > currentBest) {
                bestWeights[ex.name] = maxWeight
                if (session.date.isAfter(oneMonthAgo)) {
                    prsThisMonth++
                }
            }
        }
    }

    val exercisesTracked = sessions.flatMap { it.exercises.map { e -> e.name } }.distinct().size

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroStatBox("Total Sessions", totalSessions.toString(), Modifier.weight(1f))
            HeroStatBox("Current Streak", "$streak wks", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroStatBox("PRs (30d)", prsThisMonth.toString(), Modifier.weight(1f))
            HeroStatBox("Exercises", exercisesTracked.toString(), Modifier.weight(1f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MUSCLE GROUP RADAR CHART
// ═══════════════════════════════════════════════════════════════

@Composable
fun MuscleGroupRadarChart(sessions: List<WorkoutSession>) {
    val categoryTotalCounts = mutableMapOf<String, Int>()
    sessions.forEach { session ->
        session.exercises.forEach { ex ->
            categoryTotalCounts[ex.category] = (categoryTotalCounts[ex.category] ?: 0) + 1
        }
    }
    
    if (categoryTotalCounts.size < 3) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Muscle Group Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Log exercises across at least 3 muscle groups to generate your balance radar chart.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    
    val topCategories = categoryTotalCounts.entries.sortedByDescending { it.value }.take(6)
    val maxCount = topCategories.maxOfOrNull { it.value }?.toFloat() ?: 1f
    
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Muscle Group Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Exercises logged per muscle group", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val radius = minOf(size.width, size.height) / 2f * 0.7f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val N = topCategories.size
                    
                    for (step in 1..4) {
                        val stepRadius = radius * (step / 4f)
                        val path = Path()
                        for (i in 0 until N) {
                            val angle = (i * 2 * Math.PI / N) - (Math.PI / 2)
                            val x = center.x + (stepRadius * cos(angle)).toFloat()
                            val y = center.y + (stepRadius * sin(angle)).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = Color.Gray.copy(alpha = 0.2f), style = Stroke(width = 2f))
                    }
                    
                    for (i in 0 until N) {
                        val angle = (i * 2 * Math.PI / N) - (Math.PI / 2)
                        val x = center.x + (radius * cos(angle)).toFloat()
                        val y = center.y + (radius * sin(angle)).toFloat()
                        drawLine(color = Color.Gray.copy(alpha = 0.2f), start = center, end = Offset(x, y), strokeWidth = 2f)
                    }
                    
                    val dataPath = Path()
                    val points = mutableListOf<Offset>()
                    for (i in 0 until N) {
                        val count = topCategories[i].value
                        val normalized = count / maxCount
                        val r = radius * normalized
                        val angle = (i * 2 * Math.PI / N) - (Math.PI / 2)
                        val x = center.x + (r * cos(angle)).toFloat()
                        val y = center.y + (r * sin(angle)).toFloat()
                        val offset = Offset(x, y)
                        points.add(offset)
                        if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                    }
                    dataPath.close()
                    
                    drawPath(
                        dataPath, 
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.6f), Color(0xFF4F46E5).copy(alpha = 0.2f)),
                            center = center,
                            radius = radius
                        )
                    )
                    drawPath(
                        dataPath, 
                        color = Color(0xFF4F46E5), 
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )
                    
                    for (p in points) {
                        drawCircle(color = Color.White, radius = 8f, center = p)
                        drawCircle(color = Color(0xFF06B6D4), radius = 6f, center = p)
                    }
                    
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = onSurfaceColor
                            textSize = 34f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        
                        for (i in 0 until N) {
                            val angle = (i * 2 * Math.PI / N) - (Math.PI / 2)
                            val textRadius = radius * 1.3f
                            val x = center.x + (textRadius * cos(angle)).toFloat()
                            val y = center.y + (textRadius * sin(angle)).toFloat()
                            
                            val textY = y + (paint.textSize / 3f)
                            canvas.nativeCanvas.drawText(topCategories[i].key, x, textY, paint)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TRAINING HEATMAP
// ═══════════════════════════════════════════════════════════════

@Composable
fun TrainingHeatmap(sessions: List<WorkoutSession>) {
    val weeks = 12
    val daysInWeek = 7
    val today = LocalDate.now()
    val startDate = today.minusWeeks(weeks.toLong() - 1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    
    val activityMap = remember(sessions) {
        sessions.associate { it.date to it.exercises.size }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Activity (Last 12 Weeks)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (w in 0 until weeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 0 until daysInWeek) {
                            val cellDate = startDate.plusDays((w * daysInWeek + d).toLong())
                            if (cellDate.isAfter(today)) break
                            
                            val exercises = activityMap[cellDate] ?: 0
                            val color = when {
                                exercises == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                exercises < 3 -> Color(0xFF10B981).copy(alpha = 0.4f)
                                exercises < 6 -> Color(0xFF10B981).copy(alpha = 0.7f)
                                else -> Color(0xFF10B981)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SMART COACH INSIGHTS
// ═══════════════════════════════════════════════════════════════

@Composable
fun SmartCoachInsights(sessions: List<WorkoutSession>) {
    val insights = remember(sessions) {
        val result = mutableListOf<Pair<String, Color>>() // emoji+text, accent color
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val sixtyDaysAgo = today.minusDays(60)
        
        // 1. Progressive overload detection per exercise (30d trend)
        val exerciseNames = sessions.flatMap { it.exercises.map { e -> e.name } }.distinct()
        val recentSessions = sessions.filter { !it.date.isBefore(thirtyDaysAgo) }
        val olderSessions = sessions.filter { it.date.isBefore(thirtyDaysAgo) && !it.date.isBefore(sixtyDaysAgo) }
        
        exerciseNames.forEach { name ->
            val recentMax = recentSessions
                .flatMap { s -> s.exercises.filter { it.name == name } }
                .flatMap { it.sets }
                .maxOfOrNull { it.weight } ?: 0f
            val olderMax = olderSessions
                .flatMap { s -> s.exercises.filter { it.name == name } }
                .flatMap { it.sets }
                .maxOfOrNull { it.weight } ?: 0f
            
            if (olderMax > 0f && recentMax > olderMax) {
                val pct = ((recentMax - olderMax) / olderMax * 100).roundToInt()
                if (pct >= 5) {
                    result.add("🔥 $name up $pct% in the last 30 days" to Color(0xFF10B981))
                }
            }
            if (olderMax > 0f && recentMax < olderMax && recentMax > 0f) {
                val pct = ((olderMax - recentMax) / olderMax * 100).roundToInt()
                if (pct >= 10) {
                    result.add("📉 $name dropped $pct% — consider revisiting" to Color(0xFFEF4444))
                }
            }
        }
        
        // 2. Consistency insight — this month vs last month sessions
        val thisMonthSessions = sessions.count { it.date.month == today.month && it.date.year == today.year }
        val lastMonth = today.minusMonths(1)
        val lastMonthSessions = sessions.count { it.date.month == lastMonth.month && it.date.year == lastMonth.year }
        
        if (thisMonthSessions > lastMonthSessions && lastMonthSessions > 0) {
            val diff = thisMonthSessions - lastMonthSessions
            result.add("📈 $diff more sessions this month vs last month — keep it up!" to Color(0xFF10B981))
        } else if (thisMonthSessions < lastMonthSessions && lastMonthSessions > 0) {
            val diff = lastMonthSessions - thisMonthSessions
            result.add("⚠️ $diff fewer sessions this month — stay consistent!" to Color(0xFFF59E0B))
        }
        
        // 3. Average rest days
        val sortedDates = sessions.map { it.date }.distinct().sorted()
        if (sortedDates.size >= 2) {
            val gaps = sortedDates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            val avgGap = gaps.average()
            if (avgGap <= 2.0) {
                result.add("💪 You average ${String.format("%.1f", avgGap)} rest days between sessions — solid consistency" to Color(0xFF8B5CF6))
            } else if (avgGap >= 4.0) {
                result.add("💤 Averaging ${String.format("%.1f", avgGap)} days between sessions — try to tighten up" to Color(0xFFF59E0B))
            }
        }
        
        // 4. PR streak
        val bestWeights = mutableMapOf<String, Float>()
        var weekPRs = 0
        val weekAgo = today.minusDays(7)
        sessions.sortedBy { it.date }.forEach { session ->
            session.exercises.forEach { ex ->
                val maxW = ex.sets.maxOfOrNull { it.weight } ?: 0f
                val prev = bestWeights[ex.name] ?: 0f
                if (maxW > prev) {
                    bestWeights[ex.name] = maxW
                    if (!session.date.isBefore(weekAgo)) weekPRs++
                }
            }
        }
        if (weekPRs > 0) {
            result.add("🏆 $weekPRs new PR${if (weekPRs > 1) "s" else ""} this week!" to TrophyGold)
        }
        
        // 5. Total exercises this week
        val thisWeekExercises = sessions
            .filter { !it.date.isBefore(weekAgo) }
            .sumOf { it.exercises.size }
        if (thisWeekExercises > 0) {
            result.add("🎯 $thisWeekExercises exercises logged this week" to Color(0xFF06B6D4))
        }
        
        result.take(5) // cap at 5 most relevant
    }
    
    if (insights.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Coach Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Auto-generated from your data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            
            insights.forEachIndexed { index, (text, accentColor) ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = accentColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MONTH VS MONTH COMPARISON
// ═══════════════════════════════════════════════════════════════

@Composable
fun MonthVsMonthCard(sessions: List<WorkoutSession>) {
    val today = LocalDate.now()
    val thisMonth = today.month
    val thisYear = today.year
    val lastMonthDate = today.minusMonths(1)
    val lastMonth = lastMonthDate.month
    val lastYear = lastMonthDate.year
    
    val thisMonthSessions = sessions.filter { it.date.month == thisMonth && it.date.year == thisYear }
    val lastMonthSessions = sessions.filter { it.date.month == lastMonth && it.date.year == lastYear }
    
    val thisSessionCount = thisMonthSessions.size
    val lastSessionCount = lastMonthSessions.size
    
    val thisGroups = thisMonthSessions.flatMap { it.exercises.map { e -> e.category } }.distinct().size
    val lastGroups = lastMonthSessions.flatMap { it.exercises.map { e -> e.category } }.distinct().size
    
    val thisExerciseCount = thisMonthSessions.sumOf { it.exercises.size }
    val lastExerciseCount = lastMonthSessions.sumOf { it.exercises.size }
    
    // PRs
    val bestWeightsLast = mutableMapOf<String, Float>()
    val bestWeightsThis = mutableMapOf<String, Float>()
    var lastPRs = 0
    var thisPRs = 0
    val allBest = mutableMapOf<String, Float>()
    
    sessions.sortedBy { it.date }.forEach { session ->
        session.exercises.forEach { ex ->
            val maxW = ex.sets.maxOfOrNull { it.weight } ?: 0f
            val prev = allBest[ex.name] ?: 0f
            if (maxW > prev) {
                allBest[ex.name] = maxW
                if (session.date.month == lastMonth && session.date.year == lastYear) lastPRs++
                if (session.date.month == thisMonth && session.date.year == thisYear) thisPRs++
            }
        }
    }
    
    val thisMonthLabel = thisMonth.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val lastMonthLabel = lastMonth.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Month vs Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("$lastMonthLabel → $thisMonthLabel comparison", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            
            // Header row
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Metric", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.4f))
                Text(lastMonthLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(thisMonthLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("Δ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            ComparisonRow("Sessions", lastSessionCount, thisSessionCount)
            ComparisonRow("Exercises", lastExerciseCount, thisExerciseCount)
            ComparisonRow("Muscle Groups", lastGroups, thisGroups)
            ComparisonRow("PRs Set", lastPRs, thisPRs)
        }
    }
}

@Composable
fun ComparisonRow(label: String, lastVal: Int, thisVal: Int) {
    val delta = thisVal - lastVal
    val deltaText = when {
        delta > 0 -> "+$delta ↑"
        delta < 0 -> "$delta ↓"
        else -> "—"
    }
    val deltaColor = when {
        delta > 0 -> Color(0xFF10B981)
        delta < 0 -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.4f))
        Text(lastVal.toString(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(thisVal.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(deltaText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = deltaColor, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRESSIVE OVERLOAD TABLE
// ═══════════════════════════════════════════════════════════════

@Composable
fun ProgressiveOverloadTable(sessions: List<WorkoutSession>, viewModel: WorkoutViewModel) {
    data class OverloadRow(
        val name: String,
        val category: String,
        val currentMetric: Float,
        val bestMetric: Float,
        val est1RMOrReps: Float,
        val trendPct: Int,
        val trendSymbol: String,
        val isBodyweight: Boolean
    )
    
    val rows = remember(sessions) {
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val sixtyDaysAgo = today.minusDays(60)
        val exerciseNames = sessions.flatMap { it.exercises.map { e -> e.name } }.distinct()
        
        exerciseNames.mapNotNull { name ->
            val allSessions = sessions.filter { s -> s.exercises.any { it.name == name } }.sortedBy { it.date }
            if (allSessions.isEmpty()) return@mapNotNull null
            
            val category = allSessions.last().exercises.first { it.name == name }.category
            if (category.equals("Cardio", ignoreCase = true)) return@mapNotNull null
            
            val maxRecordedWeight = allSessions.flatMap { s -> s.exercises.filter { it.name == name } }
                .flatMap { it.sets }.maxOfOrNull { it.weight } ?: 0f
            
            val isBodyweight = maxRecordedWeight <= 0f
            
            val currentMetric = if (isBodyweight) {
                allSessions.last().exercises.first { it.name == name }.sets.maxOfOrNull { it.reps.toFloat() } ?: 0f
            } else {
                allSessions.last().exercises.first { it.name == name }.sets.maxOfOrNull { it.weight } ?: 0f
            }
            
            val bestMetric = if (isBodyweight) {
                allSessions.flatMap { s -> s.exercises.filter { it.name == name } }
                    .flatMap { it.sets }.maxOfOrNull { it.reps.toFloat() } ?: 0f
            } else {
                maxRecordedWeight
            }
            
            val est1RMOrReps = if (isBodyweight) {
                bestMetric
            } else {
                allSessions.flatMap { s -> s.exercises.filter { it.name == name } }
                    .flatMap { it.sets }
                    .filter { it.reps in 1..12 && it.weight > 0f }
                    .maxOfOrNull { it.weight * (1f + (it.reps / 30f)) } ?: bestMetric
            }
            
            if (bestMetric <= 0f) return@mapNotNull null
            
            // 30d trend: compare recent max vs older max
            val recentMax = sessions
                .filter { !it.date.isBefore(thirtyDaysAgo) }
                .flatMap { s -> s.exercises.filter { it.name == name } }
                .flatMap { it.sets }
                .maxOfOrNull { if (isBodyweight) it.reps.toFloat() else it.weight } ?: 0f
                
            val olderMax = sessions
                .filter { it.date.isBefore(thirtyDaysAgo) && !it.date.isBefore(sixtyDaysAgo) }
                .flatMap { s -> s.exercises.filter { it.name == name } }
                .flatMap { it.sets }
                .maxOfOrNull { if (isBodyweight) it.reps.toFloat() else it.weight } ?: 0f
            
            val (trendPct, trendSymbol) = when {
                olderMax <= 0f -> Pair(0, "🆕")
                recentMax > olderMax -> {
                    val pct = ((recentMax - olderMax) / olderMax * 100).roundToInt()
                    Pair(pct, "↑ +$pct%")
                }
                recentMax < olderMax -> {
                    val pct = ((olderMax - recentMax) / olderMax * 100).roundToInt()
                    Pair(-pct, "↓ -$pct%")
                }
                else -> Pair(0, "→ Plateau")
            }
            
            OverloadRow(name, category, currentMetric, bestMetric, est1RMOrReps, trendPct, trendSymbol, isBodyweight)
        }.sortedByDescending { kotlin.math.abs(it.trendPct) }
    }
    
    if (rows.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Progressive Overload", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Log workouts across multiple sessions to track your 30-day progressive overload trends.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Progressive Overload", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("30-day weight trend per exercise", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Exercise", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.6f))
                Text("Current", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("Est. 1RM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("30d", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1.1f))
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            rows.forEach { row ->
                val trendColor = when {
                    row.trendPct > 0 -> Color(0xFF10B981)
                    row.trendPct < 0 -> Color(0xFFEF4444)
                    row.trendSymbol == "🆕" -> Color(0xFF8B5CF6)
                    else -> Color(0xFFF59E0B)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        row.name, 
                        style = MaterialTheme.typography.bodySmall, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.weight(1.6f),
                        maxLines = 1
                    )
                    Text(
                        "${if (row.currentMetric % 1f == 0f) row.currentMetric.toInt() else row.currentMetric}${if (row.isBodyweight) "r" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (row.isBodyweight) "${row.est1RMOrReps.toInt()} reps" else "${if (row.est1RMOrReps % 1f == 0f) row.est1RMOrReps.toInt() else String.format(java.util.Locale.US, "%.1f", row.est1RMOrReps)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrophyGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = trendColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text(
                            row.trendSymbol,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = trendColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// NEGLECTED MUSCLE GROUPS
// ═══════════════════════════════════════════════════════════════

@Composable
fun NeglectedMuscleGroups(sessions: List<WorkoutSession>) {
    val neglected = remember(sessions) {
        val today = LocalDate.now()
        val allCategories = sessions.flatMap { it.exercises.map { e -> e.category } }.distinct()
        
        allCategories.mapNotNull { category ->
            val lastTrained = sessions
                .filter { s -> s.exercises.any { it.category == category } }
                .maxByOrNull { it.date }
                ?.date
            
            if (lastTrained != null) {
                val daysAgo = ChronoUnit.DAYS.between(lastTrained, today).toInt()
                if (daysAgo >= 7) {
                    Triple(category, daysAgo, lastTrained)
                } else null
            } else null
        }.sortedByDescending { it.second }
    }
    
    if (neglected.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Neglected Muscle Groups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
            Spacer(Modifier.height(4.dp))
            Text("Not trained in 7+ days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            
            neglected.forEach { (category, daysAgo, lastDate) ->
                val urgency = when {
                    daysAgo >= 14 -> "🔴"
                    daysAgo >= 10 -> "🟠"
                    else -> "🟡"
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(urgency, fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Last trained ${lastDate.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "$daysAgo days ago",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PERSONAL RECORDS WALL
// ═══════════════════════════════════════════════════════════════

@Composable
fun PersonalRecordsWall(sessions: List<WorkoutSession>, viewModel: WorkoutViewModel) {
    val prs = remember(sessions) {
        val bests = mutableMapOf<String, Pair<Float, LocalDate>>()
        val categories = mutableMapOf<String, String>()
        
        sessions.sortedBy { it.date }.forEach { session ->
            session.exercises.forEach { ex ->
                if (!ex.category.equals("Cardio", ignoreCase = true)) {
                    val maxWeight = ex.sets.maxOfOrNull { it.weight } ?: 0f
                    if (maxWeight > 0f) {
                        val currentBest = bests[ex.name]?.first ?: 0f
                        if (maxWeight >= currentBest) {
                            bests[ex.name] = Pair(maxWeight, session.date)
                            categories[ex.name] = ex.category
                        }
                    }
                }
            }
        }
        bests.map { Triple(it.key, it.value, categories[it.key] ?: "") }
            .sortedByDescending { it.second.second }
            .take(10)
    }

    if (prs.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Personal Records Wall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Log weighted exercises to unlock and showcase your all-time PRs here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Personal Records Wall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            
            prs.forEachIndexed { index, (name, pbData, category) ->
                val (weight, date) = pbData
                val isRecent = date.isAfter(LocalDate.now().minusDays(14))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (isRecent) {
                                Spacer(Modifier.width(8.dp))
                                Text("🔥 Recent", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B))
                            }
                        }
                    }
                    Surface(
                        color = TrophyGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${if (weight % 1f == 0f) weight.toInt() else weight} kg",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TrophyGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                if (index < prs.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MOST TRAINED EXERCISES
// ═══════════════════════════════════════════════════════════════

@Composable
fun MostTrainedChart(sessions: List<WorkoutSession>) {
    val topExercises = remember(sessions) {
        val counts = mutableMapOf<String, Int>()
        val categories = mutableMapOf<String, String>()
        
        sessions.forEach { session ->
            session.exercises.forEach { ex ->
                counts[ex.name] = (counts[ex.name] ?: 0) + 1
                categories[ex.name] = ex.category
            }
        }
        counts.entries.sortedByDescending { it.value }.take(5).map { Triple(it.key, it.value, categories[it.key] ?: "") }
    }

    if (topExercises.isEmpty()) return
    
    val maxCount = topExercises.maxOfOrNull { it.second } ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Most Trained Exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            
            topExercises.forEach { (name, count, category) ->
                val fraction = count.toFloat() / maxCount.toFloat()
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$count sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(getCategoryColor(category))
                        )
                    }
                }
            }
        }
    }
}
