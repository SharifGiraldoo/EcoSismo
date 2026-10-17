package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightCyan
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

    Column(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = if (isMapExpanded) Modifier.fillMaxSize()
            else Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
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

        if (!isMapExpanded) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {

                    SeismicWaveformView(
                        latestReading = latestReading,
                        thresholdRatio = staLtaSensitivity,
                        isMonitoring = isMonitoring
                    )
                }

                item {

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1E35)),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(BrightCyan.copy(alpha = 0.5f)))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrightCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Hardware en Vivo",
                                        color = BrightCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (liveTelemetry.battery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                        contentDescription = null,
                                        tint = if (liveTelemetry.battery.isCharging) BrightCyan else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Batería: ${liveTelemetry.battery.levelPercent}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (liveTelemetry.battery.isCharging) "⚡ ${liveTelemetry.battery.chargingSource}" else "🔋 Batería Interna",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = if (liveTelemetry.battery.isCharging) BrightCyan else Color.Gray
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${liveTelemetry.battery.temperatureCelsius}°C • ${liveTelemetry.battery.voltageMv} mV",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Salud: ${liveTelemetry.battery.health}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = Color.Gray
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Acelerómetro Dinámico (PGA):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = BrightCyan
                                    )
                                    Text(
                                        text = "${"%.3f".format(liveTelemetry.sensors.resultantPgaMps2)} m/s² (~${liveTelemetry.sensors.calculatedSampleRateHz} Hz)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
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
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                                        color = Color(0xFFCBD5E1)
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Giroscopio: ${if (liveTelemetry.sensors.hasGyroscope) "Activo ✓" else "No disponible"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = if (liveTelemetry.sensors.hasGyroscope) Color(0xFFCBD5E1) else Color.Gray
                                    )
                                    Text(
                                        text = "Brújula: ${if (liveTelemetry.sensors.hasMagnetometer) "${liveTelemetry.sensors.compassHeadingDegrees.toInt()}°" else "N/A"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = if (liveTelemetry.sensors.hasMagnetometer) BrightCyan else Color.Gray
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

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
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
                                                repository.getOrCreateLocalStation()
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
                                    )
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
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Registrar", style = MaterialTheme.typography.labelSmall, maxLines = 1)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertAmber, contentColor = Color(0xFF071424))
                                ) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Probar Alerta en Mapa", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = { showDiagnosticsDialog = true },
                                    modifier = Modifier.weight(0.7f)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Sensores", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                if (stations.any { it.status == "ALERTA" }) {
                    item {
                        val alertSt = stations.firstOrNull { it.status == "ALERTA" }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.12f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                        color = MaterialTheme.colorScheme.onSurface
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
                                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424))
                                ) {
                                    Text("Normalizar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
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
                                    fontWeight = FontWeight.Bold
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
                                color = Color.Gray
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
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Telemetría real • Toca para enfocar en mapa",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.Gray
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
                        "ACTIVA" -> if (st.isLocalDevice) BrightCyan else AccentCyan
                        else -> Color.Gray
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStation = st },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 2.dp, brush = androidx.compose.ui.graphics.SolidColor(statusColor))
                        else CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(shape = CircleShape, color = statusColor, modifier = Modifier.size(10.dp)) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = st.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.15f)
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

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${st.municipality}, ${st.department} • Modelo: ${st.deviceModel}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vibración: ${st.lastVibrationMps2} m/s²",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = if (st.status == "ALERTA") AlertRed else BrightCyan,
                                    fontWeight = if (st.status == "ALERTA") FontWeight.Bold else FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (st.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                        contentDescription = null,
                                        tint = if (st.isCharging) BrightCyan else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${st.batteryLevel}% ${if (st.isCharging) "(⚡ " + st.chargingSource + ")" else ""}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${st.sampleRateHz}Hz",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
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
                                    imageVector = if (liveTelemetry.battery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = if (liveTelemetry.battery.isCharging) BrightCyan else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${liveTelemetry.battery.levelPercent}% ${if (liveTelemetry.battery.isCharging) "(⚡ " + liveTelemetry.battery.chargingSource + ")" else ""}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${liveTelemetry.battery.temperatureCelsius}°C • Acel: ~${liveTelemetry.sensors.calculatedSampleRateHz}Hz",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color.Gray
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

                    Text(
                        text = "• Telemetría real en tiempo real: acelerómetro, carga de batería y coordenadas transmitidas continuamente.\n• La estación aparecerá de inmediato con su indicador en el mapa.",
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
                            if (!isMonitoring) {
                                SeismicMonitoringService.start(context)
                            }
                            snackbarHostState.showSnackbar("¡Estación registrada en $activeMunName con telemetría en tiempo real!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424))
                ) {
                    Text("Registrar en Red y Mapa", fontWeight = FontWeight.Bold)
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

