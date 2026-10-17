package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EcoSismoApp
import com.example.R
import com.example.ui.components.OfflineStatusBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.PrimaryNavy
import kotlinx.coroutines.launch

enum class MainNavigationTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    COLLECTION_POINTS("Puntos Recolección", Icons.Filled.Recycling, Icons.Outlined.Recycling),
    SEISMIC_HISTORY("Historial Sísmico", Icons.Filled.Public, Icons.Outlined.Public),
    MONITORING_STATIONS("Estaciones", Icons.Filled.Sensors, Icons.Outlined.Sensors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    app: EcoSismoApp,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val isOnline by app.syncRepository.isOnline.collectAsState()
    val isSyncing by app.syncRepository.isSyncing.collectAsState()
    var pendingCount by remember { mutableIntStateOf(0) }

    remember(isOnline, isSyncing) {
        scope.launch {
            pendingCount = app.syncRepository.getPendingCount()
        }
    }

    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BrightCyan,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        tint = PrimaryNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ecosismo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Red Móvil Circular de Alerta Temprana",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = BrightCyan
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Información del Reto",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = PrimaryNavy,
                        titleContentColor = Color.White
                    )
                )

                OfflineStatusBar(
                    isOnline = isOnline,
                    isSyncing = isSyncing,
                    pendingCount = pendingCount,
                    onSyncClick = {
                        scope.launch {
                            val report = app.syncRepository.syncPendingData()
                            pendingCount = app.syncRepository.getPendingCount()
                            snackbarHostState.showSnackbar(report.message)
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                MainNavigationTab.entries.forEachIndexed { index, tab ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryNavy,
                            selectedTextColor = AccentCyan,
                            indicatorColor = BrightCyan,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> CollectionPointsScreen(
                    repository = app.collectionRepository,
                    snackbarHostState = snackbarHostState
                )
                1 -> SeismicHistoryScreen(
                    repository = app.seismicRepository,
                    snackbarHostState = snackbarHostState
                )
                2 -> MonitoringStationsScreen(
                    repository = app.stationRepository,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = "Reto 2: Sensores al Servicio de la Comunidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Propuesta: Red Móvil Circular de Alerta Temprana mediante reutilización de teléfonos celulares en desuso en municipios vulnerables de Colombia.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Fase 1 & 2: Puntos de recolección en las alcaldías de 10 ciudades (Armenia, Pereira, Cali, Medellín, Ibagué, Manizales, Bogotá, Bucaramanga, Quibdó y Neiva) coordinadas con Secretarías de Gobierno.\n" +
                                "• Fase 3: Reacondicionamiento de sensores integrados (acelerómetros, giroscopios, magnetómetros).\n" +
                                "• Fase 4: Despliegue de estaciones de monitoreo sísmico con algoritmo STA/LTA y persistencia offline segura en Room.\n\n" +
                                "Aviso importante: Esta solución comunitaria complementa, pero NO reemplaza los sistemas oficiales del Servicio Geológico Colombiano (SGC) y UNGRD.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

