package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonitoringStation
import com.example.data.repository.HardwareSensorDiagnostics
import com.example.data.repository.StationRepository
import com.example.service.SeismicMonitoringService
import com.example.ui.components.ColombiaMapView
import com.example.ui.components.MapDisplayMode
import com.example.ui.components.SeismicWaveformView
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightCyan
import kotlin.math.sin
import kotlin.math.cos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringStationsScreen(
    repository: StationRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val stations by repository.allStations.collectAsState(initial = emptyList())
    val alerts by repository.allAlerts.collectAsState(initial = emptyList())
    var selectedStation by remember { mutableStateOf<MonitoringStation?>(null) }
    var isMapExpanded by remember { mutableStateOf(false) }

    val isMonitoring by SeismicMonitoringService.isMonitoring.collectAsState()
    val latestReading by SeismicMonitoringService.latestReading.collectAsState()
    val liveTelemetry by (repository.telemetryManager?.telemetry
        ?: remember { com.example.data.telemetry.DeviceTelemetryManager(context).telemetry })
        .collectAsState()

    var showRegisterDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var staLtaSensitivity by remember { mutableFloatStateOf(3.2f) }

    val hardwareDiagnostics = remember { repository.performHardwareDiagnostics() }

    LaunchedEffect(staLtaSensitivity) {
        SeismicMonitoringService.configuredStaLtaThreshold = staLtaSensitivity
    }

    LaunchedEffect(Unit) {
        if (stations.none { it.id.startsWith("sta_sgc_") }) {
            repository.refreshAllStationsTelemetry()
        }
    }

    LaunchedEffect(Unit) {
        SeismicMonitoringService.alertEvents.collect { alert ->
            val alertSt = repository.allStations.first().find { it.status == "ALERTA" || it.id == alert.stationId }
            if (alertSt != null) {
                selectedStation = alertSt
            }
            snackbarHostState.showSnackbar("¡ANOMALÍA DETECTADA! ${alert.stationName}: ${alert.peakAccelerationMps2} m/s²")
        }
    }

    LaunchedEffect(stations) {
        val alertSt = stations.find { it.status == "ALERTA" }
        if (alertSt != null && (selectedStation == null || selectedStation?.status != "ALERTA")) {
            selectedStation = alertSt
        } else if (selectedStation == null && stations.isNotEmpty()) {
            selectedStation = stations.find { it.isLocalDevice } ?: stations.firstOrNull()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                SeismicMonitoringService.start(context)
            }
        }
    )

    val mapBoxContent: @Composable (Modifier) -> Unit = { boxModifier ->
        Box(modifier = boxModifier) {
            ColombiaMapView(
                mode = MapDisplayMode.MONITORING_STATIONS,
                stations = stations,
                selectedStation = selectedStation,
                onSelectStation = { selectedStation = it },
                isExpanded = isMapExpanded,
                onToggleExpand = { isMapExpanded = !isMapExpanded }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .padding(end = 56.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF071424).copy(alpha = 0.9f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = BrightCyan, modifier = Modifier.size(8.dp)) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stations.count { it.status == "ACTIVA" }} Activas",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                val alertCount = stations.count { it.status == "ALERTA" }
                if (alertCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AlertRed.copy(alpha = 0.92f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$alertCount en Alerta",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    val stationItemsContent: LazyListScope.() -> Unit = {
        item {
            SeismicWaveformView(
                        station = selectedStation,
                        latestReading = latestReading,
                        thresholdRatio = staLtaSensitivity,
                        isMonitoring = isMonitoring,
                        onStartMonitoringClick = {
                            scope.launch {
                                repository.resetLocalStationStatus()
                                val localSt = repository.getOrCreateLocalStation()
                                selectedStation = localSt
                            }
                            val isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            } else true

                            if (isNotificationGranted) {
                                SeismicMonitoringService.start(context)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onDeselectStation = if (selectedStation != null) { { selectedStation = null } } else null
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xCC091A2E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = BrightCyan, modifier = Modifier.size(8.dp)) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Telemetría Real del Dispositivo",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x2E00F5D4),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, BrightCyan)
                                ) {
                                    Text(
                                        text = "Hardware en Vivo",
                                        color = BrightCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x8005111F), RoundedCornerShape(12.dp))
                                    .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = BrightCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Acelerómetro Triaxial Físico",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Muestreo continuo a ~${liveTelemetry.sensors.calculatedSampleRateHz} Hz",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = BrightCyan
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Calibración Vectorial ✓",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = com.example.ui.theme.LiveGreenGlow,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Filtro Gravitacional IIR",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x8005111F), RoundedCornerShape(12.dp))
                                    .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Acelerómetro Dinámico (PGA):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = BrightCyan
                                    )
                                    Text(
                                        text = "${"%.3f".format(liveTelemetry.sensors.resultantPgaMps2)} m/s² (~${liveTelemetry.sensors.calculatedSampleRateHz} Hz)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "X: ${"%.2f".format(liveTelemetry.sensors.dynamicAccelX)}  Y: ${"%.2f".format(liveTelemetry.sensors.dynamicAccelY)}  Z: ${"%.2f".format(liveTelemetry.sensors.dynamicAccelZ)} m/s²",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                                        color = Color(0xFFCBD5E1)
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Giroscopio: ${if (liveTelemetry.sensors.hasGyroscope) "Activo ✓" else "No disponible"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = if (liveTelemetry.sensors.hasGyroscope) Color(0xFFCBD5E1) else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Brújula: ${if (liveTelemetry.sensors.hasMagnetometer) "${liveTelemetry.sensors.compassHeadingDegrees.toInt()}°" else "N/A"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = if (liveTelemetry.sensors.hasMagnetometer) BrightCyan else Color(0xFF64748B)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GPS: ${"%.4f".format(liveTelemetry.location.latitude)}, ${"%.4f".format(liveTelemetry.location.longitude)} (${liveTelemetry.location.nearestMunicipality})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "±${liveTelemetry.location.accuracyMeters.toInt()}m • ${liveTelemetry.deviceModel.take(16)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x990A1D33),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2E00F5D4))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (isMonitoring) {
                                            SeismicMonitoringService.stop(context)
                                        } else {
                                            scope.launch {
                                                repository.resetLocalStationStatus()
                                                val localSt = repository.getOrCreateLocalStation()
                                                selectedStation = localSt
                                            }
                                            val isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                            } else true

                                            if (isNotificationGranted) {
                                                SeismicMonitoringService.start(context)
                                            } else {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMonitoring) AlertRed else BrightCyan,
                                        contentColor = if (isMonitoring) Color.White else Color(0xFF071424)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMonitoring) "Detener" else "Iniciar Monitoreo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showRegisterDialog = true },
                                    modifier = Modifier.weight(0.9f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrightCyan)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Registrar", style = MaterialTheme.typography.labelSmall, maxLines = 1, color = BrightCyan)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val alert = repository.triggerLocalStationAnomaly(0.85f, 4.6f)
                                            val alertSt = stations.find { it.isLocalDevice } ?: stations.firstOrNull()
                                            if (alertSt != null) selectedStation = alertSt
                                            snackbarHostState.showSnackbar("¡Anomalía sísmica detectada! Alerta visible en el mapa.")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertAmber, contentColor = Color(0xFF071424)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Probar Alerta en Mapa", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = { showDiagnosticsDialog = true },
                                    modifier = Modifier.weight(0.7f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrightCyan)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Sensores", style = MaterialTheme.typography.labelSmall, maxLines = 1, color = BrightCyan)
                                }
                            }
                        }
                    }
                }

                if (stations.any { it.status == "ALERTA" }) {
                    item {
                        val alertSt = stations.firstOrNull { it.status == "ALERTA" }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = AlertRed.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, AlertRed)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "¡ESTACIÓN EN ALERTA SÍSMICA!",
                                        fontWeight = FontWeight.Black,
                                        color = AlertRed,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${alertSt?.name ?: "Estación"} detectó anomalía (${alertSt?.lastVibrationMps2 ?: 0f} m/s²). Visible en el mapa.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            stations.filter { it.status == "ALERTA" }.forEach {
                                                repository.resetStationAlert(it.id)
                                            }
                                            repository.resetLocalStation()
                                            snackbarHostState.showSnackbar("Estación restablecida a estado normal (ACTIVA).")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Normalizar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x990A1D33),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2200F5D4))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sensibilidad Algoritmo STA/LTA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Umbral: ${"%.1f".format(staLtaSensitivity)}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrightCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = staLtaSensitivity,
                                onValueChange = { staLtaSensitivity = it },
                                valueRange = 2.0f..6.0f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = BrightCyan,
                                    activeTrackColor = BrightCyan
                                )
                            )
                            Text(
                                text = "Un valor menor (2.0x-3.0x) detecta vibraciones sutiles. Un valor mayor (4.5x-6.0x) filtra ruidos cotidianos.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Red Sismológica SGC y Estaciones (${stations.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Telemetría real • Toca para enfocar en mapa",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF94A3B8)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    repository.refreshAllStationsTelemetry()
                                    snackbarHostState.showSnackbar("Telemetría real de estaciones actualizada.")
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar Telemetría", tint = BrightCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                items(stations) { st ->
                    val isSelected = selectedStation?.id == st.id
                    val statusColor = when (st.status) {
                        "ALERTA" -> AlertRed
                        "ACTIVA" -> if (st.isLocalDevice) BrightCyan else com.example.ui.theme.LiveGreenGlow
                        else -> Color.Gray
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStation = st },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xD90E2A4A) else Color(0x9908182B),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 0.8.dp,
                            if (isSelected) statusColor else Color(0x2200F5D4)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(shape = CircleShape, color = statusColor, modifier = Modifier.size(9.dp)) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = st.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrightCyan.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, BrightCyan)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(shape = CircleShape, color = BrightCyan, modifier = Modifier.size(5.dp)) {}
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Sismógrafo Activo",
                                                color = BrightCyan,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusColor.copy(alpha = 0.18f),
                                        border = androidx.compose.foundation.BorderStroke(0.6.dp, statusColor)
                                    ) {
                                        Text(
                                            text = st.status,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${st.municipality}, ${st.department} • Modelo: ${st.deviceModel}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFFCBD5E1)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PGA: ${st.lastVibrationMps2} m/s²",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = if (st.status == "ALERTA") AlertRed else BrightCyan,
                                    fontWeight = if (st.status == "ALERTA") FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${st.sampleRateHz} Hz",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = st.mountType.take(14),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "X:${"%.2f".format(st.realAccelX)} Y:${"%.2f".format(st.realAccelY)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF030A14),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, BrightCyan.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val midY = h / 2f

                                        drawLine(
                                            color = Color(0x1F00F5D4),
                                            start = Offset(0f, midY),
                                            end = Offset(w, midY),
                                            strokeWidth = 1f
                                        )

                                        val pga = st.lastVibrationMps2.coerceAtLeast(0.015f)
                                        val points = 35
                                        val stepX = w / (points - 1)
                                        val path = Path()
                                        val time = System.currentTimeMillis() / 140.0

                                        for (i in 0 until points) {
                                            val x = i * stepX
                                            val wave = sin(time + i * 0.5).toFloat() * 0.65f +
                                                    cos(time * 1.6 + i * 0.9).toFloat() * 0.35f
                                            val dy = (wave * pga * (h * 0.42f)).coerceIn(-midY * 0.85f, midY * 0.85f)
                                            val y = midY - dy
                                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }

                                        drawPath(
                                            path = path,
                                            color = if (st.status == "ALERTA") AlertRed else BrightCyan,
                                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                mapBoxContent(
                    if (isMapExpanded) Modifier.fillMaxSize()
                    else Modifier.weight(1.1f).fillMaxHeight()
                )
                if (!isMapExpanded) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = stationItemsContent
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                mapBoxContent(
                    if (isMapExpanded) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth().weight(1.0f)
                )
                if (!isMapExpanded) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.0f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = stationItemsContent
                    )
                }
            }
        }
    }


    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            icon = {
                Icon(Icons.Default.Tune, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Diagnóstico de Sensores Físicos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Evaluación de hardware de este dispositivo para la Red de Monitoreo:", style = MaterialTheme.typography.bodySmall)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Dispositivo: ${hardwareDiagnostics.deviceModel}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Sistema: ${hardwareDiagnostics.androidVersion}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                            Text("Acelerómetro: ${if (hardwareDiagnostics.hasAccelerometer) "Disponible ✓ (${hardwareDiagnostics.accelerometerVendor})" else "No disponible ✗"}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                            Text("Giroscopio: ${if (hardwareDiagnostics.hasGyroscope) "Disponible ✓" else "No disponible ✗"}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                            Text("Magnetómetro: ${if (hardwareDiagnostics.hasMagnetometer) "Disponible ✓" else "No disponible ✗"}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                            Text("Frecuencia máx estimada: ~${hardwareDiagnostics.estimatedMaxRateHz} Hz", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        }
                    }

                    Text(
                        text = hardwareDiagnostics.diagnosisSummary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (hardwareDiagnostics.isViableForMonitoring) BrightCyan else AlertAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRegisterDialog) {
        val prioritizedMunicipalities = listOf(
            Triple("Armenia", "Quindío", Pair(4.5339, -75.6811)),
            Triple("Pereira", "Risaralda", Pair(4.8133, -75.6961)),
            Triple("Manizales", "Caldas", Pair(5.0689, -75.5174)),
            Triple("Cali", "Valle del Cauca", Pair(3.4516, -76.5320)),
            Triple("Medellín", "Antioquia", Pair(6.2442, -75.5812)),
            Triple("Ibagué", "Tolima", Pair(4.4389, -75.2322)),
            Triple("Bogotá", "Cundinamarca", Pair(4.6097, -74.0817)),
            Triple("Bucaramanga", "Santander", Pair(7.1193, -73.1227)),
            Triple("Quibdó", "Chocó", Pair(5.6947, -76.6611)),
            Triple("Neiva", "Huila", Pair(2.9273, -75.2819))
        )

        var useRealGps by remember { mutableStateOf(liveTelemetry.location.hasLocation) }
        var selectedMunIndex by remember { mutableIntStateOf(0) }
        var stationName by remember { mutableStateOf("Estación Celular " + hardwareDiagnostics.deviceModel.take(12)) }
        var mountType by remember { mutableStateOf("Pared de concreto estructural") }

        val currentMun = prioritizedMunicipalities[selectedMunIndex]
        val activeLat = if (useRealGps) liveTelemetry.location.latitude else currentMun.third.first
        val activeLon = if (useRealGps) liveTelemetry.location.longitude else currentMun.third.second
        val activeMunName = if (useRealGps) liveTelemetry.location.nearestMunicipality else currentMun.first
        val activeDeptName = if (useRealGps) liveTelemetry.location.nearestDepartment else currentMun.second

        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = BrightCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar Celular como Estación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = BrightCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sensor: Acelerómetro Triaxial Físico",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "~${liveTelemetry.sensors.calculatedSampleRateHz} Hz Activo",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = BrightCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = useRealGps,
                            onClick = { useRealGps = true },
                            label = { Text("📍 GPS Real del Celular", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrightCyan,
                                selectedLabelColor = Color(0xFF071424)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !useRealGps,
                            onClick = { useRealGps = false },
                            label = { Text("🇨🇴 Municipio Red", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrightCyan,
                                selectedLabelColor = Color(0xFF071424)
                            ),
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    if (!useRealGps) {
                        Text(
                            text = "Selecciona el municipio donde se desplegará:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            prioritizedMunicipalities.forEachIndexed { idx, mun ->
                                FilterChip(
                                    selected = selectedMunIndex == idx,
                                    onClick = { selectedMunIndex = idx },
                                    label = { Text(mun.first, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrightCyan,
                                        selectedLabelColor = Color(0xFF071424)
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = stationName,
                        onValueChange = { stationName = it },
                        label = { Text("Nombre de la Estación") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0C223A)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$activeMunName, $activeDeptName (${"%.4f".format(activeLat)}, ${"%.4f".format(activeLon)})",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = BrightCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Tipo de anclaje físico:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    listOf("Pared de concreto estructural", "Suelo firme / Losa basal", "Mástil o talud monitoreado").forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mountType = type }
                        ) {
                            RadioButton(selected = mountType == type, onClick = { mountType = type })
                            Text(type, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF040E1B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = com.example.ui.theme.LiveGreenGlow, modifier = Modifier.size(7.dp)) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sismógrafo de Calibración en Vivo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0x3300F5D4)
                                ) {
                                    Text("100% DATOS REALES", color = BrightCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Seismograph live waveform inside dialog
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(65.dp)
                                    .background(Color(0x99030812), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, Color(0x2200F5D4), RoundedCornerShape(8.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val midY = h / 2f

                                    // Midline
                                    drawLine(
                                        color = Color(0x2200F5D4),
                                        start = Offset(0f, midY),
                                        end = Offset(w, midY),
                                        strokeWidth = 1f
                                    )

                                    val pga = latestReading.resultantMagnitude.coerceAtLeast(0.01f)
                                    val points = 35
                                    val stepX = w / (points - 1)
                                    val path = Path()
                                    val time = System.currentTimeMillis() / 120.0

                                    for (i in 0 until points) {
                                        val x = i * stepX
                                        val wave = (sin(time + i * 0.4) * latestReading.x.coerceIn(-1.5f, 1.5f) +
                                                cos(time * 1.4 + i * 0.7) * latestReading.y.coerceIn(-1.5f, 1.5f)).toFloat()
                                        val dy = ((wave * 0.5f + pga * 15f)).coerceIn(-midY * 0.85f, midY * 0.85f)
                                        val y = midY - dy
                                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }

                                    drawPath(
                                        path = path,
                                        color = BrightCyan,
                                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "PGA: ${"%.3f".format(latestReading.resultantMagnitude)} m/s² • X: ${"%.2f".format(latestReading.x)}  Y: ${"%.2f".format(latestReading.y)}  Z: ${"%.2f".format(latestReading.z)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color(0xFFCBD5E1),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "• Telemetría sismológica en tiempo real: acelerómetro triaxial, PGA y coordenadas transmitidas continuamente.\n• La estación aparecerá de inmediato con su indicador en el mapa y sismógrafo en vivo.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val newSt = repository.registerCurrentDeviceAsStation(
                                stationName = stationName,
                                municipality = activeMunName,
                                department = activeDeptName,
                                latitude = activeLat,
                                longitude = activeLon,
                                mountType = mountType
                            )
                            selectedStation = newSt
                            showRegisterDialog = false
                            repository.resetLocalStationStatus()
                            if (!isMonitoring) {
                                SeismicMonitoringService.start(context)
                            }
                            com.example.EcoSismoApp.instance.firebaseSyncRepository.saveStationToFirestore(newSt)
                            snackbarHostState.showSnackbar("Estación '${newSt.name}' registrada y guardada en Firebase Firestore.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirmar y Activar Sismógrafo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

