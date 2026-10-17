package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.data.model.MonitoringStation
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseSyncRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val databaseId: String = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        "ai-studio-android-ecosismo-bfe1d4ee-227e-4372-912d-f7aa597109d9"
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncStatus = MutableStateFlow("Listo para sincronizar")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { fbAuth ->
        val user = fbAuth.currentUser
        _currentUser.value = user
        if (user != null) {
            _syncStatus.value = "Conectado: ${user.displayName ?: user.email ?: "Usuario"}"
            scope.launch(Dispatchers.IO) {
                saveUserProfile(user)
            }
        } else {
            _syncStatus.value = "Modo local (Sin sesión en la nube)"
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    private suspend fun saveUserProfile(user: FirebaseUser) {
        try {
            val userMap = hashMapOf(
                "userId" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: "Usuario EcoSismo"),
                "registeredAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            firestore.collection("users").document(user.uid)
                .set(userMap, SetOptions.merge())
                .await()
            Log.d("FirebaseSync", "Perfil guardado en Firestore: ${user.uid}")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error guardando perfil de usuario", e)
        }
    }

    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(context)
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: error("No se pudo obtener el usuario de Firebase")
                _currentUser.value = user
                _syncStatus.value = "Autenticado con Google en Firebase"
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Credencial no compatible"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("FirebaseSync", "Error CredentialManager", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error iniciando sesión con Google", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _syncStatus.value = "Sesión cerrada"
    }

    suspend fun saveStationToFirestore(station: MonitoringStation): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val stationDoc = hashMapOf(
                "id" to station.id,
                "name" to station.name,
                "municipality" to station.municipality,
                "department" to station.department,
                "latitude" to station.latitude,
                "longitude" to station.longitude,
                "deviceModel" to station.deviceModel,
                "status" to station.status,
                "lastVibrationMps2" to station.lastVibrationMps2.toDouble(),
                "sampleRateHz" to station.sampleRateHz,
                "mountType" to station.mountType,
                "ownerId" to user.uid,
                "updatedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            firestore.collection("monitoring_stations").document(station.id)
                .set(stationDoc, SetOptions.merge())
                .await()
            _syncStatus.value = "Estación '${station.name}' guardada en Firebase"
            Log.d("FirebaseSync", "Estación sincronizada con Firebase: ${station.id}")
            true
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error guardando estación en Firestore", e)
            false
        }
    }

    suspend fun saveSeismicLog(stationId: String, pgaMps2: Float, status: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val logId = "log_${System.currentTimeMillis()}"
            val logDoc = hashMapOf(
                "id" to logId,
                "stationId" to stationId,
                "pgaMps2" to pgaMps2.toDouble(),
                "status" to status,
                "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                "userId" to user.uid
            )
            firestore.collection("seismic_logs").document(logId)
                .set(logDoc)
                .await()
            Log.d("FirebaseSync", "Registro sísmico guardado en Firestore: $logId")
            true
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error guardando registro sísmico", e)
            false
        }
    }

    suspend fun fetchCloudStations(): List<MonitoringStation> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = firestore.collection("monitoring_stations")
                .limit(50)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val mun = doc.getString("municipality") ?: "Colombia"
                val dept = doc.getString("department") ?: "Nacional"
                val lat = doc.getDouble("latitude") ?: 4.5709
                val lon = doc.getDouble("longitude") ?: -74.2973
                val model = doc.getString("deviceModel") ?: "Sensor Red"
                val st = doc.getString("status") ?: "ACTIVA"
                val pga = (doc.getDouble("lastVibrationMps2") ?: 0.02).toFloat()
                val hz = doc.getLong("sampleRateHz")?.toInt() ?: 100
                val mount = doc.getString("mountType") ?: "Pared estructural"

                MonitoringStation(
                    id = id,
                    name = name,
                    municipality = mun,
                    department = dept,
                    latitude = lat,
                    longitude = lon,
                    deviceModel = model,
                    status = st,
                    lastVibrationMps2 = pga,
                    sampleRateHz = hz,
                    mountType = mount,
                    isLocalDevice = false,
                    isCharging = true
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error consultando estaciones de Firestore", e)
            emptyList()
        }
    }
}
