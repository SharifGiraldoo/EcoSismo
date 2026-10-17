package com.example.ui.screens

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EcoSismoApp
import com.example.ui.components.OfflineStatusBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.LiveGreenGlow
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
    val firebaseSync = app.firebaseSyncRepository
    val currentUser by firebaseSync.currentUser.collectAsStateWithLifecycle()
    var showFirebaseDialog by remember { mutableStateOf(false) }

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
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF050E17),
                        Color(0xFF081728),
                        Color(0xFF0C1F35)
                    )
                )
            ),
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                color = Color(0xCC050E1A),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrightCyan,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Vibration,
                                            contentDescription = null,
                                            tint = PrimaryNavy,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "Ecosismo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
                            IconButton(onClick = { showFirebaseDialog = true }) {
                                Icon(
                                    imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                    contentDescription = "Firebase Firestore Cloud",
                                    tint = if (currentUser != null) com.example.ui.theme.LiveGreenGlow else BrightCyan
                                )
                            }
                            IconButton(onClick = { showAboutDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Información del Reto",
                                    tint = BrightCyan.copy(alpha = 0.85f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
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
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xEB071728),
                border = BorderStroke(1.dp, BrightCyan.copy(alpha = 0.22f)),
                shadowElevation = 12.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
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
                                selectedIconColor = Color(0xFF040D17),
                                selectedTextColor = BrightCyan,
                                indicatorColor = BrightCyan,
                                unselectedIconColor = Color(0xFF8899A6),
                                unselectedTextColor = Color(0xFF8899A6)
                            )
                        )
                    }
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
            containerColor = Color(0xF2081A2C),
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(Icons.Default.Vibration, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(34.dp))
            },
            title = {
                Text(
                    text = "Reto 2: Sensores al Servicio de la Comunidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Propuesta: Red Móvil Circular de Alerta Temprana mediante reutilización de teléfonos celulares en desuso en municipios vulnerables de Colombia.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "• Telemetría en tiempo real obtenida del Servicio Geológico Colombiano (SGC), USGS, EMSC, FDSN y sensores físicos del teléfono.\n" +
                                "• Sin datos simulados: todas las lecturas de aceleración, coordenadas y catálogo sísmico reflejan eventos y sensores reales.\n" +
                                "• Puntos de recolección en alcaldías y Secretarías de Gobierno para economía circular y reacondicionamiento comunal.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, color = BrightCyan)
                }
            }
        )
    }

    if (showFirebaseDialog) {
        AlertDialog(
            onDismissRequest = { showFirebaseDialog = false },
            containerColor = Color(0xF2081A2C),
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = if (currentUser != null) com.example.ui.theme.LiveGreenGlow else BrightCyan,
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text(
                    text = "Firebase Firestore Cloud",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x66020B16),
                        border = BorderStroke(0.6.dp, Color(0x3300F5D4))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Base de Datos Activa:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("gen-lang-client-0618940609", fontSize = 11.sp, color = BrightCyan, fontWeight = FontWeight.Bold)
                            Text("Colecciones: /monitoring_stations, /users, /seismic_logs", fontSize = 9.5.sp, color = Color(0xFFCBD5E1))
                        }
                    }

                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2600F5D4),
                            border = BorderStroke(0.8.dp, BrightCyan)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Usuario: ${currentUser?.displayName ?: "Usuario EcoSismo"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Email: ${currentUser?.email ?: ""}", fontSize = 11.sp, color = BrightCyan)
                                Text("UID: ${currentUser?.uid?.take(14)}...", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("✓ Conexión en la nube establecida y datos guardados", fontSize = 11.sp, color = com.example.ui.theme.LiveGreenGlow, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                firebaseSync.signOut()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Sesión en la nube cerrada.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x40E63946), contentColor = Color(0xFFFF8585)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cerrar Sesión")
                        }
                    } else {
                        Text(
                            text = "Inicia sesión con Google para sincronizar tus estaciones celulares y reportes sísmicos en tiempo real en la nube de Firebase Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    val result = firebaseSync.signInWithGoogle()
                                    result.onSuccess { user ->
                                        showFirebaseDialog = false
                                        snackbarHostState.showSnackbar("Conectado con ${user.displayName ?: user.email} en Firebase.")
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar("Google Sign-In: ${err.message ?: "Cancelado"}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = Color(0xFF071424)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Iniciar Sesión con Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFirebaseDialog = false }) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, color = BrightCyan)
                }
            }
        )
    }
}
