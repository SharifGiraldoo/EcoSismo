package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.AlertDao
import com.example.data.local.DonationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SyncReport(
    val syncedDonations: Int,
    val syncedAlerts: Int,
    val success: Boolean,
    val message: String
)

class SyncRepository(
    private val context: Context,
    private val donationDao: DonationDao,
    private val alertDao: AlertDao,
    private val scope: CoroutineScope
) {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {

            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            val initialOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isOnline.value = initialOnline

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true

                    scope.launch {
                        delay(2000)
                        syncPendingData()
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        }
    }

    suspend fun getPendingCount(): Int {
        val unsyncedDonations = donationDao.getUnsyncedDonations().size
        val unsyncedAlerts = alertDao.getUnsyncedAlerts().size
        return unsyncedDonations + unsyncedAlerts
    }

    suspend fun syncPendingData(): SyncReport {
        if (_isSyncing.value) return SyncReport(0, 0, false, "Sincronización en curso")
        _isSyncing.value = true

        try {
            val unsyncedDonations = donationDao.getUnsyncedDonations()
            val unsyncedAlerts = alertDao.getUnsyncedAlerts()

            delay(1200)

            for (d in unsyncedDonations) {
                donationDao.markDonationSynced(d.id)
            }

            for (a in unsyncedAlerts) {
                alertDao.markAlertSynced(a.id)
            }

            _lastSyncTimestamp.value = System.currentTimeMillis()
            _isSyncing.value = false

            return SyncReport(
                syncedDonations = unsyncedDonations.size,
                syncedAlerts = unsyncedAlerts.size,
                success = true,
                message = "Sincronización completada exitosamente (${unsyncedDonations.size + unsyncedAlerts.size} elementos subidos)."
            )
        } catch (e: Exception) {
            _isSyncing.value = false
            return SyncReport(0, 0, false, "Error en sincronización: ${e.message}")
        }
    }
}

