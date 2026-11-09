package com.minetallest.minefiy.ytmusic

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/** Raised when no client could produce a playable audio URL. */
class StreamResolutionException(
    val videoId: String,
    message: String,
) : Exception("Could not resolve stream for $videoId: $message")

/**
 * Turns a YouTube `videoId` into a playable audio URL via the InnerTube
 * `player` endpoint — the same API the YouTube apps use.
 *
 * Why this runs on the device and not on yt-proxy:
 * datacenter IPs (Cloud Run, CI, this sandbox) get
 * `playabilityStatus = LOGIN_REQUIRED` / "Sign in to confirm you're not a bot"
 * for every client. A phone's mobile/residential IP does not. So audio is
 * resolved here, and only metadata goes through the backend.
 *
 * Client order is deliberate: ANDROID_VR and IOS are the clients that still
 * return direct `adaptiveFormats[].url` values without a PO token. WEB is
 * deliberately absent — it requires a PO token and returns ciphered URLs.
 *
 * URLs are IP-bound and expire (~6 h), so they are cached in memory for 45
 * minutes only, never persisted.
 */
class StreamResolver(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class CachedStream(val url: String, val expiresAtMillis: Long)

    private val cache = ConcurrentHashMap<String, CachedStream>()
    private val gate = Mutex()

    /** True when a cached, still-valid URL exists. */
    fun cachedUrl(videoId: String): String? =
        cache[videoId]?.takeIf { it.expiresAtMillis > now() }?.url

    fun clearCache() {
        cache.clear()
    }

    /**
     * Resolves the best audio URL for [videoId], trying each client in order.
     *
     * @throws StreamResolutionException when every client refuses (network
     *   flagged as a bot, region block, private/removed video).
     */
    suspend fun resolve(videoId: String): String {
        require(videoId.isNotBlank()) { "videoId must not be blank" }

        cachedUrl(videoId)?.let { return it }

        // Serialise resolutions so a burst of tracks does not hammer YouTube.
        return gate.withLock {
            cachedUrl(videoId)?.let { return@withLock it }

            val failures = mutableListOf<String>()
            for (profile in CLIENTS) {
                try {
                    val url = requestAudioUrl(profile, videoId)
                    cache[videoId] = CachedStream(url, now() + CACHE_TTL_MS)
                    return@withLock url
                } catch (e: StreamResolutionException) {
                    failures += "${profile.name}: ${e.message}"
                } catch (e: Exception) {
                    failures += "${profile.name}: ${typeOf(e)}: ${e.message}"
                }
            }
            throw StreamResolutionException(
                videoId,
                failures.joinToString("; ").ifBlank { "no clients configured" },
            )
        }
    }

    private fun typeOf(e: Exception): String = e::class.simpleName ?: "Exception"

    private suspend fun requestAudioUrl(profile: ClientProfile, videoId: String): String {
        val request = PlayerRequest(
            context = PlayerContext(client = profile.context, user = PlayerUser()),
            videoId = videoId,
        )

        val response = httpClient.post(PLAYER_ENDPOINT) {
            contentType(ContentType.Application.Json)
            header("User-Agent", profile.userAgent)
            header("X-YouTube-Client-Name", profile.clientId)
            header("X-YouTube-Client-Version", profile.context.clientVersion)
            header("Origin", "https://www.youtube.com")
            setBody(json.encodeToString(PlayerRequest.serializer(), request))
        }

        val bodyText = response.bodyAsText()
        if (response.status.value != 200) {
            throw StreamResolutionException(videoId, "player HTTP ${response.status.value}")
        }

        val player = json.decodeFromString(PlayerResponseDto.serializer(), bodyText)

        val status = player.playabilityStatus?.status.orEmpty()
        if (status != "OK") {
            val reason = player.playabilityStatus?.reason?.takeIf { it.isNotBlank() }
            val hint = when {
                reason?.contains("bot", ignoreCase = true) == true ->
                    "YouTube flagged this network. Try mobile data or a different connection."
                status == "LOGIN_REQUIRED" -> "This track requires sign-in from this network."
                status == "UNPLAYABLE" || status == "ERROR" -> "Track is unavailable (removed, private or region-locked)."
                else -> "playabilityStatus=$status"
            }
            throw StreamResolutionException(videoId, reason?.let { "$hint ($it)" } ?: hint)
        }

        val streaming = player.streamingData
            ?: throw StreamResolutionException(videoId, "response had no streamingData")

        val allFormats = streaming.adaptiveFormats.orEmpty() + streaming.formats.orEmpty()
        val audio = allFormats.filter { it.isAudio() }
        if (audio.isEmpty()) {
            throw StreamResolutionException(videoId, "no audio formats offered")
        }

        val direct = audio.filter { !it.url.isNullOrBlank() }
        if (direct.isEmpty()) {
            val ciphered = audio.count { !it.signatureCipher.isNullOrBlank() || !it.cipher.isNullOrBlank() }
            throw StreamResolutionException(
                videoId,
                "all ${audio.size} audio formats are ciphered ($ciphered with signatureCipher); client ${profile.name} cannot play them",
            )
        }

        // Prefer m4a (ExoPlayer-friendly, seekable), then highest bitrate.
        return direct
            .sortedWith(
                compareBy(
                    { if (it.mimeType?.startsWith("audio/mp4") == true) 0 else 1 },
                    { -(it.bitrate ?: 0) },
                ),
            )
            .first()
            .url!!
    }

    companion object {
        private const val PLAYER_ENDPOINT =
            "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        private const val CACHE_TTL_MS = 45L * 60L * 1000L

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        fun defaultHttpClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 12_000
                connectTimeoutMillis = 8_000
                socketTimeoutMillis = 12_000
            }
        }
    }
}

