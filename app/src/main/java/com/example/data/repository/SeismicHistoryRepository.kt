package com.example.data.repository

import com.example.data.local.EarthquakeDao
import com.example.data.model.SgcEarthquake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SeismicHistoryRepository(
    private val earthquakeDao: EarthquakeDao
) {
    val earthquakes: Flow<List<SgcEarthquake>> = earthquakeDao.getAllEarthquakes()

    val officialSgcViewerUrl = "https://www.sgc.gov.co/sismos"
    val alternateSgcArcGisUrl = "https://www.arcgis.com/apps/mapviewer/index.html?webmap=4449ff089b2140f096052733d3c8a91f"
    val officialSgcPortalName = "Servicio Geológico Colombiano (SGC) - Red Sismológica Nacional"

    private val _isLiveSyncing = MutableStateFlow(false)
    val isLiveSyncing: StateFlow<Boolean> = _isLiveSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _liveDataSource = MutableStateFlow("API Abierta USGS & EMSC")
    val liveDataSource: StateFlow<String> = _liveDataSource.asStateFlow()

    private val _liveStatusMessage = MutableStateFlow("Monitoreo en Tiempo Real Activo")
    val liveStatusMessage: StateFlow<String> = _liveStatusMessage.asStateFlow()

    fun start247LiveMonitoring(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    refreshEarthquakes()
                } catch (_: Exception) {}
                delay(35000L)
            }
        }
    }

    suspend fun refreshEarthquakes(): Result<Int> = withContext(Dispatchers.IO) {
        _isLiveSyncing.value = true
        try {
            val usgsResult = fetchFromUsgs()
            if (usgsResult.isNotEmpty()) {
                earthquakeDao.insertAllEarthquakes(usgsResult)
                _lastSyncTimestamp.value = System.currentTimeMillis()
                _liveDataSource.value = "USGS FDSNWS GeoJSON (En Vivo)"
                _liveStatusMessage.value = "${usgsResult.size} sismos recibidos en tiempo real"
                _isLiveSyncing.value = false
                return@withContext Result.success(usgsResult.size)
            }

            val emscResult = fetchFromEmsc()
            if (emscResult.isNotEmpty()) {
                earthquakeDao.insertAllEarthquakes(emscResult)
                _lastSyncTimestamp.value = System.currentTimeMillis()
                _liveDataSource.value = "EMSC CSEM Open Source (En Vivo)"
                _liveStatusMessage.value = "${emscResult.size} sismos recibidos (EMSC)"
                _isLiveSyncing.value = false
                return@withContext Result.success(emscResult.size)
            }

            _isLiveSyncing.value = false
            _liveStatusMessage.value = "Esperando nueva actividad sísmica en Colombia"
            Result.success(0)
        } catch (e: Exception) {
            _isLiveSyncing.value = false
            _liveStatusMessage.value = "Sin conexión temporal. Reintentando..."
            Result.failure(e)
        }
    }

    private fun fetchFromUsgs(): List<SgcEarthquake> {
        val apiUrl = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson" +
                "&minlatitude=-4.5&maxlatitude=13.5&minlongitude=-79.5&maxlongitude=-66.8" +
                "&minmagnitude=1.0&limit=60&orderby=time"

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 7000
        connection.readTimeout = 7000
        connection.setRequestProperty("User-Agent", "EcoSismo-Colombia/2.0")
        connection.requestMethod = "GET"

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return emptyList()
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()

        val json = JSONObject(response)
        val features = json.optJSONArray("features") ?: return emptyList()
        val parsedList = mutableListOf<SgcEarthquake>()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CO"))
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val props = feature.getJSONObject("properties")
            val geom = feature.getJSONObject("geometry")
            val coords = geom.getJSONArray("coordinates")

            val lon = coords.getDouble(0)
            val lat = coords.getDouble(1)
            val depth = if (coords.length() > 2) coords.getDouble(2) else 10.0
            val mag = props.optDouble("mag", 0.0)
            if (mag <= 0.0) continue

            val time = props.optLong("time", System.currentTimeMillis())
            val place = props.optString("place", "Colombia")

            val alertLevel = when {
                mag >= 4.5 -> "Rojo"
                mag >= 3.5 -> "Amarillo"
                else -> "Verde"
            }

            parsedList.add(
                SgcEarthquake(
                    id = feature.optString("id", "usgs_$i"),
                    magnitude = (mag * 10).toInt() / 10.0,
                    depthKm = (depth * 10).toInt() / 10.0,
                    location = place,
                    municipality = extractMunicipality(place),
                    department = extractDepartment(place),
                    latitude = lat,
                    longitude = lon,
                    timestamp = time,
                    formattedTime = sdf.format(Date(time)),
                    agency = "USGS / SGC Red Sismológica",
                    alertLevel = alertLevel
                )
            )
        }
        return parsedList
    }

    private fun fetchFromEmsc(): List<SgcEarthquake> {
        val apiUrl = "https://www.seismicportal.eu/fdsnws/event/1/query?format=json" +
                "&minlat=-4.5&maxlat=13.5&minlon=-79.5&maxlon=-66.8&limit=60"

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 7000
        connection.readTimeout = 7000
        connection.setRequestProperty("User-Agent", "EcoSismo-Colombia/2.0")
        connection.requestMethod = "GET"

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return emptyList()
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()

        val json = JSONObject(response)
        val features = json.optJSONArray("features") ?: return emptyList()
        val parsedList = mutableListOf<SgcEarthquake>()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CO"))
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val props = feature.getJSONObject("properties")
            val geom = feature.getJSONObject("geometry")
            val coords = geom.getJSONArray("coordinates")

            val lon = coords.getDouble(0)
            val lat = coords.getDouble(1)
            val depth = if (coords.length() > 2) coords.getDouble(2) else 10.0
            val mag = props.optDouble("mag", 0.0)
            if (mag <= 0.0) continue

            val timeIso = props.optString("time", "")
            val timeMs = try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                isoFormat.parse(timeIso)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val place = props.optString("flynn_region", "Colombia")

            val alertLevel = when {
                mag >= 4.5 -> "Rojo"
                mag >= 3.5 -> "Amarillo"
                else -> "Verde"
            }

            parsedList.add(
                SgcEarthquake(
                    id = feature.optString("id", "emsc_$i"),
                    magnitude = (mag * 10).toInt() / 10.0,
                    depthKm = (depth * 10).toInt() / 10.0,
                    location = place,
                    municipality = extractMunicipality(place),
                    department = extractDepartment(place),
                    latitude = lat,
                    longitude = lon,
                    timestamp = timeMs,
                    formattedTime = sdf.format(Date(timeMs)),
                    agency = "EMSC / Red Internacional SGC",
                    alertLevel = alertLevel
                )
            )
        }
        return parsedList
    }

    private fun extractMunicipality(place: String): String {
        val parts = place.split(",")
        return if (parts.isNotEmpty()) parts[0].trim() else place
    }

    private fun extractDepartment(place: String): String {
        val parts = place.split(",")
        return if (parts.size > 1) parts[1].trim() else "Colombia"
    }

    fun buildInteractiveSgcMapHtml(earthquakes: List<SgcEarthquake>): String {
        val markersJson = StringBuilder("[")
        earthquakes.forEachIndexed { idx, eq ->
            if (idx > 0) markersJson.append(",")
            val escapedLoc = eq.location.replace("\"", "\\\"").replace("'", "\\'")
            val color = when {
                eq.magnitude >= 4.5 -> "#FF3366"
                eq.magnitude >= 3.5 -> "#FFB703"
                else -> "#00F5D4"
            }
            markersJson.append("""{"lat":${eq.latitude},"lng":${eq.longitude},"mag":${eq.magnitude},"loc":"$escapedLoc","depth":${eq.depthKm},"time":"${eq.formattedTime}","agency":"${eq.agency}","color":"$color"}""")
        }
        markersJson.append("]")

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Visor Sísmico Nacional</title>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" crossorigin="" />
                <style>
                    * { box-sizing: border-box; }
                    html, body { height: 100%; width: 100%; margin: 0; padding: 0; background: #050E17; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; overflow: hidden; }
                    #map { height: 100%; width: 100%; background: #050E17; }
                    #fallback-canvas { display: none; width: 100%; height: 100%; }

                    .glass-bar {
                        position: absolute; top: 10px; left: 10px; right: 10px; z-index: 1000;
                        background: rgba(9, 24, 41, 0.85); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
                        border: 1px solid rgba(0, 245, 212, 0.3); border-radius: 16px;
                        color: white; padding: 8px 14px; box-shadow: 0 8px 24px rgba(0,0,0,0.5);
                        display: flex; justify-content: space-between; align-items: center;
                    }
                    .glass-title { font-size: 13px; font-weight: 800; color: #00F5D4; display: flex; align-items: center; gap: 8px; letter-spacing: 0.3px; }
                    .live-indicator { width: 8px; height: 8px; border-radius: 50%; background: #06D6A0; box-shadow: 0 0 10px #06D6A0; animation: pulse 1.8s infinite; }
                    @keyframes pulse { 0% { transform: scale(0.9); opacity: 0.8; } 50% { transform: scale(1.3); opacity: 1; } 100% { transform: scale(0.9); opacity: 0.8; } }
                    .glass-sub { font-size: 10px; color: #94A3B8; margin-top: 1px; }
                    .glass-badge { background: rgba(0, 245, 212, 0.18); border: 1px solid #00F5D4; color: #00F5D4; font-size: 10px; font-weight: 700; padding: 3px 8px; border-radius: 12px; }

                    .filter-bar {
                        position: absolute; top: 64px; left: 10px; z-index: 1000;
                        display: flex; gap: 6px;
                    }
                    .btn-chip {
                        background: rgba(13, 35, 56, 0.82); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
                        border: 1px solid rgba(255,255,255,0.15);
                        color: #E2E8F0; padding: 5px 10px; border-radius: 14px; font-size: 11px;
                        font-weight: 600; cursor: pointer; transition: all 0.2s;
                    }
                    .btn-chip.active { background: #00F5D4; color: #050E17; border-color: #00F5D4; box-shadow: 0 0 12px rgba(0,245,212,0.4); font-weight: 700; }

                    .legend {
                        position: absolute; bottom: 12px; left: 10px; z-index: 1000;
                        background: rgba(9, 24, 41, 0.88); backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px);
                        border: 1px solid rgba(0, 245, 212, 0.22);
                        border-radius: 12px; padding: 7px 12px; color: white; font-size: 10px;
                        box-shadow: 0 6px 16px rgba(0,0,0,0.6);
                    }
                    .legend-item { display: flex; align-items: center; margin: 3px 0; }
                    .legend-circle { width: 9px; height: 9px; border-radius: 50%; margin-right: 6px; }

                    .center-btn {
                        position: absolute; bottom: 12px; right: 10px; z-index: 1000;
                        background: #00F5D4; color: #050E17; border: none; border-radius: 20px;
                        padding: 7px 14px; font-size: 11px; font-weight: 800; cursor: pointer;
                        box-shadow: 0 4px 16px rgba(0,245,212,0.4); display: flex; align-items: center; gap: 5px;
                    }

                    .leaflet-popup-content-wrapper { background: rgba(10, 28, 48, 0.95); backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px); color: white; border-radius: 16px; border: 1.5px solid #00F5D4; box-shadow: 0 8px 30px rgba(0,0,0,0.7); }
                    .leaflet-popup-tip { background: rgba(10, 28, 48, 0.95); }
                    .custom-popup h3 { margin: 0 0 6px 0; font-size: 13px; color: #FFFFFF; font-weight: 700; }
                    .custom-popup p { margin: 3px 0; font-size: 11px; color: #CBD5E1; }
                    .badge { display: inline-block; padding: 2px 7px; border-radius: 6px; font-weight: 800; font-size: 10px; color: #050E17; margin-right: 4px; }
                    .popup-footer { margin-top: 8px; padding-top: 5px; border-top: 1px solid rgba(255,255,255,0.12); font-size: 9px; color: #00F5D4; font-weight: 600; }
                </style>
            </head>
            <body>
                <div class="glass-bar">
                    <div>
                        <div class="glass-title"><span class="live-indicator"></span> En Vivo • SGC & USGS</div>
                        <div class="glass-sub">Red Sismológica Nacional en Tiempo Real</div>
                    </div>
                    <div class="glass-badge" id="event-count-badge">${earthquakes.size} Sismos</div>
                </div>

                <div class="filter-bar">
                    <button class="btn-chip active" onclick="filterByMag(0)">Todos</button>
                    <button class="btn-chip" onclick="filterByMag(3.0)">M 3.0+</button>
                    <button class="btn-chip" onclick="filterByMag(4.0)">M 4.0+</button>
                    <button class="btn-chip" onclick="filterByMag(4.5)">M 4.5+</button>
                </div>

                <div class="legend">
                    <div class="legend-item"><span class="legend-circle" style="background:#00F5D4"></span> M &lt; 3.5 (Leve)</div>
                    <div class="legend-item"><span class="legend-circle" style="background:#FFB703"></span> M 3.5 - 4.4 (Moderado)</div>
                    <div class="legend-item"><span class="legend-circle" style="background:#FF3366"></span> M &gt;= 4.5 (Fuerte)</div>
                </div>

                <button class="center-btn" onclick="centerColombia()">🇨🇴 Centrar</button>

                <div id="map"></div>
                <canvas id="fallback-canvas"></canvas>

                <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js" crossorigin=""></script>
                <script>
                    var earthquakes = $markersJson;
                    var map = null;
                    var markerLayers = [];
                    var currentMinMag = 0;

                    function initLeafletMap() {
                        try {
                            if (typeof L === 'undefined') {
                                throw new Error('Leaflet no cargado');
                            }
                            map = L.map('map', { zoomControl: false }).setView([4.5709, -74.2973], 6);
                            L.control.zoom({ position: 'topright' }).addTo(map);

                            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                                attribution: '&copy; SGC &copy; OpenStreetMap',
                                maxZoom: 18,
                                subdomains: 'abcd',
                                errorTileUrl: 'https://tile.openstreetmap.org/6/19/30.png'
                            }).addTo(map);

                            renderMarkers(0);
                        } catch (err) {
                            document.getElementById('map').style.display = 'none';
                            renderCanvasFallback();
                        }
                    }

                    function renderMarkers(minMag) {
                        if (!map) return;
                        markerLayers.forEach(function(l) { map.removeLayer(l); });
                        markerLayers = [];

                        var filtered = earthquakes.filter(function(eq) { return eq.mag >= minMag; });
                        document.getElementById('event-count-badge').innerText = filtered.length + ' Sismos';

                        filtered.forEach(function(eq) {
                            var radius = Math.max(eq.mag * 3.8, 8);
                            var circle = L.circleMarker([eq.lat, eq.lng], {
                                radius: radius,
                                fillColor: eq.color,
                                color: '#FFFFFF',
                                weight: 1.5,
                                opacity: 0.95,
                                fillOpacity: 0.82
                            }).addTo(map);

                            circle.bindPopup(
                                '<div class="custom-popup">' +
                                '<h3><span class="badge" style="background:' + eq.color + '">M ' + eq.mag.toFixed(1) + '</span> ' + eq.loc + '</h3>' +
                                '<p><b>Profundidad:</b> ' + eq.depth + ' km</p>' +
                                '<p><b>Fecha/Hora:</b> ' + eq.time + '</p>' +
                                '<p><b>Coordenadas:</b> ' + eq.lat.toFixed(3) + '°, ' + eq.lng.toFixed(3) + '°</p>' +
                                '<p><b>Fuente:</b> ' + (eq.agency || 'Red Sismológica') + '</p>' +
                                '<div class="popup-footer">✓ Servicio Geológico Colombiano • Tiempo Real</div>' +
                                '</div>'
                            );
                            markerLayers.push(circle);
                        });
                    }

                    function filterByMag(minMag) {
                        currentMinMag = minMag;
                        var buttons = document.querySelectorAll('.filter-bar .btn-chip');
                        buttons.forEach(function(b) {
                            b.classList.remove('active');
                            if (minMag === 0 && b.innerText === 'Todos') b.classList.add('active');
                            if (minMag === 3.0 && b.innerText.includes('3.0')) b.classList.add('active');
                            if (minMag === 4.0 && b.innerText.includes('4.0')) b.classList.add('active');
                            if (minMag === 4.5 && b.innerText.includes('4.5')) b.classList.add('active');
                        });
                        if (map) {
                            renderMarkers(minMag);
                        } else {
                            renderCanvasFallback();
                        }
                    }

                    function centerColombia() {
                        if (map) {
                            map.setView([4.5709, -74.2973], 6);
                        } else {
                            renderCanvasFallback();
                        }
                    }

                    function renderCanvasFallback() {
                        var canvas = document.getElementById('fallback-canvas');
                        canvas.style.display = 'block';
                        var ctx = canvas.getContext('2d');
                        canvas.width = window.innerWidth;
                        canvas.height = window.innerHeight;

                        ctx.fillStyle = '#050E17';
                        ctx.fillRect(0, 0, canvas.width, canvas.height);

                        ctx.strokeStyle = '#0E2238';
                        ctx.lineWidth = 1;
                        for (var x = 0; x < canvas.width; x += 40) {
                            ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
                        }
                        for (var y = 0; y < canvas.height; y += 40) {
                            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
                        }

                        var minLon = -79.5, maxLon = -66.8, minLat = -4.5, maxLat = 13.0;
                        function project(lat, lon) {
                            var nx = (lon - minLon) / (maxLon - minLon);
                            var ny = 1.0 - (lat - minLat) / (maxLat - minLat);
                            var w = canvas.width * 0.85;
                            var h = canvas.height * 0.82;
                            var ox = (canvas.width - w) / 2;
                            var oy = (canvas.height - h) / 2;
                            return { x: ox + nx * w, y: oy + ny * h };
                        }

                        var filtered = earthquakes.filter(function(eq) { return eq.mag >= currentMinMag; });
                        filtered.forEach(function(eq) {
                            var pt = project(eq.lat, eq.lng);
                            ctx.beginPath();
                            ctx.arc(pt.x, pt.y, Math.max(eq.mag * 3.5, 7), 0, Math.PI * 2);
                            ctx.fillStyle = eq.color;
                            ctx.fill();
                            ctx.lineWidth = 1.5;
                            ctx.strokeStyle = '#FFFFFF';
                            ctx.stroke();
                            ctx.fillStyle = '#FFFFFF';
                            ctx.font = 'bold 10px sans-serif';
                            ctx.fillText('M' + eq.mag.toFixed(1), pt.x + 9, pt.y + 3);
                        });
                    }

                    window.onload = function() {
                        initLeafletMap();
                    };
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
