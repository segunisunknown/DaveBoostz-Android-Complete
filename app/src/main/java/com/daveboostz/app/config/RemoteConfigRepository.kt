package com.daveboostz.app.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daveboostz.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.remoteConfigStore by preferencesDataStore(name = "daveboostz_remote_config")

@Serializable
private data class SupabasePublishedConfigRow(
    val payload_json: JsonElement? = null,
    val version_number: Int = 1,
    val updated_at: String? = null
)

@Serializable
private data class SupabaseSessionPayload(
    val app_version: String,
    val device_model: String,
    val android_version: String,
    val game_package: String,
    val duration_sec: Long,
    val avg_fps: Int? = null,
    val max_temp_c: Float,
    val shizuku_active: Boolean,
    val created_at: String
)

@Singleton
class RemoteConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheKey = stringPreferencesKey("published_config_json")
    private val lastSyncKey = longPreferencesKey("last_sync_epoch_ms")

    private val _config = MutableStateFlow(PublishedRemoteConfig().validated())
    val config: StateFlow<PublishedRemoteConfig> = _config.asStateFlow()

    private val _syncStatus = MutableStateFlow("Using cached/default configuration")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncedAtMs = MutableStateFlow(0L)
    val lastSyncedAtMs: StateFlow<Long> = _lastSyncedAtMs.asStateFlow()

    init {
        scope.launch {
            loadFromCache()
            fetchPublishedConfigFromSupabase()
            // Poll published config while the app/overlay process is alive. Cached config
            // remains available immediately if the network is unavailable.
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                fetchPublishedConfigFromSupabase()
            }
        }
    }

    private suspend fun loadFromCache() {
        runCatching {
            val prefs = context.remoteConfigStore.data.first()
            val cached = prefs[cacheKey]
            _lastSyncedAtMs.value = prefs[lastSyncKey] ?: 0L
            if (!cached.isNullOrBlank()) {
                _config.value = json.decodeFromString<PublishedRemoteConfig>(cached).validated()
                _syncStatus.value = "Loaded cached config (v${_config.value.publishedVersionNumber})"
            }
        }
    }

    private suspend fun fetchPublishedConfigFromSupabase() {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank()) {
            _syncStatus.value = "Offline mode (Supabase URL/anon key not configured)"
            return
        }

        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/daveboostz_config?id=eq.published&select=payload_json,version_number,updated_at")
                .get()
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _syncStatus.value = "Offline fallback (HTTP ${response.code})"
                    return@use
                }
                val body = response.body?.string().orEmpty()
                val rows = json.decodeFromString<List<SupabasePublishedConfigRow>>(body)
                val payloadElement = rows.firstOrNull()?.payload_json ?: return@use
                val parsed = when (payloadElement) {
                    is JsonPrimitive -> json.decodeFromString<PublishedRemoteConfig>(payloadElement.content)
                    else -> json.decodeFromJsonElement<PublishedRemoteConfig>(payloadElement)
                }.validated()

                val serialized = json.encodeToString(parsed)
                val nowMs = System.currentTimeMillis()
                _config.value = parsed
                _lastSyncedAtMs.value = nowMs
                _syncStatus.value = "Synced live via Supabase (Rev #${parsed.publishedVersionNumber})"
                context.remoteConfigStore.edit {
                    it[cacheKey] = serialized
                    it[lastSyncKey] = nowMs
                }
            }
        }.onFailure { err ->
            _syncStatus.value = "Offline fallback: ${err.localizedMessage ?: "Supabase sync unavailable"}"
        }
    }

    fun refreshNow() {
        scope.launch {
            fetchPublishedConfigFromSupabase()
        }
    }

    /**
     * Records an opt-in gaming session summary to Supabase PostgreSQL when configured
     * and the user explicitly enabled telemetry in Settings.
     */
    fun reportOptInSession(
        gamePackage: String,
        deviceModel: String,
        androidVersion: String,
        durationSec: Long,
        avgFps: Int?,
        maxTempC: Float,
        shizukuActive: Boolean
    ) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank()) return

        scope.launch {
            runCatching {
                val bodyJson = json.encodeToString(
                    SupabaseSessionPayload(
                        app_version = BuildConfig.VERSION_NAME,
                        device_model = deviceModel,
                        android_version = androidVersion,
                        game_package = gamePackage,
                        duration_sec = durationSec.coerceIn(0L, 86400L),
                        avg_fps = avgFps,
                        max_temp_c = maxTempC,
                        shizuku_active = shizukuActive,
                        created_at = Instant.now().toString()
                    )
                )
                val request = Request.Builder()
                    .url("$baseUrl/rest/v1/daveboostz_sessions")
                    .post(bodyJson.toRequestBody(jsonMediaType))
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .build()
                httpClient.newCall(request).execute().close()
            }
        }
    }
}
