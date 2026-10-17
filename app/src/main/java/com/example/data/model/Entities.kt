package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "donations")
data class DeviceDonation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val brand: String,
    val model: String,
    val condition: String,
    val workingSensors: String,
    val collectionPointId: String,
    val collectionPointName: String,
    val city: String,
    val donorName: String,
    val donorContact: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "stations")
data class MonitoringStation(
    @PrimaryKey
    val id: String,
    val name: String,
    val municipality: String,
    val department: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val sampleRateHz: Int,
    val deviceModel: String,
    val androidVersion: String,
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val hasMagnetometer: Boolean,
    val batteryLevel: Int,
    val lastVibrationMps2: Float,
    val lastHeartbeatTimestamp: Long,
    val mountType: String = "Pared de concreto estructural",
    val isLocalDevice: Boolean = false,
    val isSynced: Boolean = true,
    val isCharging: Boolean = false,
    val chargingSource: String = "Batería",
    val batteryTemperatureC: Float = 28.0f,
    val batteryVoltageMv: Int = 4100,
    val batteryHealth: String = "Buena",
    val realAccelX: Float = 0f,
    val realAccelY: Float = 0f,
    val realAccelZ: Float = 9.8f
)

@Entity(tableName = "seismic_alerts")
data class SeismicAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stationId: String,
    val stationName: String,
    val municipality: String,
    val timestamp: Long = System.currentTimeMillis(),
    val peakAccelerationMps2: Float,
    val staLtaRatio: Float,
    val intensityCategory: String,
    val coincidenceCount: Int = 1,
    val isConfirmed: Boolean = false,
    val isSynced: Boolean = false
)

@Entity(tableName = "sgc_earthquakes")
data class SgcEarthquake(
    @PrimaryKey
    val id: String,
    val magnitude: Double,
    val depthKm: Double,
    val location: String,
    val municipality: String,
    val department: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val formattedTime: String,
    val agency: String = "Servicio Geológico Colombiano (SGC)",
    val alertLevel: String = "Informativo"
)

