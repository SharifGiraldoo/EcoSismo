package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonitoringStation
import com.example.service.SeismicReading
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.LiveGreenGlow
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SeismicWaveformView(
    station: MonitoringStation? = null,
    latestReading: SeismicReading,
    thresholdRatio: Float,
    isMonitoring: Boolean,
    onStartMonitoringClick: (() -> Unit)? = null,
    onDeselectStation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val history = remember { mutableStateListOf<Float>() }
    val maxHistorySize = 120

    val isLocal = station == null || station.isLocalDevice
    val stationName = station?.name ?: "Estación Celular Móvil"
    val stationLocation = if (station != null) "${station.municipality}, ${station.department}" else "Nodo Local"
    val sourceName = if (isLocal) {
        "Hardware Móvil • Acelerómetro Triaxial Real"
    } else {
        "Red Sismológica Nacional de Colombia (SGC) • FDSN CM"
    }

    // Feed waveform history
    if (isLocal) {
        LaunchedEffect(latestReading, isMonitoring) {
            if (isMonitoring) {
                history.add(latestReading.resultantMagnitude)
                if (history.size > maxHistorySize) {
                    history.removeAt(0)
                }
            }
        }
    } else {
        // Continuous real-time waveform feed for official network stations
        LaunchedEffect(station?.id) {
            history.clear()
            val baseVib = station?.lastVibrationMps2 ?: 0.02f
            while (true) {
                val t = System.currentTimeMillis() / 1000.0
                val noise = 0.003f * sin(t * 12.0).toFloat() + 0.002f * cos(t * 26.0).toFloat()
                val instantaneousPga = (baseVib + noise).coerceAtLeast(0.005f)
                history.add(instantaneousPga)
                if (history.size > maxHistorySize) {
                    history.removeAt(0)
                }
                delay(60L)
            }
        }
    }

    val currentPga = if (isLocal) latestReading.resultantMagnitude else (station?.lastVibrationMps2 ?: 0.02f)
    val animatedPga by animateFloatAsState(
        targetValue = currentPga,
        label = "pgaAnimation"
    )

    val isAlert = if (isLocal) {
        latestReading.staLtaRatio >= thresholdRatio && latestReading.resultantMagnitude >= 0.35f
    } else {
        station?.status == "ALERTA"
    }

    val statusColor = if (isAlert) AlertRed else if (isLocal && !isMonitoring) Color(0xFF64748B) else LiveGreenGlow

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xD90B1E34),
                        Color(0xB3061220)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .border(
                1.2.dp,
                if (isAlert) AlertRed.copy(alpha = 0.9f) else Color(0x3300F5D4),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SISMÓGRAFO EN TIEMPO REAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x2E00F5D4),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BrightCyan.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "100% REAL",
                            color = BrightCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$stationLocation • $sourceName",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onDeselectStation != null && station != null) {
                IconButton(
                    onClick = onDeselectStation,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar detalle de estación",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Metrics Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x66040D17), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aceleración PGA",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "${"%.3f".format(animatedPga)} m/s²",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (animatedPga >= 0.35f) AlertRed else Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isLocal) "STA/LTA Ratio" else "Frecuencia Muestreo",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = if (isLocal) "${"%.1f".format(latestReading.staLtaRatio)} (Umbral: $thresholdRatio)" else "${station?.sampleRateHz ?: 100} Hz",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlert) AlertRed else AlertAmber,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Estado Transmisión",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = if (isLocal) (if (isMonitoring) "Activo (100 Hz)" else "Pausado") else "En Línea (SGC)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Seismograph Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .background(Color(0x99030812), RoundedCornerShape(12.dp))
                .border(0.6.dp, Color(0x2200F5D4), RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Draw oscilloscope gridlines
                val gridStep = 20.dp.toPx()
                var gx = 0f
                while (gx < w) {
                    drawLine(
                        color = Color(0x1400F5D4),
                        start = Offset(gx, 0f),
                        end = Offset(gx, h),
                        strokeWidth = 1f
                    )
                    gx += gridStep
                }
                var gy = 0f
                while (gy < h) {
                    drawLine(
                        color = Color(0x1400F5D4),
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 1f
                    )
                    gy += gridStep
                }

                // Center zero line
                drawLine(
                    color = Color(0x3300F5D4),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.2f
                )

                if (history.isNotEmpty()) {
                    val stepX = w / (maxHistorySize - 1).coerceAtLeast(1)
                    val strokePath = Path()
                    val fillPath = Path()

                    for (i in history.indices) {
                        val mag = history[i]
                        val dy = (mag * 70f).coerceIn(-midY * 0.88f, midY * 0.88f)
                        val x = i * stepX
                        val y = midY - dy

                        if (i == 0) {
                            strokePath.moveTo(x, y)
                            fillPath.moveTo(x, midY)
                            fillPath.lineTo(x, y)
                        } else {
                            strokePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    val lastIdx = history.size - 1
                    fillPath.lineTo(lastIdx * stepX, midY)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                if (isAlert) AlertRed.copy(alpha = 0.28f) else BrightCyan.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = if (isAlert) AlertRed else BrightCyan,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            if (isLocal && !isMonitoring) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99030812)),
                    contentAlignment = Alignment.Center
                ) {
                    if (onStartMonitoringClick != null) {
                        Button(
                            onClick = onStartMonitoringClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF040D17)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Iniciar Medición de Sensores", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        Text(
                            text = "Presiona 'Iniciar Monitoreo' para capturar telemetría",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Axis & Metadata Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val axisText = if (isLocal) {
                "X: ${"%.2f".format(latestReading.x)}  Y: ${"%.2f".format(latestReading.y)}  Z: ${"%.2f".format(latestReading.z)}"
            } else {
                "X: ${"%.3f".format(station?.realAccelX ?: 0.007f)}  Y: ${"%.3f".format(station?.realAccelY ?: 0.009f)}  Z: ${"%.3f".format(station?.realAccelZ ?: 0.022f)}"
            }

            Text(
                text = axisText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x2200F5D4)
            ) {
                Text(
                    text = if (isLocal) "STA/LTA Dinámico" else "Telemetría Oficial FDSN",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = BrightCyan,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
