package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectionPoint
import com.example.data.model.DeviceDonation
import com.example.data.repository.CollectionRepository
import com.example.data.repository.RouteOptimizationResult
import com.example.ui.components.ColombiaMapView
import com.example.ui.components.MapDisplayMode
import com.example.ui.components.openGoogleMapsNavigation
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BrightCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CollectionPointsScreen(
    repository: CollectionRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val points = remember { repository.getCollectionPoints() }
    var selectedPoint by remember { mutableStateOf(points.first()) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showRouteDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val selectedForRoute = remember { mutableStateListOf<String>() }
    var optimizedRouteResult by remember { mutableStateOf<RouteOptimizationResult?>(null) }

    val donations by repository.allDonations.collectAsState(initial = emptyList())
    val donationCount by repository.donationCount.collectAsState(initial = 0)

    var isMapExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        if (!isMapExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                points.forEach { point ->
                    val isSelected = selectedPoint.id == point.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPoint = point },
                        label = { Text(point.city, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF040D17)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrightCyan,
                            selectedLabelColor = Color(0xFF040D17),
                            containerColor = Color(0x6608182B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) BrightCyan else Color(0x2200F5D4)
                        )
                    )
                }
            }
        }

        Box(
            modifier = if (isMapExpanded) Modifier.fillMaxSize()
            else Modifier
                .fillMaxWidth()
                .weight(1.05f)
        ) {
            ColombiaMapView(
                mode = MapDisplayMode.COLLECTION_POINTS,
                collectionPoints = points,
                selectedPoint = selectedPoint,
                onSelectPoint = { selectedPoint = it },
                routeStops = optimizedRouteResult?.stops ?: emptyList(),
                isExpanded = isMapExpanded,
                onToggleExpand = { isMapExpanded = !isMapExpanded }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .padding(end = 60.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC071628),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4)),
                    shadowElevation = 6.dp
                ) {
                    TextButton(
                        onClick = { showRouteDialog = true },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.AltRoute, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (optimizedRouteResult != null) "Ruta (${optimizedRouteResult?.totalDistanceKm} km)" else "Ruta Optimizada",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC071628),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300B4D8)),
                    shadowElevation = 6.dp
                ) {
                    TextButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Entregas ($donationCount)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (!isMapExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xD9071526),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x2E00F5D4)),
                shadowElevation = 8.dp
            ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0x2E00F5D4),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BrightCyan.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = selectedPoint.department.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Punto Oficial Verificado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = selectedPoint.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2E00F5D4),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, BrightCyan)
                        ) {
                            IconButton(
                                onClick = {
                                    openGoogleMapsNavigation(
                                        context,
                                        selectedPoint.latitude,
                                        selectedPoint.longitude,
                                        selectedPoint.name
                                    )
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    Icons.Default.Directions,
                                    contentDescription = "Cómo llegar",
                                    tint = BrightCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x8005111F), RoundedCornerShape(14.dp))
                            .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedPoint.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedPoint.openingHours,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tel: ${selectedPoint.contactPhone} • ${selectedPoint.officeUnit}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                item {

                    Text(
                        text = "¿QUÉ HACER AL LLEGAR?",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    selectedPoint.instructions.forEachIndexed { index, instr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentCyan.copy(alpha = 0.15f),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = instr,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDonationDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Icon(Icons.Default.Recycling, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar Entrega", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                openGoogleMapsNavigation(
                                    context,
                                    selectedPoint.latitude,
                                    selectedPoint.longitude,
                                    selectedPoint.name
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cómo Llegar")
                        }
                    }
                }

                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fuente verificada: ${selectedPoint.verifiedSource}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

    if (showDonationDialog) {
        var brand by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var condition by remember { mutableStateOf("Funcional") }
        var donorName by remember { mutableStateOf("") }
        var donorContact by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var hasAccel by remember { mutableStateOf(true) }
        var hasGyro by remember { mutableStateOf(false) }
        var hasBatteryOk by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar Entrega de Celular", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Punto: ${selectedPoint.name} (${selectedPoint.city})",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marca (ej. Xiaomi, Samsung, Motorola)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Modelo (ej. Redmi Note 8, Moto G6)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        Text("Estado del Dispositivo:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        listOf("Funcional", "Parcialmente funcional (pantalla partida)", "No enciende / Para partes").forEach { cond ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { condition = cond }
                            ) {
                                RadioButton(selected = condition == cond, onClick = { condition = cond })
                                Text(cond, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        Text("Sensores & Componentes Operativos:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasAccel, onCheckedChange = { hasAccel = it })
                            Text("Acelerómetro (Vibraciones)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasGyro, onCheckedChange = { hasGyro = it })
                            Text("Giroscopio (Inclinación)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasBatteryOk, onCheckedChange = { hasBatteryOk = it })
                            Text("Batería operativa (No inflada)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = donorName,
                            onValueChange = { donorName = it },
                            label = { Text("Nombre del Donante / Ciudadano") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = donorContact,
                            onValueChange = { donorContact = it },
                            label = { Text("Teléfono o Correo de contacto") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Observaciones / Detalles técnicos") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (brand.isNotBlank() && model.isNotBlank()) {
                            val sensorsList = buildList {
                                if (hasAccel) add("Acelerómetro")
                                if (hasGyro) add("Giroscopio")
                                if (hasBatteryOk) add("Batería OK")
                            }.joinToString(", ")

                            scope.launch {
                                repository.registerDonation(
                                    brand = brand,
                                    model = model,
                                    condition = condition,
                                    workingSensors = sensorsList,
                                    point = selectedPoint,
                                    donorName = donorName.ifBlank { "Ciudadano Anónimo" },
                                    donorContact = donorContact,
                                    notes = notes
                                )
                                showDonationDialog = false
                                snackbarHostState.showSnackbar("¡Entrega registrada con éxito! Guardada en almacenamiento local.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("Guardar Entrega")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showRouteDialog) {
        AlertDialog(
            onDismissRequest = { showRouteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AltRoute, contentDescription = null, tint = BrightCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimizar Ruta de Recolección", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Selecciona las alcaldías para calcular la ruta multi-parada más eficiente:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(points) { pt ->
                            val isChecked = selectedForRoute.contains(pt.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedForRoute.remove(pt.id)
                                        else selectedForRoute.add(pt.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedForRoute.add(pt.id)
                                        else selectedForRoute.remove(pt.id)
                                    }
                                )
                                Column {
                                    Text(pt.city, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(pt.name, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color.Gray)
                                }
                            }
                        }
                    }

                    optimizedRouteResult?.let { result ->
                        HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrightCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Ruta Calculada: ${result.stops.size} paradas",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = BrightCyan
                            )
                            Text(
                                text = "Distancia total estimada: ${result.totalDistanceKm} km",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Tiempo de viaje estimado: ~${result.estimatedDurationMinutes / 60}h ${result.estimatedDurationMinutes % 60}m",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Orden: " + result.stops.joinToString(" → ") { it.city },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color.LightGray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedForRoute.isNotEmpty()) {
                            val result = repository.optimizeMultiStopRoute(selectedPoint.id, selectedForRoute.toList())
                            optimizedRouteResult = result
                            showRouteDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Ruta calculada: ${result.stops.size} paradas, ${result.totalDistanceKm} km.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan)
                ) {
                    Text("Calcular Ruta", color = Color(0xFF071424), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (optimizedRouteResult != null) {
                    TextButton(onClick = {
                        optimizedRouteResult = null
                        selectedForRoute.clear()
                    }) {
                        Text("Limpiar Ruta")
                    }
                } else {
                    TextButton(onClick = { showRouteDialog = false }) {
                        Text("Cerrar")
                    }
                }
            }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Recycling, contentDescription = null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Historial de Celulares Donados", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                if (donations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay entregas registradas en este dispositivo. Puedes registrar la primera con el botón 'Registrar Entrega'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(donations) { d ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${d.brand} ${d.model}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (d.isSynced) AccentCyan.copy(alpha = 0.2f) else AlertAmber.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (d.isSynced) "Sincronizado" else "Pendiente de Sync",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (d.isSynced) AccentCyan else AlertAmber,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Punto: ${d.city} • Estado: ${d.condition}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Sensores: ${d.workingSensors} • Donante: ${d.donorName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

