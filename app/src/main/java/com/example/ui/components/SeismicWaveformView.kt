package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SeismicReading
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightCyan

@Composable
fun SeismicWaveformView(
    latestReading: SeismicReading,
    thresholdRatio: Float,
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {

    val history = remember { mutableStateListOf<Float>() }
    val maxHistorySize = 120

    LaunchedEffect(latestReading) {
        if (isMonitoring) {
            history.add(latestReading.resultantMagnitude)
            if (history.size > maxHistorySize) {
                history.removeAt(0)
            }
        }
    }

    val animatedPga by animateFloatAsState(
        targetValue = latestReading.resultantMagnitude,
        label = "pgaAnimation"
    )

    val isAlert = latestReading.staLtaRatio >= thresholdRatio

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF07111E), RoundedCornerShape(12.dp))
            .border(1.dp, if (isAlert) AlertRed else Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SISMÓGRAFO EN VIVO",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrightCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = if (isMonitoring) "Muestreo acelerométrico continuo" else "Sensor en pausa",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.LightGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PGA: ${"%.3f".format(animatedPga)} m/s²",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (animatedPga > 0.4f) AlertRed else Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "STA/LTA: ${"%.1f".format(latestReading.staLtaRatio)} (Umbral: $thresholdRatio)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = if (isAlert) AlertRed else AlertAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFF040A12), RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                val gridStep = 20.dp.toPx()
                var gx = 0f
                while (gx < w) {
                    drawLine(
                        color = Color(0xFF0D223A),
                        start = Offset(gx, 0f),
                        end = Offset(gx, h),
                        strokeWidth = 1f
                    )
                    gx += gridStep
                }
                var gy = 0f
                while (gy < h) {
                    drawLine(
                        color = Color(0xFF0D223A),
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 1f
                    )
                    gy += gridStep
                }

                drawLine(
                    color = Color(0xFF1B4965),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.5f
                )

                if (history.isNotEmpty()) {
                    val stepX = w / (maxHistorySize - 1).coerceAtLeast(1)
                    val path = Path()

                    for (i in history.indices) {
                        val mag = history[i]

                        val dy = (mag * 60f).coerceIn(-midY * 0.9f, midY * 0.9f)
                        val x = i * stepX
                        val y = midY - dy

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = if (isAlert) AlertRed else BrightCyan,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "X: ${"%.2f".format(latestReading.x)}  Y: ${"%.2f".format(latestReading.y)}  Z: ${"%.2f".format(latestReading.z)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Filtro Pasa-Altas (α=0.82)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = BrightCyan.copy(alpha = 0.7f)
            )
        }
    }
}

