package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.EcoSismoDatabase
import com.example.data.model.SeismicAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class SeismicReading(
    val x: Float,
    val y: Float,
    val z: Float,
    val resultantMagnitude: Float,
    val staLtaRatio: Float,
    val timestamp: Long = System.currentTimeMillis()
)

class SeismicMonitoringService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private val alpha = 0.82f

    private var sta = 0.05f
    private var lta = 0.05f
    private val staAlpha = 0.15f
    private val ltaAlpha = 0.005f

    var staLtaThreshold: Float = 3.5f
    var minPgaThresholdMps2: Float = 0.22f

    private var lastAlertTimestamp: Long = 0
    private val alertCooldownMs = 8000L

    inner class LocalBinder : Binder() {
        fun getService(): SeismicMonitoringService = this@SeismicMonitoringService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_MONITORING) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildForegroundNotification("Monitoreando vibraciones en tiempo real")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            startSensorMonitoring()
            _isMonitoring.value = true
        } catch (e: Exception) {
            _isMonitoring.value = false
            stopSelf()
        }
        return START_STICKY
    }

    private fun startSensorMonitoring() {
        accelerometer?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    private fun stopMonitoring() {
        sensorManager?.unregisterListener(this)
        _isMonitoring.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        gravityX = alpha * gravityX + (1 - alpha) * rawX
        gravityY = alpha * gravityY + (1 - alpha) * rawY
        gravityZ = alpha * gravityZ + (1 - alpha) * rawZ

        val linearX = rawX - gravityX
        val linearY = rawY - gravityY
        val linearZ = rawZ - gravityZ

        val pga = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        sta = staAlpha * pga + (1 - staAlpha) * sta
        lta = ltaAlpha * pga + (1 - ltaAlpha) * lta

        val safeLta = if (lta < 0.01f) 0.01f else lta
        val ratio = sta / safeLta

        val reading = SeismicReading(
            x = linearX,
            y = linearY,
            z = linearZ,
            resultantMagnitude = pga,
            staLtaRatio = ratio
        )
        _latestReading.value = reading

        val now = System.currentTimeMillis()
        val currentThreshold = configuredStaLtaThreshold
        val currentMinPga = configuredMinPgaThresholdMps2
        if (ratio >= currentThreshold && pga >= currentMinPga && (now - lastAlertTimestamp > alertCooldownMs)) {
            lastAlertTimestamp = now
            triggerSeismicAlert(pga, ratio)
        }

        if (now - lastTelemetrySyncTime >= 1500) {
            lastTelemetrySyncTime = now
            serviceScope.launch {
                try {
                    val app = applicationContext as? com.example.EcoSismoApp
                    if (app != null) {
                        val battery = app.deviceTelemetryManager.readRealBattery()
                        app.stationRepository.updateLocalStationTelemetry(
                            status = "ACTIVA",
                            pga = pga,
                            battery = battery,
                            accelX = linearX,
                            accelY = linearY,
                            accelZ = linearZ
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private var lastTelemetrySyncTime: Long = 0

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerSeismicAlert(pga: Float, ratio: Float) {
        val category = when {
            pga >= 1.2f -> "CRÍTICO"
            pga >= 0.6f -> "FUERTE"
            pga >= 0.35f -> "MODERADO"
            else -> "LEVE"
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(400)
            }
        } catch (_: Exception) {}

        serviceScope.launch {
            try {
                val app = applicationContext as? com.example.EcoSismoApp
                if (app != null) {
                    val alert = app.stationRepository.triggerLocalStationAnomaly(pga, ratio)
                    _alertEvents.emit(alert)
                } else {
                    val db = EcoSismoDatabase.getDatabase(applicationContext)
                    val alert = SeismicAlert(
                        stationId = "estacion_local",
                        stationName = "Estación Comunitaria Móvil",
                        municipality = "Nodo Local Activo",
                        peakAccelerationMps2 = (pga * 100).toInt() / 100f,
                        staLtaRatio = (ratio * 10).toInt() / 10f,
                        intensityCategory = category,
                        coincidenceCount = 1,
                        isConfirmed = false,
                        isSynced = false
                    )
                    db.alertDao().insertAlert(alert)
                    db.stationDao().updateLocalStationStatus("ALERTA", pga, System.currentTimeMillis())
                    _alertEvents.emit(alert)
                }
            } catch (e: Exception) {
                android.util.Log.e("SeismicService", "Error triggering station anomaly", e)
            }
        }

        showNotification(
            id = ALERT_NOTIFICATION_ID,
            title = "¡Alerta Sísmica Detectada! [$category]",
            text = "Vibración acelerométrica: ${"%.2f".format(pga)} m/s² (STA/LTA: ${"%.1f".format(ratio)}). Red comunitaria en alerta."
        )
    }

    private fun buildForegroundNotification(status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, SeismicMonitoringService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITORING)
            .setContentTitle("Ecosismo: Estación Activa")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showNotification(id: Int, title: String, text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING,
                "Monitoreo Sísmico en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente de estación comunitaria activa"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alertas Sísmicas Comunitarias",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de vibraciones y sismos detectados por la red"
                enableVibration(true)
            }

            manager.createNotificationChannel(monitoringChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    companion object {
        const val ACTION_START_MONITORING = "com.example.service.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.service.STOP_MONITORING"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        const val CHANNEL_MONITORING = "channel_ecos_monitoring"
        const val CHANNEL_ALERTS = "channel_ecos_alerts"

        private val _isMonitoring = MutableStateFlow(false)
        val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

        private val _latestReading = MutableStateFlow(SeismicReading(0f, 0f, 0f, 0f, 1f))
        val latestReading: StateFlow<SeismicReading> = _latestReading.asStateFlow()

        private val _alertEvents = MutableSharedFlow<SeismicAlert>(extraBufferCapacity = 5)
        val alertEvents: SharedFlow<SeismicAlert> = _alertEvents.asSharedFlow()

        @Volatile
        var configuredStaLtaThreshold: Float = 3.2f

        @Volatile
        var configuredMinPgaThresholdMps2: Float = 0.20f

        fun start(context: Context) {
            val intent = Intent(context, SeismicMonitoringService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SeismicMonitoringService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }
}

