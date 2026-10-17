package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.SgcEarthquake
import com.example.data.repository.SeismicHistoryRepository
import com.example.ui.components.ColombiaMapView
import com.example.ui.components.MapDisplayMode
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightCyan
import kotlinx.coroutines.launch

enum class SgcViewerMode {
    INTERACTIVE_SGC,
    PORTAL_WEB,
    ARCGIS_SGC
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SeismicHistoryScreen(
    repository: SeismicHistoryRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }

    val earthquakes by repository.earthquakes.collectAsState(initial = emptyList())
    var selectedEarthquake by remember { mutableStateOf<SgcEarthquake?>(null) }

    var minMagFilter by remember { mutableDoubleStateOf(0.0) }
    var depthFilter by remember { mutableStateOf("TODAS") }

    val filteredList = remember(earthquakes, minMagFilter, depthFilter) {
        earthquakes.filter { eq ->
            val matchMag = eq.magnitude >= minMagFilter
            val matchDepth = when (depthFilter) {
                "SUPERFICIAL" -> eq.depthKm <= 30.0
                "INTERMEDIA" -> eq.depthKm > 30.0 && eq.depthKm <= 70.0
                "PROFUNDA" -> eq.depthKm > 70.0
                else -> true
            }
            matchMag && matchDepth
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HISTORIAL SÍSMICO DE COLOMBIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Fuente oficial: Servicio Geológico Colombiano (SGC)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            val result = repository.refreshEarthquakes()
                            isRefreshing = false
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("Datos sísmicos actualizados (${result.getOrNull()} eventos)")
                            } else {
                                snackbarHostState.showSnackbar("Sin conexión a API; mostrando catálogo local del SGC")
                            }
                        }
                    }
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar catálogo", tint = BrightCyan)
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrightCyan
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Mapa Sísmico (${filteredList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Visor Oficial SGC", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (selectedTab == 0) {

            if (!isMapExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)

                    FilterChip(
                        selected = minMagFilter == 0.0,
                        onClick = { minMagFilter = 0.0 },
                        label = { Text("Todas M", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = minMagFilter == 3.0,
                        onClick = { minMagFilter = 3.0 },
                        label = { Text("M ≥ 3.0", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = minMagFilter == 4.0,
                        onClick = { minMagFilter = 4.0 },
                        label = { Text("M ≥ 4.0", fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    FilterChip(
                        selected = depthFilter == "TODAS",
                        onClick = { depthFilter = "TODAS" },
                        label = { Text("Todas Prof.", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = depthFilter == "SUPERFICIAL",
                        onClick = { depthFilter = "SUPERFICIAL" },
                        label = { Text("Superficial (<30km)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = depthFilter == "INTERMEDIA",
                        onClick = { depthFilter = "INTERMEDIA" },
                        label = { Text("Intermedia (30-70km)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = depthFilter == "PROFUNDA",
                        onClick = { depthFilter = "PROFUNDA" },
                        label = { Text("Profunda (>70km)", fontSize = 11.sp) }
                    )
                }
            }

            Box(
                modifier = if (isMapExpanded) Modifier.fillMaxSize()
                else Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
            ) {
                ColombiaMapView(
                    mode = MapDisplayMode.SEISMIC_HISTORY,
                    earthquakes = filteredList,
                    selectedEarthquake = selectedEarthquake,
                    onSelectEarthquake = { selectedEarthquake = it },
                    isExpanded = isMapExpanded,
                    onToggleExpand = { isMapExpanded = !isMapExpanded }
                )
            }

            if (!isMapExpanded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.95f)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { eq ->
                        val isSelected = selectedEarthquake?.id == eq.id
                        val badgeColor = when {
                            eq.magnitude >= 4.5 -> AlertRed
                            eq.magnitude >= 3.5 -> AlertAmber
                            else -> BrightCyan
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEarthquake = eq },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 2.dp, brush = androidx.compose.ui.graphics.SolidColor(badgeColor))
                            else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${eq.magnitude}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF071424)
                                        )
                                        Text(
                                            text = "Mag",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF071424)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = eq.location,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Profundidad: ${eq.depthKm} km • ${eq.formattedTime}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Coords: ${"%.3f".format(eq.latitude)}, ${"%.3f".format(eq.longitude)} • ${eq.agency}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {

            var viewerMode by remember { mutableStateOf(SgcViewerMode.INTERACTIVE_SGC) }
            var webViewInstance by remember { mutableStateOf<WebView?>(null) }
            var webViewLoading by remember { mutableStateOf(false) }
            var webViewProgress by remember { mutableIntStateOf(0) }
            var cloudFrontBlocked by remember { mutableStateOf(false) }

            fun loadViewerContent(view: WebView, mode: SgcViewerMode) {
                cloudFrontBlocked = false
                when (mode) {
                    SgcViewerMode.INTERACTIVE_SGC -> {
                        val html = repository.buildInteractiveSgcMapHtml(filteredList)
                        view.loadDataWithBaseURL("https://www.sgc.gov.co/", html, "text/html", "UTF-8", null)
                    }
                    SgcViewerMode.PORTAL_WEB -> {
                        view.loadUrl(repository.officialSgcViewerUrl)
                    }
                    SgcViewerMode.ARCGIS_SGC -> {
                        view.loadUrl(repository.alternateSgcArcGisUrl)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = Color(0xFF0A1E35),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Visor Sísmico Oficial del SGC",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    val targetUrl = when (viewerMode) {
                                        SgcViewerMode.INTERACTIVE_SGC -> "https://www.sgc.gov.co/sismos"
                                        SgcViewerMode.PORTAL_WEB -> "https://www.sgc.gov.co/sismos"
                                        SgcViewerMode.ARCGIS_SGC -> repository.alternateSgcArcGisUrl
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    Icons.Default.OpenInBrowser,
                                    contentDescription = "Abrir en navegador Chrome",
                                    tint = BrightCyan
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = viewerMode == SgcViewerMode.INTERACTIVE_SGC,
                                onClick = {
                                    viewerMode = SgcViewerMode.INTERACTIVE_SGC
                                    webViewInstance?.let { loadViewerContent(it, SgcViewerMode.INTERACTIVE_SGC) }
                                },
                                label = { Text("Mapa Interactivo SGC", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrightCyan,
                                    selectedLabelColor = Color(0xFF071424)
                                )
                            )
                            FilterChip(
                                selected = viewerMode == SgcViewerMode.PORTAL_WEB,
                                onClick = {
                                    viewerMode = SgcViewerMode.PORTAL_WEB
                                    webViewInstance?.let { loadViewerContent(it, SgcViewerMode.PORTAL_WEB) }
                                },
                                label = { Text("Portal sgc.gov.co", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrightCyan,
                                    selectedLabelColor = Color(0xFF071424)
                                )
                            )
                            FilterChip(
                                selected = viewerMode == SgcViewerMode.ARCGIS_SGC,
                                onClick = {
                                    viewerMode = SgcViewerMode.ARCGIS_SGC
                                    webViewInstance?.let { loadViewerContent(it, SgcViewerMode.ARCGIS_SGC) }
                                },
                                label = { Text("Visor ArcGIS SGC", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrightCyan,
                                    selectedLabelColor = Color(0xFF071424)
                                )
                            )
                            IconButton(
                                onClick = {
                                    webViewInstance?.let { loadViewerContent(it, viewerMode) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (cloudFrontBlocked && viewerMode == SgcViewerMode.PORTAL_WEB) {
                    Surface(
                        color = AlertAmber.copy(alpha = 0.95f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF071424), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "El portal externo sgc.gov.co restringe accesos automáticos. Usa 'Mapa Interactivo SGC' o abre en Chrome.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color(0xFF071424),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = {
                                    viewerMode = SgcViewerMode.INTERACTIVE_SGC
                                    webViewInstance?.let { loadViewerContent(it, SgcViewerMode.INTERACTIVE_SGC) }
                                }
                            ) {
                                Text("Ver Mapa", color = Color(0xFF071424), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewInstance = this
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    allowContentAccess = true
                                    allowFileAccess = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        webViewProgress = newProgress
                                        if (newProgress >= 90) {
                                            webViewLoading = false
                                        }
                                    }

                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        return true
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        webViewLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        webViewLoading = false
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?
                                    ) {
                                        if (request?.isForMainFrame == true && errorResponse?.statusCode != null && errorResponse.statusCode >= 400) {
                                            webViewLoading = false
                                            if (errorResponse.statusCode == 403) {
                                                cloudFrontBlocked = true
                                            }
                                        }
                                    }
                                }

                                loadViewerContent(this, viewerMode)
                            }
                        },
                        update = { view ->
                            webViewInstance = view
                        }
                    )

                    if (webViewLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BrightCyan)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cargando visor SGC... ($webViewProgress%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

