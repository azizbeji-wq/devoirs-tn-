package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.progress.GradePoint

/**
 * Courbe d'évolution des notes sur 20 dessinée nativement en Jetpack Compose Canvas
 * (sans dépendance graphique externe), adaptée aux thèmes clair et sombre.
 */
@Composable
fun GradeEvolutionCard(
    subjects: List<String>,
    selectedSubject: String?,
    points: List<GradePoint>,
    onSelectSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = selectedSubject?.let { subjectAccentColor(it) }
        ?: MaterialTheme.colorScheme.primary
    val emerald = adaptiveEmerald()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("grade_evolution_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ShowChart,
                    contentDescription = stringResource(R.string.cd_grade_chart_icon),
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.grade_evolution_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (subjects.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects) { subject ->
                        val isSelected = subject == selectedSubject
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectSubject(subject) },
                            modifier = Modifier.testTag("chart_subject_chip_$subject"),
                            label = { Text(localizedSubjectName(subject)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.grade_evolution_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Légende claire de la courbe et du seuil de moyenne 10/20
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendItem(
                        color = accentColor,
                        dashed = false,
                        label = stringResource(R.string.grade_legend_score)
                    )
                    ChartLegendItem(
                        color = emerald,
                        dashed = true,
                        label = stringResource(R.string.grade_legend_pass_line)
                    )
                }

                GradeEvolutionCanvas(
                    points = points,
                    lineColor = accentColor,
                    passLineBaseColor = emerald,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .testTag("grade_evolution_canvas")
                )

                val latest = points.last()
                val localizedSelected = selectedSubject?.let { localizedSubjectName(it) }.orEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.grade_evolution_attempts,
                            points.size,
                            localizedSelected
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.grade_evolution_latest,
                            formatGrade(latest.scoreOn20)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (latest.scoreOn20 >= 10.0) emerald else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    dashed: Boolean,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(width = 18.dp, height = 6.dp)) {
            val y = size.height / 2f
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Zone Canvas traçant la grille 0..20, la ligne de moyenne 10/20 en pointillés
 * et la courbe lissée des notes obtenues.
 */
@Composable
private fun GradeEvolutionCanvas(
    points: List<GradePoint>,
    lineColor: Color,
    passLineBaseColor: Color,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val passLineColor = passLineBaseColor.copy(alpha = 0.60f)
    val textMeasurer = rememberTextMeasurer()

    val axisStyle = TextStyle(
        color = labelColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
    val pointStyle = TextStyle(
        color = lineColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )

    Canvas(modifier = modifier) {
        val leftPad = 34.dp.toPx()
        val rightPad = 18.dp.toPx()
        val topPad = 18.dp.toPx()
        val bottomPad = 28.dp.toPx()

        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        // Lignes horizontales de repère : 0, 5, 10 (moyenne), 15, 20
        val gridValues = listOf(0, 5, 10, 15, 20)
        gridValues.forEach { value ->
            val y = topPad + chartHeight * (1f - value / 20f)
            val isPassLine = value == 10
            drawLine(
                color = if (isPassLine) passLineColor else gridColor.copy(alpha = 0.65f),
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartWidth, y),
                strokeWidth = if (isPassLine) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (isPassLine) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
            )

            val measured = textMeasurer.measure(value.toString(), style = axisStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                    y = y - measured.size.height / 2f
                )
            )
        }

        val coords = points.mapIndexed { index, pt ->
            val x = if (points.size == 1) {
                leftPad + chartWidth / 2f
            } else {
                leftPad + chartWidth * (index.toFloat() / (points.size - 1).toFloat())
            }
            val clamped = pt.scoreOn20.coerceIn(0.0, 20.0).toFloat()
            val y = topPad + chartHeight * (1f - clamped / 20f)
            Offset(x, y)
        }

        if (coords.size >= 2) {
            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }

            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, topPad + chartHeight)
                lineTo(coords.first().x, topPad + chartHeight)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.24f),
                        lineColor.copy(alpha = 0.02f)
                    ),
                    startY = topPad,
                    endY = topPad + chartHeight
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Points de mesure et étiquettes lisibles
        coords.forEachIndexed { idx, offset ->
            drawCircle(
                color = surfaceColor,
                radius = 5.5.dp.toPx(),
                center = offset
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = offset
            )

            val scoreLabel = formatGrade(points[idx].scoreOn20)
            val scoreLayout = textMeasurer.measure(scoreLabel, style = pointStyle)
            drawText(
                textLayoutResult = scoreLayout,
                topLeft = Offset(
                    x = (offset.x - scoreLayout.size.width / 2f)
                        .coerceIn(leftPad, size.width - scoreLayout.size.width - 2.dp.toPx()),
                    y = (offset.y - scoreLayout.size.height - 6.dp.toPx()).coerceAtLeast(0f)
                )
            )

            val shouldDrawDate = points.size <= 6 || idx == 0 || idx == points.lastIndex || idx % 2 == 0
            if (shouldDrawDate) {
                val dateLayout = textMeasurer.measure(points[idx].shortDateLabel, style = axisStyle)
                drawText(
                    textLayoutResult = dateLayout,
                    topLeft = Offset(
                        x = (offset.x - dateLayout.size.width / 2f)
                            .coerceIn(leftPad, size.width - dateLayout.size.width),
                        y = topPad + chartHeight + 8.dp.toPx()
                    )
                )
            }
        }
    }
}

private fun formatGrade(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
