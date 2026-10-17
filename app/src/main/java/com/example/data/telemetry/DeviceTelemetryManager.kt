package com.example.data.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

data class BatteryTelemetry(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val chargingSource: String = "Batería",
    val temperatureCelsius: Float = 28.0f,
    val voltageMv: Int = 4100,
    val health: String = "Buena",
    val technology: String = "Li-ion",
    val isPowerConnected: Boolean = false
)

data class SensorTelemetry(
    val hasAccelerometer: Boolean = false,
    val accelName: String = "",
    val accelVendor: String = "",
    val accelResolution: Float = 0.001f,
    val accelMaxRange: Float = 19.6f,
    val rawAccelX: Float = 0f,
    val rawAccelY: Float = 0f,
    val rawAccelZ: Float = 9.8f,
    val dynamicAccelX: Float = 0f,
    val dynamicAccelY: Float = 0f,
    val dynamicAccelZ: Float = 0f,
    val resultantPgaMps2: Float = 0.02f,
    val calculatedSampleRateHz: Int = 50,

    val hasGyroscope: Boolean = false,
    val gyroName: String = "",
    val gyroVendor: String = "",
    val gyroXRadS: Float = 0f,
    val gyroYRadS: Float = 0f,
    val gyroZRadS: Float = 0f,

    val hasMagnetometer: Boolean = false,
    val magName: String = "",
    val magVendor: String = "",
    val magXuT: Float = 0f,
    val magYuT: Float = 0f,
    val magZuT: Float = 0f,
    val compassHeadingDegrees: Float = 0f,

    val ambientLux: Float = 0f,
    val hasBarometer: Boolean = false,
    val pressureHpa: Float = 0f
)

data class LocationTelemetry(
    val hasLocation: Boolean = false,
    val latitude: Double = 4.5339,
    val longitude: Double = -75.6811,
    val altitudeMeters: Double = 1480.0,
    val accuracyMeters: Float = 5.0f,
    val provider: String = "GPS",
    val nearestMunicipality: String = "Armenia",
    val nearestDepartment: String = "Quindío"
)

data class DeviceTelemetry(
    val timestamp: Long = System.currentTimeMillis(),
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val sensors: SensorTelemetry = SensorTelemetry(),
    val location: LocationTelemetry = LocationTelemetry(),
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val hardwareBoard: String = Build.BOARD,
    val uptimeHours: Float = (SystemClock.elapsedRealtime() / 3600000.0f)
)