/** One InnerTube client identity. `clientId` is YouTube's numeric client id. */
private data class ClientProfile(
    val name: String,
    val clientId: String,
    val userAgent: String,
    val context: PlayerClientContext,
)

private val CLIENTS: List<ClientProfile> = listOf(
    ClientProfile(
        name = "ANDROID_VR",
        clientId = "28",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.62.27 " +
            "(Linux; U; Android 12; en_US) gzip",
        context = PlayerClientContext(
            clientName = "ANDROID_VR",
            clientVersion = "1.62.27",
            androidSdkVersion = 32,
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            osName = "Android",
            osVersion = "12",
        ),
    ),
    ClientProfile(
        name = "IOS",
        clientId = "5",
        userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
        context = PlayerClientContext(
            clientName = "IOS",
            clientVersion = "20.10.4",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
            osName = "iPhone",
            osVersion = "18.3.2.22D82",
        ),
    ),
    ClientProfile(
        name = "ANDROID",
        clientId = "3",
        userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip",
        context = PlayerClientContext(
            clientName = "ANDROID",
            clientVersion = "20.10.38",
            androidSdkVersion = 30,
            osName = "Android",
            osVersion = "11",
        ),
    ),
)

// --- wire models -----------------------------------------------------------

@Serializable
private data class PlayerRequest(
    val context: PlayerContext,
    val videoId: String,
    val contentCheckOk: Boolean = true,
    val racyCheckOk: Boolean = true,
)

@Serializable
private data class PlayerContext(
    val client: PlayerClientContext,
    val user: PlayerUser = PlayerUser(),
)

@Serializable
private data class PlayerUser(
    val lockedSafetyMode: Boolean = false,
)

@Serializable
private data class PlayerClientContext(
    val clientName: String,
    val clientVersion: String,
    val androidSdkVersion: Int? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val hl: String = "en",
    val gl: String = "US",
)

@Serializable
private data class PlayerResponseDto(
    val playabilityStatus: PlayabilityStatusDto? = null,
    val streamingData: StreamingDataDto? = null,
    val videoDetails: VideoDetailsDto? = null,
)

@Serializable
private data class PlayabilityStatusDto(
    val status: String? = null,
    val reason: String? = null,
)

@Serializable
private data class VideoDetailsDto(
    val videoId: String? = null,
    val title: String? = null,
    val lengthSeconds: String? = null,
    val author: String? = null,
)

@Serializable
private data class StreamingDataDto(
    val adaptiveFormats: List<FormatDto>? = null,
    val formats: List<FormatDto>? = null,
)

@Serializable
private data class FormatDto(
    val itag: Int? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val bitrate: Int? = null,
    val contentLength: String? = null,
    val audioQuality: String? = null,
    val signatureCipher: String? = null,
    val cipher: String? = null,
)

private fun FormatDto.isAudio(): Boolean = mimeType?.startsWith("audio/") == true
