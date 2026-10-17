package com.example

import android.app.Application
import com.example.data.local.EcoSismoDatabase
import com.example.data.repository.CollectionRepository
import com.example.data.repository.SeismicHistoryRepository
import com.example.data.repository.StationRepository
import com.example.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EcoSismoApp : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val database by lazy { EcoSismoDatabase.getDatabase(this) }
    val collectionRepository by lazy { CollectionRepository(database.donationDao()) }
    val seismicRepository by lazy { SeismicHistoryRepository(database.earthquakeDao()) }
    val deviceTelemetryManager by lazy { com.example.data.telemetry.DeviceTelemetryManager(this) }
    val stationRepository by lazy { StationRepository(this, database.stationDao(), database.alertDao(), deviceTelemetryManager) }
    val syncRepository by lazy { SyncRepository(this, database.donationDao(), database.alertDao(), applicationScope) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        applicationScope.launch {
            try {

                stationRepository.seedInitialCommunityStations()
                seismicRepository.ensureDefaultCatalog()

                seismicRepository.refreshEarthquakes()
            } catch (e: Exception) {
                android.util.Log.e("EcoSismoApp", "Initialization background task error", e)
            }
        }
    }

    companion object {
        lateinit var instance: EcoSismoApp
            private set
    }
}