class DeviceTelemetryManager(private val context: Context) : SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val accelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val lightSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val pressureSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val _telemetry = MutableStateFlow(readInitialTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private var gravX = 0f
    private var gravY = 0f
    private var gravZ = 9.8f
    private var isGravInitialized = false
    private val filterAlpha = 0.8f

    private var lastAccelSampleTimeNs: Long = 0
    private var smoothedDeltaNs: Long = 20_000_000L

    private var currentRawAccelX = 0f
    private var currentRawAccelY = 0f
    private var currentRawAccelZ = 9.8f
    private var currentDynAccelX = 0f
    private var currentDynAccelY = 0f
    private var currentDynAccelZ = 0f
    private var currentPga = 0.02f

    private var currentGyroX = 0f
    private var currentGyroY = 0f
    private var currentGyroZ = 0f

    private var currentMagX = 0f
    private var currentMagY = 0f
    private var currentMagZ = 0f
    private var currentHeading = 0f

    private var currentLux = 0f
    private var currentPressure = 0f

    private var isListening = false

    init {
        startSensorListening()
    }

    fun startSensorListening() {
        if (isListening) return
        isListening = true

        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        lightSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        pressureSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopSensorListening() {
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val nowNs = event.timestamp
                if (lastAccelSampleTimeNs > 0) {
                    val delta = (nowNs - lastAccelSampleTimeNs).coerceIn(1_000_000L, 200_000_000L)
                    smoothedDeltaNs = (0.9 * smoothedDeltaNs + 0.1 * delta).toLong()
                }
                lastAccelSampleTimeNs = nowNs

                currentRawAccelX = event.values[0]
                currentRawAccelY = event.values[1]
                currentRawAccelZ = event.values[2]

                if (!isGravInitialized) {
                    gravX = currentRawAccelX
                    gravY = currentRawAccelY
                    gravZ = currentRawAccelZ
                    isGravInitialized = true
                    currentDynAccelX = 0f
                    currentDynAccelY = 0f
                    currentDynAccelZ = 0f
                    currentPga = 0.01f
                } else {
                    gravX = filterAlpha * gravX + (1 - filterAlpha) * currentRawAccelX
                    gravY = filterAlpha * gravY + (1 - filterAlpha) * currentRawAccelY
                    gravZ = filterAlpha * gravZ + (1 - filterAlpha) * currentRawAccelZ

                    currentDynAccelX = currentRawAccelX - gravX
                    currentDynAccelY = currentRawAccelY - gravY
                    currentDynAccelZ = currentRawAccelZ - gravZ

                    currentPga = sqrt(
                        currentDynAccelX * currentDynAccelX +
                                currentDynAccelY * currentDynAccelY +
                                currentDynAccelZ * currentDynAccelZ
                    )
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                currentGyroX = event.values[0]
                currentGyroY = event.values[1]
                currentGyroZ = event.values[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                currentMagX = event.values[0]
                currentMagY = event.values[1]
                currentMagZ = event.values[2]
                val angleRad = atan2(currentMagY.toDouble(), currentMagX.toDouble())
                var deg = Math.toDegrees(angleRad).toFloat()
                if (deg < 0) deg += 360f
                currentHeading = deg
            }
            Sensor.TYPE_LIGHT -> {
                currentLux = event.values[0]
            }
            Sensor.TYPE_PRESSURE -> {
                currentPressure = event.values[0]
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastEmitTime >= 200) {
            lastEmitTime = now
            updateTelemetrySnapshot()
        }
    }

    private var lastEmitTime: Long = 0

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun readRealBattery(): BatteryTelemetry {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)

            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = if (level >= 0 && scale > 0) ((level * 100) / scale) else 90

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val chargingSource = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "Puerto USB"
                    BatteryManager.BATTERY_PLUGGED_AC -> "Red Eléctrica (AC)"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Carga Inalámbrica"
                    else -> if (isCharging) "Conectado a Energía" else "Batería Interna"
                }

                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
                val tempC = tempTenths / 10.0f
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)

                val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                val health = when (healthInt) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Buena (Óptima)"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Sobrecalentamiento"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Deteriorada"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobretensión"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Fría"
                    else -> "Normal"
                }

                val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

                BatteryTelemetry(
                    levelPercent = percent.coerceIn(0, 100),
                    isCharging = isCharging,
                    chargingSource = chargingSource,
                    temperatureCelsius = tempC,
                    voltageMv = voltage,
                    health = health,
                    technology = technology,
                    isPowerConnected = plugged > 0 || isCharging
                )
            } else {
                BatteryTelemetry()
            }
        } catch (_: Exception) {
            BatteryTelemetry()
        }
    }

    fun readRealLocation(): LocationTelemetry {
        var bestLoc: Location? = null
        try {
            val gps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            bestLoc = when {
                gps != null && net != null -> if (gps.time > net.time) gps else net
                gps != null -> gps
                else -> net
            }
        } catch (_: SecurityException) {}

        if (bestLoc != null) {
            val (mun, dept) = findNearestColombianMunicipality(bestLoc.latitude, bestLoc.longitude)
            return LocationTelemetry(
                hasLocation = true,
                latitude = bestLoc.latitude,
                longitude = bestLoc.longitude,
                altitudeMeters = if (bestLoc.hasAltitude()) bestLoc.altitude else 1480.0,
                accuracyMeters = if (bestLoc.hasAccuracy()) bestLoc.accuracy else 8.0f,
                provider = bestLoc.provider ?: "GPS",
                nearestMunicipality = mun,
                nearestDepartment = dept
            )
        }

        return LocationTelemetry()
    }

    fun findNearestColombianMunicipality(lat: Double, lon: Double): Pair<String, String> {
        val municipalities = listOf(
            Triple("Armenia", "Quindío", Pair(4.5339, -75.6811)),
            Triple("Pereira", "Risaralda", Pair(4.8133, -75.6961)),
            Triple("Manizales", "Caldas", Pair(5.0689, -75.5174)),
            Triple("Cali", "Valle del Cauca", Pair(3.4516, -76.5320)),
            Triple("Medellín", "Antioquia", Pair(6.2442, -75.5812)),
            Triple("Ibagué", "Tolima", Pair(4.4389, -75.2322)),
            Triple("Bogotá", "Cundinamarca", Pair(4.6097, -74.0817)),
            Triple("Bucaramanga", "Santander", Pair(7.1193, -73.1227)),
            Triple("Los Santos", "Santander", Pair(6.7820, -73.1180)),
            Triple("Pasto", "Nariño", Pair(1.2136, -77.2811)),
            Triple("Popayán", "Cauca", Pair(2.4448, -76.6147)),
            Triple("Neiva", "Huila", Pair(2.9273, -75.2819)),
            Triple("Villavicencio", "Meta", Pair(4.1420, -73.6266)),
            Triple("Cúcuta", "Norte de Santander", Pair(7.8939, -72.5078)),
            Triple("Quibdó", "Chocó", Pair(5.6947, -76.6611)),
            Triple("Montería", "Córdoba", Pair(8.7479, -75.8814)),
            Triple("Barranquilla", "Atlántico", Pair(10.9685, -74.7813)),
            Triple("Cartagena", "Bolívar", Pair(10.3910, -75.4794)),
            Triple("Santa Marta", "Magdalena", Pair(11.2408, -74.1990))
        )

        var closest = municipalities.first()
        var minDist = Double.MAX_VALUE

        municipalities.forEach { mun ->
            val dLat = lat - mun.third.first
            val dLon = lon - mun.third.second
            val dist = dLat * dLat + dLon * dLon
            if (dist < minDist) {
                minDist = dist
                closest = mun
            }
        }

        return Pair(closest.first, closest.second)
    }

    fun updateTelemetrySnapshot() {
        val rateHz = if (smoothedDeltaNs > 0) {
            (1_000_000_000L / smoothedDeltaNs).toInt().coerceIn(10, 150)
        } else 50

        val sensorData = SensorTelemetry(
            hasAccelerometer = accelSensor != null,
            accelName = accelSensor?.name ?: "No detectado",
            accelVendor = accelSensor?.vendor ?: "N/A",
            accelResolution = accelSensor?.resolution ?: 0.001f,
            accelMaxRange = accelSensor?.maximumRange ?: 19.6f,
            rawAccelX = currentRawAccelX,
            rawAccelY = currentRawAccelY,
            rawAccelZ = currentRawAccelZ,
            dynamicAccelX = currentDynAccelX,
            dynamicAccelY = currentDynAccelY,
            dynamicAccelZ = currentDynAccelZ,
            resultantPgaMps2 = (currentPga * 1000).toInt() / 1000f,
            calculatedSampleRateHz = rateHz,

            hasGyroscope = gyroSensor != null,
            gyroName = gyroSensor?.name ?: "No disponible",
            gyroVendor = gyroSensor?.vendor ?: "N/A",
            gyroXRadS = currentGyroX,
            gyroYRadS = currentGyroY,
            gyroZRadS = currentGyroZ,

            hasMagnetometer = magSensor != null,
            magName = magSensor?.name ?: "No disponible",
            magVendor = magSensor?.vendor ?: "N/A",
            magXuT = currentMagX,
            magYuT = currentMagY,
            magZuT = currentMagZ,
            compassHeadingDegrees = currentHeading,

            ambientLux = currentLux,
            hasBarometer = pressureSensor != null,
            pressureHpa = currentPressure
        )

        _telemetry.value = DeviceTelemetry(
            timestamp = System.currentTimeMillis(),
            battery = readRealBattery(),
            sensors = sensorData,
            location = readRealLocation(),
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            hardwareBoard = Build.BOARD,
            uptimeHours = (SystemClock.elapsedRealtime() / 3600000.0f)
        )
    }

    private fun readInitialTelemetry(): DeviceTelemetry {
        return DeviceTelemetry(
            timestamp = System.currentTimeMillis(),
            battery = readRealBattery(),
            sensors = SensorTelemetry(
                hasAccelerometer = accelSensor != null,
                accelName = accelSensor?.name ?: "Acelerómetro",
                accelVendor = accelSensor?.vendor ?: "Hardware",
                hasGyroscope = gyroSensor != null,
                gyroName = gyroSensor?.name ?: "No disponible",
                hasMagnetometer = magSensor != null,
                magName = magSensor?.name ?: "No disponible"
            ),
            location = readRealLocation(),
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            hardwareBoard = Build.BOARD,
            uptimeHours = (SystemClock.elapsedRealtime() / 3600000.0f)
        )
    }
}

