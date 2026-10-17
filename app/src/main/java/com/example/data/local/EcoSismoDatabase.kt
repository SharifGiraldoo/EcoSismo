package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DeviceDonation
import com.example.data.model.MonitoringStation
import com.example.data.model.SeismicAlert
import com.example.data.model.SgcEarthquake
import kotlinx.coroutines.flow.Flow

@Dao
interface DonationDao {
    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    fun getAllDonations(): Flow<List<DeviceDonation>>

    @Query("SELECT * FROM donations WHERE isSynced = 0")
    suspend fun getUnsyncedDonations(): List<DeviceDonation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(donation: DeviceDonation): Long

    @Query("UPDATE donations SET isSynced = 1 WHERE id = :id")
    suspend fun markDonationSynced(id: Long)

    @Query("SELECT COUNT(*) FROM donations")
    fun getDonationCount(): Flow<Int>
}

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY status = 'ALERTA' DESC, lastHeartbeatTimestamp DESC")
    fun getAllStations(): Flow<List<MonitoringStation>>

    @Query("SELECT * FROM stations WHERE id = :id")
    fun getStationById(id: String): Flow<MonitoringStation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: MonitoringStation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStations(stations: List<MonitoringStation>)

    @Query("SELECT * FROM stations WHERE isLocalDevice = 1 LIMIT 1")
    suspend fun getLocalStation(): MonitoringStation?

    @Query("UPDATE stations SET status = :status, lastVibrationMps2 = :vibration, lastHeartbeatTimestamp = :timestamp WHERE isLocalDevice = 1")
    suspend fun updateLocalStationStatus(status: String, vibration: Float, timestamp: Long)

    @Query("UPDATE stations SET status = :status, lastVibrationMps2 = :vibration, batteryLevel = :batteryLevel, isCharging = :isCharging, chargingSource = :chargingSource, batteryTemperatureC = :batteryTemp, realAccelX = :accelX, realAccelY = :accelY, realAccelZ = :accelZ, lastHeartbeatTimestamp = :timestamp WHERE isLocalDevice = 1")
    suspend fun updateLocalStationRealTelemetry(
        status: String,
        vibration: Float,
        batteryLevel: Int,
        isCharging: Boolean,
        chargingSource: String,
        batteryTemp: Float,
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        timestamp: Long
    )

    @Query("UPDATE stations SET status = :status, lastVibrationMps2 = :vibration, lastHeartbeatTimestamp = :timestamp WHERE id = :id")
    suspend fun updateStationTelemetry(id: String, status: String, vibration: Float, timestamp: Long)

    @Query("DELETE FROM stations WHERE id = :id")
    suspend fun deleteStation(id: String)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM seismic_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<SeismicAlert>>

    @Query("SELECT * FROM seismic_alerts WHERE isSynced = 0")
    suspend fun getUnsyncedAlerts(): List<SeismicAlert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SeismicAlert): Long

    @Query("UPDATE seismic_alerts SET isSynced = 1 WHERE id = :id")
    suspend fun markAlertSynced(id: Long)
}

@Dao
interface EarthquakeDao {
    @Query("SELECT * FROM sgc_earthquakes ORDER BY timestamp DESC")
    fun getAllEarthquakes(): Flow<List<SgcEarthquake>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEarthquakes(earthquakes: List<SgcEarthquake>)

    @Query("DELETE FROM sgc_earthquakes")
    suspend fun clearEarthquakes()
}

@Database(
    entities = [
        DeviceDonation::class,
        MonitoringStation::class,
        SeismicAlert::class,
        SgcEarthquake::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EcoSismoDatabase : RoomDatabase() {
    abstract fun donationDao(): DonationDao
    abstract fun stationDao(): StationDao
    abstract fun alertDao(): AlertDao
    abstract fun earthquakeDao(): EarthquakeDao

    companion object {
        @Volatile
        private var INSTANCE: EcoSismoDatabase? = null

        fun getDatabase(context: Context): EcoSismoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EcoSismoDatabase::class.java,
                    "ecosismo_community_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
