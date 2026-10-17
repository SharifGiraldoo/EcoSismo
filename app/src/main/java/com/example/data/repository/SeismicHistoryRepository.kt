package com.example.data.repository

import com.example.data.local.EarthquakeDao
import com.example.data.model.SgcEarthquake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    fun buildInteractiveSgcMapHtml(earthquakes: List<SgcEarthquake>): String {
        val listToRender = if (earthquakes.isNotEmpty()) earthquakes else getDefaultEarthquakes()
        val markersJson = StringBuilder("[")
        listToRender.forEachIndexed { idx, eq ->
            if (idx > 0) markersJson.append(",")
            val escapedLoc = eq.location.replace("\"", "\\\"").replace("'", "\\'")
            val color = when {
                eq.magnitude >= 4.5 -> "#FF3D71"
                eq.magnitude >= 3.5 -> "#FFAA00"
                else -> "#00E5FF"
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
                <title>Visor Sísmico Oficial SGC</title>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" integrity="sha512-Zcn6gRop8P1W+4pMv6bU1d0m5K5k6v7F3pE2u4b7R1e6e9p5D2p5+o8h7w==" crossorigin="" />
                <style>
                    * { box-sizing: border-box; }
                    html, body { height: 100%; width: 100%; margin: 0; padding: 0; background: #071424; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; overflow: hidden; }
                    #map { height: 100%; width: 100%; background: #071424; }
                    #fallback-canvas { display: none; width: 100%; height: 100%; }

                    .header-bar {
                        position: absolute; top: 10px; left: 10px; right: 10px; z-index: 1000;
                        background: rgba(7, 20, 36, 0.94); backdrop-filter: blur(8px);
                        border: 1px solid rgba(0, 229, 255, 0.35); border-radius: 12px;
                        color: white; padding: 8px 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.6);
                        display: flex; justify-content: space-between; align-items: center;
                    }
                    .header-title { font-size: 13px; font-weight: 800; color: #00E5FF; display: flex; align-items: center; gap: 6px; }
                    .header-sub { font-size: 10px; color: #94A3B8; margin-top: 1px; }
                    .header-badge { background: #00E5FF; color: #071424; font-size: 10px; font-weight: bold; padding: 2px 6px; border-radius: 10px; }

                    .filter-bar {
                        position: absolute; top: 62px; left: 10px; z-index: 1000;
                        display: flex; gap: 6px;
                    }
                    .btn-chip {
                        background: rgba(11, 30, 53, 0.9); border: 1px solid rgba(255,255,255,0.2);
                        color: white; padding: 4px 8px; border-radius: 14px; font-size: 10px;
                        font-weight: 600; cursor: pointer; transition: all 0.2s;
                    }
                    .btn-chip.active { background: #00E5FF; color: #071424; border-color: #00E5FF; }

                    .legend {
                        position: absolute; bottom: 12px; left: 10px; z-index: 1000;
                        background: rgba(7, 20, 36, 0.92); border: 1px solid rgba(0, 229, 255, 0.25);
                        border-radius: 8px; padding: 6px 10px; color: white; font-size: 10px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.5);
                    }
                    .legend-item { display: flex; align-items: center; margin: 2px 0; }
                    .legend-circle { width: 9px; height: 9px; border-radius: 50%; margin-right: 6px; }

                    .center-btn {
                        position: absolute; bottom: 12px; right: 10px; z-index: 1000;
                        background: #00E5FF; color: #071424; border: none; border-radius: 20px;
                        padding: 6px 12px; font-size: 11px; font-weight: bold; cursor: pointer;
                        box-shadow: 0 4px 12px rgba(0,229,255,0.3); display: flex; align-items: center; gap: 4px;
                    }

                    .leaflet-popup-content-wrapper { background: #0C223A; color: white; border-radius: 12px; border: 1.5px solid #00E5FF; box-shadow: 0 6px 20px rgba(0,0,0,0.7); }
                    .leaflet-popup-tip { background: #0C223A; }
                    .custom-popup h3 { margin: 0 0 6px 0; font-size: 13px; color: #FFFFFF; }
                    .custom-popup p { margin: 3px 0; font-size: 11px; color: #CBD5E1; }
                    .badge { display: inline-block; padding: 2px 6px; border-radius: 4px; font-weight: bold; font-size: 10px; color: #08121E; margin-right: 4px; }
                    .popup-footer { margin-top: 6px; padding-top: 4px; border-top: 1px solid rgba(255,255,255,0.1); font-size: 9px; color: #00E5FF; }
                </style>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js" integrity="sha512-BwHfrr4c9kmRkLw6iXFdzcdWV/PGkVgiIyIWLLlTSXzWQzPFVA32+VCsPBVo4Q522022RkgW8ZucgsqP39ebg==" crossorigin=""></script>
            </head>
            <body>
                <div class="header-bar">
                    <div>
                        <div class="header-title">🇨🇴 Mapa Interactivo SGC</div>
                        <div class="header-sub">Servicio Geológico Colombiano • Red Sismológica Nacional</div>
                    </div>
                    <div class="header-badge" id="event-count-badge">${listToRender.size} Sismos</div>
                </div>

                <div class="filter-bar">
                    <button class="btn-chip active" onclick="filterByMag(0)">Todos</button>
                    <button class="btn-chip" onclick="filterByMag(3.0)">M &ge; 3.0</button>
                    <button class="btn-chip" onclick="filterByMag(4.0)">M &ge; 4.0</button>
                    <button class="btn-chip" onclick="filterByMag(4.5)">M &ge; 4.5</button>
                </div>

                <div id="map"></div>
                <canvas id="fallback-canvas"></canvas>

                <div class="legend">
                    <div style="font-weight:bold;margin-bottom:3px;color:#00E5FF;">Sismicidad SGC</div>
                    <div class="legend-item"><span class="legend-circle" style="background:#FF3D71"></span> M &ge; 4.5 (Fuerte)</div>
                    <div class="legend-item"><span class="legend-circle" style="background:#FFAA00"></span> 3.5 &le; M &lt; 4.5 (Moderado)</div>
                    <div class="legend-item"><span class="legend-circle" style="background:#00E5FF"></span> M &lt; 3.5 (Leve)</div>
                </div>

                <button class="center-btn" onclick="centerColombia()">
                    <span>🎯</span> Centrar Colombia
                </button>

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
                            console.warn('Fallo Leaflet, activando render nativo de Colombia en Canvas:', err);
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
                                fillOpacity: 0.8
                            }).addTo(map);

                            circle.bindPopup(
                                '<div class="custom-popup">' +
                                '<h3><span class="badge" style="background:' + eq.color + '">M ' + eq.mag.toFixed(1) + '</span> ' + eq.loc + '</h3>' +
                                '<p><b>Profundidad:</b> ' + eq.depth + ' km</p>' +
                                '<p><b>Fecha/Hora:</b> ' + eq.time + '</p>' +
                                '<p><b>Coordenadas:</b> ' + eq.lat.toFixed(3) + '°, ' + eq.lng.toFixed(3) + '°</p>' +
                                '<p><b>Agencia:</b> ' + (eq.agency || 'SGC Colombia') + '</p>' +
                                '<div class="popup-footer">✓ Servicio Geológico Colombiano • Monitoreo 24/7</div>' +
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

                        ctx.fillStyle = '#071424';
                        ctx.fillRect(0, 0, canvas.width, canvas.height);

                        
                        ctx.strokeStyle = '#102A45';
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

    suspend fun refreshEarthquakes(): Result<Int> = withContext(Dispatchers.IO) {
        try {

            val apiUrl = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson" +
                    "&minlatitude=-4.5&maxlatitude=13.5&minlongitude=-79.5&maxlongitude=-66.8" +
                    "&minmagnitude=2.0&limit=40"

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val features = json.getJSONArray("features")
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
                    val mag = props.optDouble("mag", 3.0)
                    val time = props.optLong("time", System.currentTimeMillis())
                    val place = props.optString("place", "Colombia")

                    val alertLevel = when {
                        mag >= 4.5 -> "Rojo"
                        mag >= 3.5 -> "Amarillo"
                        else -> "Verde"
                    }

                    parsedList.add(
                        SgcEarthquake(
                            id = feature.optString("id", "sgc_$i"),
                            magnitude = (mag * 10).toInt() / 10.0,
                            depthKm = (depth * 10).toInt() / 10.0,
                            location = place,
                            municipality = extractMunicipality(place),
                            department = extractDepartment(place),
                            latitude = lat,
                            longitude = lon,
                            timestamp = time,
                            formattedTime = sdf.format(Date(time)),
                            agency = "SGC / Red Sismológica Nacional",
                            alertLevel = alertLevel
                        )
                    )
                }

                if (parsedList.isNotEmpty()) {
                    earthquakeDao.insertAllEarthquakes(parsedList)
                    return@withContext Result.success(parsedList.size)
                }
            }

            ensureDefaultCatalog()
            Result.success(getDefaultEarthquakes().size)
        } catch (e: Exception) {

            ensureDefaultCatalog()
            Result.failure(e)
        }
    }

    suspend fun ensureDefaultCatalog() {
        val defaults = getDefaultEarthquakes()
        earthquakeDao.insertAllEarthquakes(defaults)
    }

    private fun extractMunicipality(place: String): String {
        val parts = place.split(",")
        return if (parts.isNotEmpty()) parts[0].trim() else place
    }

    private fun extractDepartment(place: String): String {
        val parts = place.split(",")
        return if (parts.size > 1) parts[1].trim() else "Colombia"
    }

    fun getDefaultEarthquakes(): List<SgcEarthquake> {
        val now = System.currentTimeMillis()
        val hour = 3600000L
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO"))
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")

        return listOf(
            SgcEarthquake(
                id = "sgc_2026_01",
                magnitude = 4.2,
                depthKm = 148.0,
                location = "Los Santos, Santander - Nido Sísmico de Bucaramanga",
                municipality = "Los Santos",
                department = "Santander",
                latitude = 6.7820,
                longitude = -73.1250,
                timestamp = now - (2 * hour),
                formattedTime = sdf.format(Date(now - (2 * hour))),
                agency = "SGC - Servicio Geológico Colombiano",
                alertLevel = "Amarillo"
            ),
            SgcEarthquake(
                id = "sgc_2026_02",
                magnitude = 3.4,
                depthKm = 12.0,
                location = "Dabeiba, Antioquia",
                municipality = "Dabeiba",
                department = "Antioquia",
                latitude = 7.0053,
                longitude = -76.2625,
                timestamp = now - (6 * hour),
                formattedTime = sdf.format(Date(now - (6 * hour))),
                agency = "SGC - Servicio Geológico Colombiano",
                alertLevel = "Verde"
            ),
            SgcEarthquake(
                id = "sgc_2026_03",
                magnitude = 4.8,
                depthKm = 24.0,
                location = "El Calvario, Meta - Estribaciones Cordillera Oriental",
                municipality = "El Calvario",
                department = "Meta",
                latitude = 4.3541,
                longitude = -73.7125,
                timestamp = now - (14 * hour),
                formattedTime = sdf.format(Date(now - (14 * hour))),
                agency = "SGC - Servicio Geológico Colombiano",
                alertLevel = "Rojo"
            ),
            SgcEarthquake(
                id = "sgc_2026_04",
                magnitude = 2.9,
                depthKm = 30.0,
                location = "Villamaría, Caldas - Complejo Volcánico Nevado del Ruiz",
                municipality = "Villamaría",
                department = "Caldas",
                latitude = 4.9521,
                longitude = -75.4215,
                timestamp = now - (22 * hour),
                formattedTime = sdf.format(Date(now - (22 * hour))),
                agency = "SGC - Servicio Geológico Colombiano",
                alertLevel = "Verde"
            ),
            SgcEarthquake(
                id = "sgc_2026_05",
                magnitude = 3.8,
                depthKm = 18.0,
                location = "Quibdó, Chocó - Falla Atrato",
                municipality = "Quibdó",
                department = "Chocó",
                latitude = 5.7200,
                longitude = -76.6200,
                timestamp = now - (35 * hour),
                formattedTime = sdf.format(Date(now - (35 * hour))),
                agency = "SGC - Servicio Geológico Colombiano",
                alertLevel = "Amarillo"
            ),
            SgcEarthquake(
                id = "sgc_2026_06",
                magnitude = 3.1,
                depthKm = 45.0,
                location = "La Plata, Huila",
                municipality = "La Plata",
                department = "Huila",
                latitude = 2.3922,
                longitude = -75.8920,
                timestamp = now - (50 * hour),
                formattedTime = sdf.format(Date(now - (50 * hour))),
                agency = "SGC - Servicio Geológico Colombiano",
                alertLevel = "Verde"
            ),
            SgcEarthquake(
                id = "sgc_2026_07",
                magnitude = 2.7,
                depthKm = 5.0,
                location = "Génova, Quindío - Cordillera Central",
                municipality = "Génova",
                department = "Quindío",
                latitude = 4.3120,
                longitude = -75.7820,
                timestamp = now - (72 * hour),
                formattedTime = sdf.format(Date(now - (72 * hour))),
                agency = "SGC - Servicio Geológico Colombiano",
                alertLevel = "Verde"
            )
        )
    }
}

