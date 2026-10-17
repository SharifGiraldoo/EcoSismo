package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectionPoint
import com.example.data.model.MonitoringStation
import com.example.data.model.SgcEarthquake
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.LiveGreenGlow
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos

enum class MapDisplayMode {
    COLLECTION_POINTS,
    SEISMIC_HISTORY,
    MONITORING_STATIONS
}

@Composable
fun ColombiaMapView(
    mode: MapDisplayMode,
    collectionPoints: List<CollectionPoint> = emptyList(),
    selectedPoint: CollectionPoint? = null,
    onSelectPoint: (CollectionPoint) -> Unit = {},
    routeStops: List<CollectionPoint> = emptyList(),
    earthquakes: List<SgcEarthquake> = emptyList(),
    selectedEarthquake: SgcEarthquake? = null,
    onSelectEarthquake: (SgcEarthquake) -> Unit = {},
    stations: List<MonitoringStation> = emptyList(),
    selectedStation: MonitoringStation? = null,
    onSelectStation: (MonitoringStation) -> Unit = {},
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    val cityTextPaint = remember(density) {
        AndroidPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 11.sp.toPx() }
            color = AndroidColor.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 0f, 0f, AndroidColor.BLACK)
        }
    }

    val eqTextPaint = remember(density) {
        AndroidPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 11.sp.toPx() }
            color = AndroidColor.parseColor("#00E5FF")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 0f, 0f, AndroidColor.BLACK)
        }
    }

    val stationTextPaint = remember(density) {
        AndroidPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 11.sp.toPx() }
            color = AndroidColor.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(6f, 0f, 0f, AndroidColor.BLACK)
        }
    }

    val alertBadgePaint = remember(density) {
        AndroidPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 11.sp.toPx() }
            color = AndroidColor.parseColor("#FF3D71")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f, 0f, 0f, AndroidColor.BLACK)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val minLon = -79.5
    val maxLon = -66.8
    val minLat = -4.5
    val maxLat = 13.0

    fun projectToCanvas(lat: Double, lon: Double, width: Float, height: Float): Offset {
        val normX = ((lon - minLon) / (maxLon - minLon)).toFloat()
        val normY = 1.0f - ((lat - minLat) / (maxLat - minLat)).toFloat()

        val baseCanvasW = width * 0.85f
        val baseCanvasH = height * 0.88f
        val offsetX = (width - baseCanvasW) / 2f
        val offsetY = (height - baseCanvasH) / 2f

        val localX = offsetX + (normX * baseCanvasW)
        val localY = offsetY + (normY * baseCanvasH)

        val centerX = width / 2f
        val centerY = height / 2f
        val transformedX = centerX + (localX - centerX) * zoom + panX
        val transformedY = centerY + (localY - centerY) * zoom + panY

        return Offset(transformedX, transformedY)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF071424))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(0.6f, 6.0f)
                    zoom = newZoom
                    val maxPan = 2000f * newZoom
                    panX = (panX + pan.x).coerceIn(-maxPan, maxPan)
                    panY = (panY + pan.y).coerceIn(-maxPan, maxPan)
                }
            }
            .pointerInput(mode, collectionPoints, earthquakes, stations, zoom, panX, panY) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()

                    when (mode) {
                        MapDisplayMode.COLLECTION_POINTS -> {
                            val touched = collectionPoints.find { point ->
                                val pt = projectToCanvas(point.latitude, point.longitude, w, h)
                                hypot((tapOffset.x - pt.x).toDouble(), (tapOffset.y - pt.y).toDouble()) < 42.0
                            }
                            touched?.let { onSelectPoint(it) }
                        }
                        MapDisplayMode.SEISMIC_HISTORY -> {
                            val touched = earthquakes.find { q ->
                                val pt = projectToCanvas(q.latitude, q.longitude, w, h)
                                hypot((tapOffset.x - pt.x).toDouble(), (tapOffset.y - pt.y).toDouble()) < 42.0
                            }
                            touched?.let { onSelectEarthquake(it) }
                        }
                        MapDisplayMode.MONITORING_STATIONS -> {
                            val touched = stations.find { st ->
                                val pt = projectToCanvas(st.latitude, st.longitude, w, h)
                                hypot((tapOffset.x - pt.x).toDouble(), (tapOffset.y - pt.y).toDouble()) < 42.0
                            }
                            touched?.let { onSelectStation(it) }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val native = drawContext.canvas.nativeCanvas

            val lats = listOf(0.0, 4.0, 8.0, 12.0)
            val lons = listOf(-78.0, -74.0, -70.0)

            lats.forEach { lat ->
                val p1 = projectToCanvas(lat, -79.5, canvasW, canvasH)
                val p2 = projectToCanvas(lat, -66.8, canvasW, canvasH)
                drawLine(
                    color = Color(0xFF132A4A).copy(alpha = 0.5f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.dp.toPx()
                )
            }

            lons.forEach { lon ->
                val p1 = projectToCanvas(-4.5, lon, canvasW, canvasH)
                val p2 = projectToCanvas(13.0, lon, canvasW, canvasH)
                drawLine(
                    color = Color(0xFF132A4A).copy(alpha = 0.5f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.dp.toPx()
                )
            }

            val boundaryPoints = listOf(
                Pair(12.45, -71.66),
                Pair(11.8, -71.3),
                Pair(11.3, -72.6),
                Pair(9.5, -73.2),
                Pair(8.5, -72.5),
                Pair(7.1, -71.8),
                Pair(6.2, -69.9),
                Pair(4.0, -67.7),
                Pair(1.2, -66.9),
                Pair(-1.2, -69.5),
                Pair(-4.2, -69.9),
                Pair(-2.5, -73.0),
                Pair(-0.5, -75.2),
                Pair(0.8, -77.6),
                Pair(1.5, -78.8),
                Pair(3.9, -77.3),
                Pair(6.5, -77.4),
                Pair(8.1, -76.8),
                Pair(9.4, -75.8),
                Pair(10.5, -75.5),
                Pair(11.0, -74.8),
                Pair(11.3, -74.2),
                Pair(12.45, -71.66)
            )

            val borderPath = Path()
            boundaryPoints.forEachIndexed { idx, (lat, lon) ->
                val p = projectToCanvas(lat, lon, canvasW, canvasH)
                if (idx == 0) borderPath.moveTo(p.x, p.y) else borderPath.lineTo(p.x, p.y)
            }
            borderPath.close()

            drawPath(
                path = borderPath,
                color = Color(0xFF0C1E34)
            )
            drawPath(
                path = borderPath,
                color = Color(0xFF1E3A5F),
                style = Stroke(width = 2.dp.toPx())
            )

            if (mode == MapDisplayMode.COLLECTION_POINTS && routeStops.size >= 2) {
                val routePath = Path()
                routeStops.forEachIndexed { i, pt ->
                    val canvasPt = projectToCanvas(pt.latitude, pt.longitude, canvasW, canvasH)
                    if (i == 0) routePath.moveTo(canvasPt.x, canvasPt.y)
                    else routePath.lineTo(canvasPt.x, canvasPt.y)
                }
                drawPath(
                    path = routePath,
                    color = BrightCyan,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                    )
                )
            }

            when (mode) {
                MapDisplayMode.COLLECTION_POINTS -> {
                    collectionPoints.forEach { point ->
                        val pt = projectToCanvas(point.latitude, point.longitude, canvasW, canvasH)
                        val isSelected = selectedPoint?.id == point.id

                        if (isSelected) {
                            drawCircle(
                                color = BrightCyan.copy(alpha = 0.35f),
                                radius = 22.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = BrightCyan,
                                radius = 16.dp.toPx(),
                                center = pt,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        drawCircle(
                            color = if (isSelected) BrightCyan else AccentCyan,
                            radius = if (isSelected) 10.dp.toPx() else 8.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = pt
                        )

                        val textX = pt.x + 12.dp.toPx()
                        val textY = pt.y + 4.dp.toPx()
                        if (textX in -50f..(canvasW + 50f) && textY in 0f..(canvasH + 50f)) {
                            try {
                                cityTextPaint.color = if (isSelected) AndroidColor.parseColor("#00E5FF") else AndroidColor.WHITE
                                native.drawText(point.city, textX, textY, cityTextPaint)
                            } catch (_: Throwable) {}
                        }
                    }
                }

                MapDisplayMode.SEISMIC_HISTORY -> {
                    earthquakes.forEach { eq ->
                        val pt = projectToCanvas(eq.latitude, eq.longitude, canvasW, canvasH)
                        val isSelected = selectedEarthquake?.id == eq.id

                        val color = when {
                            eq.magnitude >= 4.5 -> AlertRed
                            eq.magnitude >= 3.5 -> AlertAmber
                            else -> BrightCyan
                        }

                        val radiusBase = (eq.magnitude.toFloat() * 3.5f).dp.toPx()

                        if (eq.magnitude >= 4.0 || isSelected) {
                            drawCircle(
                                color = color.copy(alpha = pulseAlpha),
                                radius = radiusBase + (pulseRadius * 0.8f),
                                center = pt
                            )
                        }

                        drawCircle(
                            color = color.copy(alpha = 0.25f),
                            radius = radiusBase,
                            center = pt
                        )
                        drawCircle(
                            color = color,
                            radius = radiusBase * 0.5f,
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = pt
                        )

                        val textX = pt.x + 10.dp.toPx()
                        val textY = pt.y + 4.dp.toPx()
                        if (textX in -50f..(canvasW + 50f) && textY in 0f..(canvasH + 50f)) {
                            try {
                                eqTextPaint.color = when {
                                    eq.magnitude >= 4.5 -> AndroidColor.parseColor("#FF3D71")
                                    eq.magnitude >= 3.5 -> AndroidColor.parseColor("#FFAA00")
                                    else -> AndroidColor.parseColor("#00E5FF")
                                }
                                native.drawText("M${eq.magnitude}", textX, textY, eqTextPaint)
                            } catch (_: Throwable) {}
                        }
                    }
                }

                MapDisplayMode.MONITORING_STATIONS -> {
                    stations.forEach { st ->
                        val pt = projectToCanvas(st.latitude, st.longitude, canvasW, canvasH)
                        val isSelected = selectedStation?.id == st.id

                        val stColor = when (st.status) {
                            "ALERTA" -> AlertRed
                            "ACTIVA" -> if (st.isLocalDevice) BrightCyan else AccentCyan
                            else -> Color.Gray
                        }

                        if (st.status == "ALERTA") {

                            drawCircle(
                                color = AlertRed.copy(alpha = pulseAlpha * 0.35f),
                                radius = (pulseRadius * 3.5f + 25f),
                                center = pt,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = AlertAmber.copy(alpha = pulseAlpha * 0.6f),
                                radius = (pulseRadius * 2.0f + 15f),
                                center = pt,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = AlertRed.copy(alpha = pulseAlpha * 0.5f),
                                radius = (pulseRadius + 8f),
                                center = pt
                            )
                        } else if (st.isLocalDevice) {
                            drawCircle(
                                color = BrightCyan.copy(alpha = 0.35f),
                                radius = 18.dp.toPx(),
                                center = pt
                            )
                        }

                        drawCircle(
                            color = stColor,
                            radius = if (st.status == "ALERTA") 12.dp.toPx() else 9.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = if (st.status == "ALERTA") 5.dp.toPx() else 3.5.dp.toPx(),
                            center = pt
                        )

                        if (st.status == "ALERTA") {
                            val alertBadgeText = "¡ALERTA! ${st.lastVibrationMps2} m/s²"
                            val badgeX = pt.x - 30.dp.toPx()
                            val badgeY = pt.y - 18.dp.toPx()
                            if (badgeX in -100f..(canvasW + 100f) && badgeY in 0f..(canvasH + 50f)) {
                                try {
                                    native.drawText(alertBadgeText, badgeX, badgeY, alertBadgePaint)
                                } catch (_: Throwable) {}
                            }
                        }

                        val labelText = if (st.isLocalDevice) "${st.municipality} (Celular Local)" else st.municipality
                        val textX = pt.x + 12.dp.toPx()
                        val textY = pt.y + 4.dp.toPx()
                        if (textX in -50f..(canvasW + 50f) && textY in 0f..(canvasH + 50f)) {
                            try {
                                stationTextPaint.color = if (st.status == "ALERTA") AndroidColor.parseColor("#FF3D71")
                                else if (st.isLocalDevice) AndroidColor.parseColor("#00E5FF")
                                else AndroidColor.WHITE
                                native.drawText(labelText, textX, textY, stationTextPaint)
                            } catch (_: Throwable) {}
                        }
                    }
                }
            }
        }

        if (mode == MapDisplayMode.MONITORING_STATIONS) {
            val alertStation = stations.find { it.status == "ALERTA" }
            if (alertStation != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertRed.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 44.dp, start = 8.dp, end = 58.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "¡ALERTA SÍSMICA EN ESTACIÓN!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${alertStation.name} (${alertStation.municipality}) • ${alertStation.lastVibrationMps2} m/s²",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (onToggleExpand != null) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC091B2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    shadowElevation = 6.dp
                ) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isExpanded) "Reducir Mapa" else "Expandir Mapa",
                            tint = if (isExpanded) BrightCyan else Color.White
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xCC091B2E),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { zoom = (zoom * 1.35f).coerceAtMost(5.5f) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xCC091B2E),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { zoom = (zoom / 1.35f).coerceAtLeast(0.6f) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xCC091B2E),
                border = BorderStroke(1.dp, BrightCyan.copy(alpha = 0.35f)),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        zoom = 1.0f
                        panX = 0f
                        panY = 0f
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centrar Colombia", tint = BrightCyan)
                }
            }
        }

        if (mode == MapDisplayMode.SEISMIC_HISTORY) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xDD071526),
                border = BorderStroke(0.8.dp, BrightCyan.copy(alpha = 0.35f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .padding(end = 56.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color(0xFF06D6A0), modifier = Modifier.size(7.dp)) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🇨🇴 SGC & USGS (${earthquakes.size} sismos)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (mode == MapDisplayMode.SEISMIC_HISTORY && selectedEarthquake != null) {
            val eq = selectedEarthquake
            val badgeColor = when {
                eq.magnitude >= 4.5 -> AlertRed
                eq.magnitude >= 3.5 -> AlertAmber
                else -> BrightCyan
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xEB081A2C),
                border = BorderStroke(1.2.dp, badgeColor.copy(alpha = 0.75f)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = badgeColor
                            ) {
                                Text(
                                    text = "M ${eq.magnitude}",
                                    color = Color(0xFF050E17),
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = eq.location,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Prof: ${eq.depthKm} km • ${eq.formattedTime}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onSelectEarthquake(eq) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "● En vivo • ${eq.agency}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = BrightCyan,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sgc.gov.co/sismos")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Ver en SGC", fontSize = 10.sp, color = BrightCyan, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    openGoogleMapsNavigation(context, eq.latitude, eq.longitude, "Epicentro SGC: " + eq.location)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (mode == MapDisplayMode.MONITORING_STATIONS && selectedStation != null) {
            val st = selectedStation
            val stColor = when (st.status) {
                "ALERTA" -> AlertRed
                "ACTIVA" -> if (st.isLocalDevice) BrightCyan else AccentCyan
                else -> Color.Gray
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xEB081A2C),
                border = BorderStroke(1.2.dp, stColor.copy(alpha = 0.75f)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(shape = CircleShape, color = stColor, modifier = Modifier.size(10.dp)) {}
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

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = stColor.copy(alpha = 0.18f),
                            border = BorderStroke(0.6.dp, stColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = st.status,
                                color = stColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${st.municipality}, ${st.department} • ${st.deviceModel}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFB0C4DE)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sismógrafo en tiempo real de la estación
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF040E1B),
                        border = BorderStroke(0.8.dp, BrightCyan.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val midY = h / 2f

                                // Grid line
                                drawLine(
                                    color = Color(0x1F00F5D4),
                                    start = Offset(0f, midY),
                                    end = Offset(w, midY),
                                    strokeWidth = 1f
                                )

                                val pga = st.lastVibrationMps2.coerceAtLeast(0.015f)
                                val points = 40
                                val stepX = w / (points - 1)
                                val path = Path()
                                val time = System.currentTimeMillis() / 150.0

                                for (i in 0 until points) {
                                    val x = i * stepX
                                    val wave = sin(time + i * 0.45).toFloat() * 0.6f +
                                            cos(time * 1.5 + i * 0.85).toFloat() * 0.4f
                                    val dy = (wave * pga * (h * 0.38f)).coerceIn(-midY * 0.85f, midY * 0.85f)
                                    val y = midY - dy
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = if (st.status == "ALERTA") AlertRed else BrightCyan,
                                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = if (st.status == "ALERTA") AlertRed else LiveGreenGlow, modifier = Modifier.size(5.dp)) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SISMÓGRAFO EN VIVO",
                                    color = BrightCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PGA: ${st.lastVibrationMps2} m/s²",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = if (st.status == "ALERTA") AlertRed else BrightCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${st.sampleRateHz} Hz",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${st.mountType.take(16)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Button(
                            onClick = {
                                openGoogleMapsNavigation(context, st.latitude, st.longitude, "Estación: " + st.name)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (mode == MapDisplayMode.COLLECTION_POINTS && selectedPoint != null) {
            val point = selectedPoint
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xEB081A2C),
                border = BorderStroke(1.2.dp, BrightCyan.copy(alpha = 0.65f)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = point.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${point.address}, ${point.city}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acepta: ${point.acceptedItems.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = BrightCyan,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = {
                                openGoogleMapsNavigation(context, point.latitude, point.longitude, point.name)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                }
            }
        }
    }
}
}

fun openGoogleMapsNavigation(context: Context, lat: Double, lng: Double, label: String) {
    try {
        val encodedLabel = Uri.encode(label)
        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mapIntent)
        return
    } catch (_: Exception) {}

    try {
        val geoUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        val webIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Exception) {}
}

