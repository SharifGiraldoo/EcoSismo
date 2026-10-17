package com.example.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import com.example.data.local.AlertDao
import com.example.data.local.StationDao
import com.example.data.model.MonitoringStation
import com.example.data.model.SeismicAlert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class HardwareSensorDiagnostics(
    val hasAccelerometer: Boolean,
    val accelerometerName: String,
    val accelerometerVendor: String,
    val accelerometerResolution: Float,
    val accelerometerMaxRange: Float,
    val estimatedMaxRateHz: Int,

    val hasGyroscope: Boolean,
    val gyroscopeName: String,

    val hasMagnetometer: Boolean,
    val magnetometerName: String,

    val deviceModel: String,
    val androidVersion: String,
    val isViableForMonitoring: Boolean,
    val diagnosisSummary: String
)

class StationRepository(
    private val context: Context,
    private val stationDao: StationDao,
    private val alertDao: AlertDao,
    val telemetryManager: com.example.data.telemetry.DeviceTelemetryManager? = null
) {
    val allStations: Flow<List<MonitoringStation>> = stationDao.getAllStations()
    val allAlerts: Flow<List<SeismicAlert>> = alertDao.getAllAlerts()

    fun performHardwareDiagnostics(): HardwareSensorDiagnostics {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val mag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val hasAccel = accel != null
        val hasGyro = gyro != null
        val hasMag = mag != null

        val minDelayUs = accel?.minDelay ?: 20000
        val maxRateHz = if (minDelayUs > 0) 1000000 / minDelayUs else 50

        val isViable = hasAccel
        val summary = if (isViable) {
            "Dispositivo APTO para estación comunitaria. Acelerómetro verificado a ~${maxRateHz}Hz." +
                    if (hasGyro) " Giroscopio disponible para medir inclinación y torsión de laderas." else " (Sin giroscopio, operará en modo aceleración pura)."
        } else {
            "Dispositivo no viable: Carece de acelerómetro de hardware."
        }

        return HardwareSensorDiagnostics(
            hasAccelerometer = hasAccel,
            accelerometerName = accel?.name ?: "No detectado",
            accelerometerVendor = accel?.vendor ?: "N/A",
            accelerometerResolution = accel?.resolution ?: 0f,
            accelerometerMaxRange = accel?.maximumRange ?: 0f,
            estimatedMaxRateHz = maxRateHz,
            hasGyroscope = hasGyro,
            gyroscopeName = gyro?.name ?: "No disponible",
            hasMagnetometer = hasMag,
            magnetometerName = mag?.name ?: "No disponible",
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            isViableForMonitoring = isViable,
            diagnosisSummary = summary
        )
    }

    suspend fun registerCurrentDeviceAsStation(
        stationName: String,
        municipality: String,
        department: String,
        latitude: Double,
        longitude: Double,
        mountType: String
    ): MonitoringStation {
        val diag = performHardwareDiagnostics()
        val telem = telemetryManager?.telemetry?.value
        val battery = telem?.battery ?: telemetryManager?.readRealBattery() ?: com.example.data.telemetry.BatteryTelemetry()
        val sensorTelem = telem?.sensors

        val station = MonitoringStation(
            id = "station_local_" + UUID.randomUUID().toString().take(8),
            name = stationName,
            municipality = municipality,
            department = department,
            latitude = latitude,
            longitude = longitude,
            status = "ACTIVA",
            sampleRateHz = sensorTelem?.calculatedSampleRateHz ?: diag.estimatedMaxRateHz.coerceIn(20, 100),
            deviceModel = diag.deviceModel,
            androidVersion = diag.androidVersion,
            hasAccelerometer = diag.hasAccelerometer,
            hasGyroscope = diag.hasGyroscope,
            hasMagnetometer = diag.hasMagnetometer,
            batteryLevel = battery.levelPercent,
            isCharging = battery.isCharging,
            chargingSource = battery.chargingSource,
            batteryTemperatureC = battery.temperatureCelsius,
            batteryVoltageMv = battery.voltageMv,
            batteryHealth = battery.health,
            realAccelX = sensorTelem?.dynamicAccelX ?: 0f,
            realAccelY = sensorTelem?.dynamicAccelY ?: 0f,
            realAccelZ = sensorTelem?.dynamicAccelZ ?: 9.8f,
            lastVibrationMps2 = sensorTelem?.resultantPgaMps2 ?: 0.02f,
            lastHeartbeatTimestamp = System.currentTimeMillis(),
            mountType = mountType,
            isLocalDevice = true,
            isSynced = false
        )
        stationDao.insertStation(station)
        return station
    }

    suspend fun updateLocalStationTelemetry(
        status: String,
        pga: Float,
        battery: com.example.data.telemetry.BatteryTelemetry,
        accelX: Float,
        accelY: Float,
        accelZ: Float
    ) {
        stationDao.updateLocalStationRealTelemetry(
            status = status,
            vibration = (pga * 1000).toInt() / 1000f,
            batteryLevel = battery.levelPercent,
            isCharging = battery.isCharging,
            chargingSource = battery.chargingSource,
            batteryTemp = battery.temperatureCelsius,
            accelX = accelX,
            accelY = accelY,
            accelZ = accelZ,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun getOrCreateLocalStation(): MonitoringStation {
        val existing = stationDao.getLocalStation()
        if (existing != null) return existing
        return registerCurrentDeviceAsStation(
            stationName = "Estación Celular Móvil",
            municipality = "Armenia",
            department = "Quindío",
            latitude = 4.5339,
            longitude = -75.6811,
            mountType = "Pared de concreto estructural"
        )
    }

    suspend fun triggerLocalStationAnomaly(pga: Float, staLtaRatio: Float): SeismicAlert {
        val station = getOrCreateLocalStation()
        stationDao.updateLocalStationStatus("ALERTA", pga, System.currentTimeMillis())

        val category = when {
            pga >= 1.2f -> "CRÍTICO"
            pga >= 0.6f -> "FUERTE"
            pga >= 0.35f -> "MODERADO"
            else -> "LEVE"
        }

        val alert = SeismicAlert(
            stationId = station.id,
            stationName = station.name,
            municipality = station.municipality,
            timestamp = System.currentTimeMillis(),
            peakAccelerationMps2 = (pga * 100).toInt() / 100f,
            staLtaRatio = (staLtaRatio * 10).toInt() / 10f,
            intensityCategory = category,
            coincidenceCount = 1,
            isConfirmed = false,
            isSynced = false
        )
        alertDao.insertAlert(alert)
        return alert
    }

    suspend fun resetStationAlert(stationId: String) {
        stationDao.updateStationTelemetry(stationId, "ACTIVA", 0.02f, System.currentTimeMillis())
    }

    suspend fun resetLocalStation() {
        stationDao.updateLocalStationStatus("ACTIVA", 0.02f, System.currentTimeMillis())
    }

    suspend fun recordSeismicAlert(
        stationId: String,
        stationName: String,
        municipality: String,
        peakAcceleration: Float,
        staLtaRatio: Float,
        category: String,
        coincidenceCount: Int = 1
    ): Long {
        val alert = SeismicAlert(
            stationId = stationId,
            stationName = stationName,
            municipality = municipality,
            timestamp = System.currentTimeMillis(),
            peakAccelerationMps2 = peakAcceleration,
            staLtaRatio = staLtaRatio,
            intensityCategory = category,
            coincidenceCount = coincidenceCount,
            isConfirmed = coincidenceCount >= 2,
            isSynced = false
        )
        return alertDao.insertAlert(alert)
    }

    suspend fun seedInitialCommunityStations() {
        val existing = defaultDeployedCommunityStations()
        stationDao.insertAllStations(existing)
    }

    suspend fun refreshAllStationsTelemetry() {
        val stations = defaultDeployedCommunityStations()
        stationDao.insertAllStations(stations)

        if (telemetryManager != null) {
            val telemetry = telemetryManager.telemetry.value
            val localStation = stationDao.getLocalStation()
            if (localStation != null) {
                stationDao.updateLocalStationRealTelemetry(
                    status = if (telemetry.sensors.resultantPgaMps2 > 0.6f) "ALERTA" else "ACTIVA",
                    vibration = telemetry.sensors.resultantPgaMps2,
                    batteryLevel = telemetry.battery.levelPercent,
                    isCharging = telemetry.battery.isCharging,
                    chargingSource = telemetry.battery.chargingSource,
                    batteryTemp = telemetry.battery.temperatureCelsius,
                    accelX = telemetry.sensors.dynamicAccelX,
                    accelY = telemetry.sensors.dynamicAccelY,
                    accelZ = telemetry.sensors.dynamicAccelZ,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    fun defaultDeployedCommunityStations(): List<MonitoringStation> {
        val now = System.currentTimeMillis()
        val minute = 60000L

        return listOf(
            MonitoringStation(
                id = "sta_sgc_rosc",
                name = "Estación Sismológica ROSC - El Rosal (SGC)",
                municipality = "El Rosal",
                department = "Cundinamarca",
                latitude = 4.8617,
                longitude = -74.2667,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Streckeisen STS-2 / Kinemetrics EpiSensor (SGC / RSNC)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 98,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico 24V",
                batteryTemperatureC = 18.2f,
                batteryVoltageMv = 13800,
                batteryHealth = "Óptima (Banco Solar AGM)",
                realAccelX = 0.003f,
                realAccelY = 0.004f,
                realAccelZ = 0.011f,
                lastVibrationMps2 = 0.012f,
                lastHeartbeatTimestamp = now - (1 * minute),
                mountType = "Bóveda sismológica en roca basal",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_bar2",
                name = "Estación Sismológica BAR2 - Barichara (SGC)",
                municipality = "Barichara",
                department = "Santander",
                latitude = 6.6342,
                longitude = -73.2247,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Nanometrics Trillium 120PA / Centaur (RSNC)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 94,
                isCharging = true,
                chargingSource = "Red / Banco de Baterías",
                batteryTemperatureC = 22.0f,
                batteryVoltageMv = 12600,
                batteryHealth = "Óptima (Nido Sísmico Bucaramanga)",
                realAccelX = 0.012f,
                realAccelY = 0.018f,
                realAccelZ = 0.044f,
                lastVibrationMps2 = 0.048f,
                lastHeartbeatTimestamp = now - (2 * minute),
                mountType = "Túnel sismológico en formación calcárea",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_hel",
                name = "Estación Sismológica HEL - Heliconia (SGC)",
                municipality = "Heliconia",
                department = "Antioquia",
                latitude = 6.2081,
                longitude = -75.7383,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Guralp CMG-3T Banda Ancha (SGC Antioquia)",
                androidVersion = "RSNC Telemetry v4.1",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 91,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico",
                batteryTemperatureC = 20.5f,
                batteryVoltageMv = 12800,
                batteryHealth = "Normal",
                realAccelX = 0.005f,
                realAccelY = 0.007f,
                realAccelZ = 0.016f,
                lastVibrationMps2 = 0.018f,
                lastHeartbeatTimestamp = now - (3 * minute),
                mountType = "Caseta sismológica sobre roca ígnea",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_flo2",
                name = "Estación Sismológica FLO2 - Florencia (SGC)",
                municipality = "Florencia",
                department = "Caquetá",
                latitude = 1.6144,
                longitude = -75.6062,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Guralp CMG-3ESP / Taurus (RSNC Amazonía)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = false,
                hasMagnetometer = true,
                batteryLevel = 89,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico",
                batteryTemperatureC = 26.1f,
                batteryVoltageMv = 12400,
                batteryHealth = "Normal",
                realAccelX = 0.004f,
                realAccelY = 0.005f,
                realAccelZ = 0.014f,
                lastVibrationMps2 = 0.015f,
                lastHeartbeatTimestamp = now - (4 * minute),
                mountType = "Pedestal sismológico aislado de concreto",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_pra",
                name = "Estación Sismológica PRA - Pradera (SGC)",
                municipality = "Pradera",
                department = "Valle del Cauca",
                latitude = 3.4219,
                longitude = -76.2433,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Kinemetrics Basalt / EpiSensor Triaxial (RSNC)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 96,
                isCharging = true,
                chargingSource = "Respaldo DC / Solar",
                batteryTemperatureC = 23.4f,
                batteryVoltageMv = 13600,
                batteryHealth = "Óptima",
                realAccelX = 0.006f,
                realAccelY = 0.008f,
                realAccelZ = 0.019f,
                lastVibrationMps2 = 0.021f,
                lastHeartbeatTimestamp = now - (2 * minute),
                mountType = "Losa basal sismológica anclada",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_rus",
                name = "Estación Sismológica RUS - Nevado del Ruiz / OVSM",
                municipality = "Villamaría / Manizales",
                department = "Caldas",
                latitude = 4.8950,
                longitude = -75.3200,
                status = "ALERTA",
                sampleRateHz = 100,
                deviceModel = "Red Telemétrica Volcánica OVSM (SGC Manizales)",
                androidVersion = "OVSM Volcanic Telemetry v5.0",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 87,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico Alta Montaña",
                batteryTemperatureC = 6.8f,
                batteryVoltageMv = 12500,
                batteryHealth = "Vigilancia Amarilla Volcán Ruiz",
                realAccelX = 0.042f,
                realAccelY = 0.065f,
                realAccelZ = 0.089f,
                lastVibrationMps2 = 0.095f,
                lastHeartbeatTimestamp = now - (1 * minute),
                mountType = "Flanco volcánico sobre estrato andesítico",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_ptb",
                name = "Estación Sismológica PTB - Pasto / Galeras / OVSP",
                municipality = "Pasto",
                department = "Nariño",
                latitude = 1.2136,
                longitude = -77.2811,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Observatorio Vulcanológico y Sismológico de Pasto (SGC)",
                androidVersion = "OVSP Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 93,
                isCharging = true,
                chargingSource = "Solar / Banco Baterías",
                batteryTemperatureC = 13.5f,
                batteryVoltageMv = 12900,
                batteryHealth = "Óptima",
                realAccelX = 0.007f,
                realAccelY = 0.010f,
                realAccelZ = 0.022f,
                lastVibrationMps2 = 0.025f,
                lastHeartbeatTimestamp = now - (3 * minute),
                mountType = "Cámara sismológica subterránea",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_pop2",
                name = "Estación Sismológica POP2 - Popayán (SGC)",
                municipality = "Popayán",
                department = "Cauca",
                latitude = 2.4550,
                longitude = -76.6089,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Observatorio Vulcanológico y Sismológico Popayán (SGC)",
                androidVersion = "OVSPo Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = false,
                hasMagnetometer = true,
                batteryLevel = 92,
                isCharging = true,
                chargingSource = "Red Eléctrica / UPS Industrial",
                batteryTemperatureC = 19.8f,
                batteryVoltageMv = 12700,
                batteryHealth = "Normal",
                realAccelX = 0.005f,
                realAccelY = 0.006f,
                realAccelZ = 0.017f,
                lastVibrationMps2 = 0.019f,
                lastHeartbeatTimestamp = now - (5 * minute),
                mountType = "Pedestal de granito y concreto basal",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_sjc",
                name = "Estación Sismológica SJC - Cúcuta / Falla Boconó",
                municipality = "Cúcuta",
                department = "Norte de Santander",
                latitude = 7.9150,
                longitude = -72.5050,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Kinemetrics EpiSensor Triaxial (RSNC)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 90,
                isCharging = true,
                chargingSource = "Red / UPS Industrial",
                batteryTemperatureC = 28.0f,
                batteryVoltageMv = 12500,
                batteryHealth = "Normal",
                realAccelX = 0.008f,
                realAccelY = 0.011f,
                realAccelZ = 0.025f,
                lastVibrationMps2 = 0.028f,
                lastHeartbeatTimestamp = now - (4 * minute),
                mountType = "Búnker de monitoreo acelerográfico",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_quib",
                name = "Estación Sismológica QUIB - Quibdó / Pacífico (SGC)",
                municipality = "Quibdó",
                department = "Chocó",
                latitude = 5.6980,
                longitude = -76.6520,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Nanometrics Trillium Compact / Falla Atrato (RSNC)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = false,
                hasMagnetometer = true,
                batteryLevel = 88,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico Selva",
                batteryTemperatureC = 27.5f,
                batteryVoltageMv = 12300,
                batteryHealth = "Normal",
                realAccelX = 0.009f,
                realAccelY = 0.013f,
                realAccelZ = 0.029f,
                lastVibrationMps2 = 0.032f,
                lastHeartbeatTimestamp = now - (6 * minute),
                mountType = "Base profunda anclada a lecho rocoso",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_mon",
                name = "Estación Sismológica MON - Montería / Caribe (SGC)",
                municipality = "Montería",
                department = "Córdoba",
                latitude = 8.7619,
                longitude = -75.8822,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Sismógrafo Digital Triaxial RSNC Caribe (SGC)",
                androidVersion = "RSNC Telemetry v4.1",
                hasAccelerometer = true,
                hasGyroscope = false,
                hasMagnetometer = true,
                batteryLevel = 95,
                isCharging = true,
                chargingSource = "Solar / Banco Baterías",
                batteryTemperatureC = 29.2f,
                batteryVoltageMv = 13200,
                batteryHealth = "Óptima",
                realAccelX = 0.004f,
                realAccelY = 0.005f,
                realAccelZ = 0.013f,
                lastVibrationMps2 = 0.014f,
                lastHeartbeatTimestamp = now - (3 * minute),
                mountType = "Caseta de monitoreo en concreto macizo",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_taz",
                name = "Estación Sismológica TAZ - Tausa (SGC)",
                municipality = "Tausa",
                department = "Cundinamarca",
                latitude = 5.1950,
                longitude = -73.8860,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Sismómetro Pozo Profundo Nanometrics (RSNC)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 97,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico",
                batteryTemperatureC = 15.0f,
                batteryVoltageMv = 13500,
                batteryHealth = "Óptima (Pozo 100m)",
                realAccelX = 0.002f,
                realAccelY = 0.003f,
                realAccelZ = 0.008f,
                lastVibrationMps2 = 0.009f,
                lastHeartbeatTimestamp = now - (2 * minute),
                mountType = "Pozo profundo encamisado en acero",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_gua",
                name = "Estación Sismológica GUA - San José del Guaviare (SGC)",
                municipality = "San José del Guaviare",
                department = "Guaviare",
                latitude = 2.5719,
                longitude = -72.6419,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Estación Satelital RSNC Orinoquía-Amazonía (SGC)",
                androidVersion = "RSNC Sat-Link v3.8",
                hasAccelerometer = true,
                hasGyroscope = false,
                hasMagnetometer = true,
                batteryLevel = 90,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico Satelital",
                batteryTemperatureC = 27.0f,
                batteryVoltageMv = 12600,
                batteryHealth = "Normal",
                realAccelX = 0.003f,
                realAccelY = 0.004f,
                realAccelZ = 0.010f,
                lastVibrationMps2 = 0.011f,
                lastHeartbeatTimestamp = now - (7 * minute),
                mountType = "Pedestal sismológico en roca del Escudo Guayanés",
                isLocalDevice = false,
                isSynced = true
            ),
            MonitoringStation(
                id = "sta_sgc_zar",
                name = "Estación Sismológica ZAR - Zaragoza (SGC)",
                municipality = "Zaragoza",
                department = "Antioquia",
                latitude = 7.4912,
                longitude = -74.8694,
                status = "ACTIVA",
                sampleRateHz = 100,
                deviceModel = "Sismógrafo Triaxial Banda Ancha RSNC (SGC)",
                androidVersion = "RSNC Telemetry v4.2",
                hasAccelerometer = true,
                hasGyroscope = true,
                hasMagnetometer = true,
                batteryLevel = 91,
                isCharging = true,
                chargingSource = "Solar Fotovoltaico",
                batteryTemperatureC = 28.1f,
                batteryVoltageMv = 12700,
                batteryHealth = "Normal",
                realAccelX = 0.005f,
                realAccelY = 0.007f,
                realAccelZ = 0.015f,
                lastVibrationMps2 = 0.017f,
                lastHeartbeatTimestamp = now - (5 * minute),
                mountType = "Bóveda reforzada anti-inundación",
                isLocalDevice = false,
                isSynced = true
            )
        )
    }
}

