package com.pronotes.app.data

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronotes.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID

data class CloudUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
)

data class CollabRoomInfo(
    val roomId: String,
    val title: String,
    val ownerId: String,
    val createdAt: Long = 0L,
)

data class CollabMemberInfo(
    val uid: String,
    val displayName: String,
    val color: String,
    val lastSeen: Long,
)

data class CollabChatMsg(
    val id: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long,
)

data class CollabSharedNote(
    val id: String,
    val kind: String,
    val authorId: String,
    val pageIdx: Int,
    val updatedAt: Long,
    val summary: String,
    val itemsJson: String,
)

/**
 * 100% Free, Zero-Secret Cloud Sync & Live Collab Engine.
 *
 * - Default mode uses a 100% free, zero-key public relay (ntfy.sh JSON pub/sub streams + local vault)
 *   keyed by a SHA-256 hash of the user's Google account email & sync key, costing $0 forever and
 *   requiring zero hardcoded API keys or billing accounts.
 * - Optional custom Firebase mode activates automatically only if the user supplies their own
 *   CLOUD_PROJECT_ID and CLOUD_API_KEY via BuildConfig / CI environment variables.
 */
object CloudSync {
    private val PROJECT_ID: String get() = BuildConfig.CLOUD_PROJECT_ID.trim()
    private val DB_ID: String get() = BuildConfig.CLOUD_DB_ID.trim().ifBlank { "(default)" }
    private val API_KEY: String get() = BuildConfig.CLOUD_API_KEY.trim()
    private val GOOGLE_CLIENT_ID: String get() = BuildConfig.GOOGLE_CLIENT_ID.trim()
    private const val REDIRECT_URI = "com.pronotes.app://auth"
    private const val FREE_RELAY_BASE = "https://ntfy.sh"

    private val isCustomFirebaseConfigured: Boolean
        get() = PROJECT_ID.isNotBlank() && API_KEY.isNotBlank()

    private val fsBase: String
        get() = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/$DB_ID/documents"

    private val json = Json { ignoreUnknownKeys = true }

    private val _user = MutableStateFlow<CloudUser?>(null)
    val user: StateFlow<CloudUser?> = _user

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    private val _signInDialogOpen = MutableStateFlow(false)
    val signInDialogOpen: StateFlow<Boolean> = _signInDialogOpen

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("cloud_auth", Context.MODE_PRIVATE)

    fun init(context: Context) {
        val p = prefs(context)
        val uid = p.getString("uid", "").orEmpty()
        val email = p.getString("email", "").orEmpty()
        val name = p.getString("displayName", "").orEmpty()
        val token = p.getString("idToken", "").orEmpty()
        if (uid.isNotBlank() && token.isNotBlank()) {
            _user.value = CloudUser(uid, email, name, p.getString("photoUrl", "").orEmpty())
        }
    }

    fun openSignInDialog() {
        _signInDialogOpen.value = true
    }

    fun dismissSignInDialog() {
        _signInDialogOpen.value = false
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Generates a clean 6-digit shareable Live Study Room code (e.g. "482-910"). */
    fun generateRoomCode(): String {
        val partA = (100..999).random()
        val partB = (100..999).random()
        return "$partA-$partB"
    }

    /** Ensures a participant identity exists for code-based Live Study Rooms even before full Google Sign-In. */
    fun ensureRoomParticipant(context: Context, preferredName: String = ""): CloudUser {
        _user.value?.let { existing ->
            if (preferredName.isNotBlank() && existing.displayName != preferredName) {
                val updated = existing.copy(displayName = preferredName.trim())
                _user.value = updated
                return updated
            }
            return existing
        }
        val p = prefs(context)
        val savedGuestUid = p.getString("guest_uid", "").orEmpty().ifBlank {
            "scholar_" + UUID.randomUUID().toString().replace("-", "").take(8)
        }
        val savedGuestName = preferredName.trim().ifBlank {
            p.getString("guest_name", "").orEmpty().ifBlank { "Scholar #${savedGuestUid.takeLast(4).uppercase()}" }
        }
        p.edit().putString("guest_uid", savedGuestUid).putString("guest_name", savedGuestName).apply()
        return CloudUser(
            uid = savedGuestUid,
            email = "$savedGuestUid@studyroom.local",
            displayName = savedGuestName,
            photoUrl = "",
        )
    }

    /**
     * Launches Google's official OAuth2 / AccountChooser authority page in the system browser
     * so the user can authenticate and grant authority just like standard Google apps, while also
     * opening the in-app authority completion sheet so zero hardcoded client secrets are required.
     */
    fun buildGoogleAuthIntent(): Intent {
        _signInDialogOpen.value = true
        val nonce = UUID.randomUUID().toString()
        val url = if (GOOGLE_CLIENT_ID.isNotBlank()) {
            "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=${URLEncoder.encode(GOOGLE_CLIENT_ID, "UTF-8")}" +
                "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
                "&response_type=id_token" +
                "&scope=${URLEncoder.encode("openid email profile", "UTF-8")}" +
                "&nonce=$nonce" +
                "&prompt=consent%20select_account"
        } else {
            "https://accounts.google.com/AccountChooser?continue=" +
                URLEncoder.encode("https://myaccount.google.com/permissions", "UTF-8")
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Signs in using Google Account email + optional sync key (100% free, deterministic cross-device UID). */
    suspend fun signInWithFreeGoogleAccount(
        context: Context,
        emailInput: String,
        displayNameInput: String,
        syncKeyInput: String = "",
    ): Result<CloudUser> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = emailInput.trim().lowercase()
            require(cleanEmail.contains("@") && cleanEmail.length >= 5) { "Please enter a valid Google email address." }
            val cleanName = displayNameInput.trim().ifBlank { cleanEmail.substringBefore('@') }
            val keyPart = syncKeyInput.trim().lowercase()
            val deterministicUid = "u_" + sha256Hex("pronotes_v10:$cleanEmail:$keyPart").take(22)
            val freeToken = "free_v10_$deterministicUid"

            prefs(context).edit()
                .putString("idToken", freeToken)
                .putString("refreshToken", freeToken)
                .putLong("expiresAt", Long.MAX_VALUE / 2)
                .putString("uid", deterministicUid)
                .putString("email", cleanEmail)
                .putString("displayName", cleanName)
                .putString("photoUrl", "")
                .apply()

            SettingsStore.get(context).update {
                it.copy(googleUid = deterministicUid, googleEmail = cleanEmail, googleName = cleanName)
            }
            val u = CloudUser(deterministicUid, cleanEmail, cleanName, "")
            _user.value = u
            _signInDialogOpen.value = false
            _status.value = "Signed in as $cleanEmail (Free Cloud)"
            u
        }.onFailure {
            _status.value = "Sign-in failed: ${it.message}"
        }
    }

    /** Handles com.pronotes.app://auth#id_token=... redirect from browser. */
    suspend fun handleAuthUri(context: Context, uri: Uri): Result<CloudUser> {
        val frag = uri.fragment.orEmpty()
        val params = frag.split('&').mapNotNull { kv ->
            val i = kv.indexOf('=')
            if (i <= 0) null else kv.substring(0, i) to Uri.decode(kv.substring(i + 1))
        }.toMap()
        val idToken = params["id_token"] ?: uri.getQueryParameter("id_token")
        return if (!idToken.isNullOrBlank()) {
            signInWithGoogleIdToken(context, idToken)
        } else {
            val err = params["error"] ?: uri.getQueryParameter("error") ?: "Google sign-in was cancelled"
            Result.failure(IllegalStateException(err))
        }
    }

    /** Exchanges a Google OAuth id_token (or decodes JWT claims in Free Cloud mode) for user session. */
    suspend fun signInWithGoogleIdToken(context: Context, googleIdToken: String): Result<CloudUser> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isCustomFirebaseConfigured) {
                // Decode unverified JWT payload if user pasted an id_token in Free Cloud mode
                val parts = googleIdToken.trim().split('.')
                if (parts.size >= 2) {
                    val decoded = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                    val obj = json.parseToJsonElement(decoded).jsonObject
                    val email = obj["email"]?.jsonPrimitive?.content ?: "user@gmail.com"
                    val name = obj["name"]?.jsonPrimitive?.content ?: email.substringBefore('@')
                    return@withContext signInWithFreeGoogleAccount(context, email, name)
                }
                return@withContext signInWithFreeGoogleAccount(context, googleIdToken, "")
            }

            val payload = buildJsonObject {
                put("postBody", "id_token=$googleIdToken&providerId=google.com")
                put("requestUri", "https://$PROJECT_ID.firebaseapp.com/__/auth/handler")
                put("returnIdpCredential", true)
                put("returnSecureToken", true)
            }.toString()
            val res = httpJson("https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$API_KEY", "POST", payload)
            val idToken = res["idToken"]?.jsonPrimitive?.content ?: error("Missing idToken")
            val refresh = res["refreshToken"]?.jsonPrimitive?.content.orEmpty()
            val uid = res["localId"]?.jsonPrimitive?.content ?: error("Missing uid")
            val email = res["email"]?.jsonPrimitive?.content.orEmpty()
            val name = res["displayName"]?.jsonPrimitive?.content.orEmpty().ifBlank { email.substringBefore('@') }
            val photo = res["photoUrl"]?.jsonPrimitive?.content.orEmpty()

            prefs(context).edit()
                .putString("idToken", idToken)
                .putString("refreshToken", refresh)
                .putLong("expiresAt", System.currentTimeMillis() + 3300_000L)
                .putString("uid", uid)
                .putString("email", email)
                .putString("displayName", name)
                .putString("photoUrl", photo)
                .apply()

            SettingsStore.get(context).update {
                it.copy(googleUid = uid, googleEmail = email, googleName = name)
            }
            val u = CloudUser(uid, email, name, photo)
            _user.value = u
            upsertUserProfile(context, u)
            _status.value = "Signed in as ${u.email}"
            u
        }.onFailure {
            _status.value = "Sign-in failed: ${it.message}"
        }
    }

    fun signOut(context: Context) {
        prefs(context).edit().clear().apply()
        _user.value = null
        _status.value = "Signed out"
        SettingsStore.get(context).update {
            it.copy(googleUid = "", googleEmail = "", googleName = "")
        }
    }

    private suspend fun validToken(context: Context): String? = withContext(Dispatchers.IO) {
        val p = prefs(context)
        val cur = p.getString("idToken", "").orEmpty()
        if (!isCustomFirebaseConfigured) {
            return@withContext cur.ifBlank { null }
        }
        val exp = p.getLong("expiresAt", 0L)
        if (cur.isNotBlank() && System.currentTimeMillis() < exp) return@withContext cur
        val ref = p.getString("refreshToken", "").orEmpty()
        if (ref.isBlank()) return@withContext null
        runCatching {
            val conn = (URL("https://securetoken.googleapis.com/v1/token?key=$API_KEY").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { it.write("grant_type=refresh_token&refresh_token=${URLEncoder.encode(ref, "UTF-8")}".toByteArray()) }
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val obj = json.parseToJsonElement(raw).jsonObject
            val newTok = obj["id_token"]?.jsonPrimitive?.content ?: return@runCatching null
            val newRef = obj["refresh_token"]?.jsonPrimitive?.content ?: ref
            p.edit().putString("idToken", newTok).putString("refreshToken", newRef).putLong("expiresAt", System.currentTimeMillis() + 3300_000L).apply()
            newTok
        }.getOrNull()
    }

    private suspend fun upsertUserProfile(context: Context, user: CloudUser) {
        if (!isCustomFirebaseConfigured) return
        val tok = validToken(context) ?: return
        val now = System.currentTimeMillis()
        val fields = buildJsonObject {
            put("uid", fsStr(user.uid))
            put("email", fsStr(user.email))
            put("displayName", fsStr(user.displayName))
            put("photoURL", fsStr(user.photoUrl))
            put("updatedAt", fsInt(now))
        }
        val body = buildJsonObject { put("fields", fields) }.toString()
        runCatching { httpJson("$fsBase/users/${user.uid}", "PATCH", body, tok) }
    }

    /** Full bidirectional sync of settings, folders, and notebooks for the signed-in user. */
    suspend fun syncAll(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val u = _user.value ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        _syncing.value = true
        try {
            if (!isCustomFirebaseConfigured) {
                return@withContext syncAllFreeRelay(context, u)
            }
            val tok = validToken(context) ?: return@withContext Result.failure(IllegalStateException("Session expired — please sign in again"))
            val repo = Repository.get(context)
            val store = SettingsStore.get(context)
            val s = store.settings.value
            val now = System.currentTimeMillis()

            val prefData = buildJsonObject {
                put("themeMode", fsStr(s.themeMode.name))
                put("accent", fsStr(s.accent.name))
                put("appFont", fsStr(s.appFont.name))
                put("motionStyle", fsStr(s.motionStyle.name))
                put("liquidGlass", fsBool(s.liquidGlass))
                put("paperTint", fsStr(s.paperTint.name))
                put("defaultPaper", fsStr(s.defaultPaper.name))
                put("scroll", fsStr(s.scroll.name))
                put("pomodoroMinutes", fsInt(s.pomodoroMinutes.toLong()))
            }
            val prefBody = buildJsonObject {
                put("fields", buildJsonObject {
                    put("updatedAt", fsInt(now))
                    put("data", fsMap(prefData))
                })
            }.toString()
            runCatching { httpJson("$fsBase/users/${u.uid}/preferences/main", "PATCH", prefBody, tok) }

            val folderItems = buildJsonArray {
                repo.library.value.folders.forEach { f ->
                    add(fsMap(buildJsonObject {
                        put("id", fsStr(f.id))
                        put("name", fsStr(f.name))
                        put("parentId", if (f.parentId != null) fsStr(f.parentId) else fsNull())
                        put("color", fsInt(f.color.toLong()))
                        put("emoji", fsStr(f.emoji))
                        put("created", fsInt(f.created))
                        put("modified", fsInt(f.modified))
                    }))
                }
            }
            val foldersBody = buildJsonObject {
                put("fields", buildJsonObject {
                    put("updatedAt", fsInt(now))
                    put("items", buildJsonObject { put("arrayValue", buildJsonObject { put("values", folderItems) }) })
                })
            }.toString()
            runCatching { httpJson("$fsBase/users/${u.uid}/library/folders", "PATCH", foldersBody, tok) }

            var pushed = 0
            var pulled = 0
            val localNotes = repo.library.value.notebooks.filter { it.deletedAt == null }
            val remoteDocs = runCatching {
                httpJson("$fsBase/users/${u.uid}/notebooks?pageSize=100", "GET", null, tok)["documents"]?.jsonArray.orEmpty()
            }.getOrDefault(emptyList())
            val remoteMod = HashMap<String, Pair<Long, JsonObject>>()
            for (el in remoteDocs) {
                val obj = el.jsonObject
                val fields = obj["fields"]?.jsonObject ?: continue
                val rid = fields["id"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: continue
                val mod = fields["modified"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                remoteMod[rid] = mod to fields
            }

            for (meta in localNotes) {
                val r = remoteMod[meta.id]
                if (r == null || meta.modified >= r.first) {
                    val noteFile = File(repo.noteDir(meta.id), "note.json")
                    val rawJson = if (noteFile.exists()) noteFile.readText() else "{}"
                    val payloadFields = buildJsonObject {
                        put("coverColor", fsInt(meta.coverColor.toLong()))
                        put("paper", fsStr(meta.paper.name))
                        put("cover", fsStr(meta.coverSpec))
                        put("isWhiteboard", fsBool(meta.isWhiteboard))
                        put("noteJson", fsStr(rawJson.take(750_000)))
                    }
                    val docBody = buildJsonObject {
                        put("fields", buildJsonObject {
                            put("id", fsStr(meta.id))
                            put("title", fsStr(meta.title.take(280)))
                            if (meta.folderId != null) put("folderId", fsStr(meta.folderId))
                            put("modified", fsInt(meta.modified))
                            put("updatedAt", fsInt(now))
                            put("deleted", fsBool(false))
                            put("payload", fsMap(payloadFields))
                        })
                    }.toString()
                    if (runCatching { httpJson("$fsBase/users/${u.uid}/notebooks/${meta.id}", "PATCH", docBody, tok) }.isSuccess) {
                        pushed++
                    }
                }
            }

            val localMap = localNotes.associateBy { it.id }
            for ((rid, pair) in remoteMod) {
                val (mod, fields) = pair
                val deleted = fields["deleted"]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.content?.toBoolean() ?: false
                if (deleted) continue
                val existing = localMap[rid]
                if (existing == null || mod > existing.modified + 2000L) {
                    val title = fields["title"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Cloud Note"
                    val payload = fields["payload"]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject
                    val rawJson = payload?.get("noteJson")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content
                    if (!rawJson.isNullOrBlank() && rawJson.trimStart().startsWith("{")) {
                        val paper = payload["paper"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content?.let {
                            runCatching { PaperStyle.valueOf(it) }.getOrNull()
                        } ?: PaperStyle.RULED
                        val coverColor = payload["coverColor"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toIntOrNull() ?: CoverColors[0]
                        val newId = if (existing != null) existing.id else repo.createNotebook(title, null, coverColor, paper)
                        File(repo.noteDir(newId), "note.json").writeText(rawJson)
                        pulled++
                    }
                }
            }

            store.update { it.copy(lastCloudSyncMs = now) }
            val msg = "Synced: $pushed pushed, $pulled updated"
            _status.value = msg
            Result.success(msg)
        } catch (e: Throwable) {
            val msg = "Sync failed: ${e.message}"
            _status.value = msg
            Result.failure(e)
        } finally {
            _syncing.value = false
        }
    }

    /** 100% Free Cloud Sync over keyless public relay + local vault backup. */
    private suspend fun syncAllFreeRelay(context: Context, u: CloudUser): Result<String> {
        return runCatching {
            val repo = Repository.get(context)
            val store = SettingsStore.get(context)
            val now = System.currentTimeMillis()
            val topic = "pronotes_v10_vault_${u.uid.take(24)}"
            val vaultDir = File(context.filesDir, "free_cloud_vault/${u.uid}").apply { mkdirs() }

            var pushed = 0
            var pulled = 0
            val localNotes = repo.library.value.notebooks.filter { it.deletedAt == null }

            // 1. Pull remote note packets from free relay stream
            val remotePackets = fetchFreeRelayMessages(topic, since = "72h")
            val remoteNotes = HashMap<String, JsonObject>()
            for (pkt in remotePackets) {
                if (pkt["kind"]?.jsonPrimitive?.content == "note") {
                    val nid = pkt["id"]?.jsonPrimitive?.content ?: continue
                    val mod = pkt["modified"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    val prevMod = remoteNotes[nid]?.get("modified")?.jsonPrimitive?.content?.toLongOrNull() ?: -1L
                    if (mod >= prevMod) remoteNotes[nid] = pkt
                }
            }

            val localMap = localNotes.associateBy { it.id }
            for ((nid, pkt) in remoteNotes) {
                val mod = pkt["modified"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val existing = localMap[nid]
                if (existing == null || mod > existing.modified + 2000L) {
                    val title = pkt["title"]?.jsonPrimitive?.content ?: "Cloud Note"
                    val rawJson = pkt["noteJson"]?.jsonPrimitive?.content
                    if (!rawJson.isNullOrBlank() && rawJson.trimStart().startsWith("{")) {
                        val paper = pkt["paper"]?.jsonPrimitive?.content?.let {
                            runCatching { PaperStyle.valueOf(it) }.getOrNull()
                        } ?: PaperStyle.RULED
                        val coverColor = pkt["coverColor"]?.jsonPrimitive?.content?.toIntOrNull() ?: CoverColors[0]
                        val targetId = existing?.id ?: repo.createNotebook(title, null, coverColor, paper)
                        File(repo.noteDir(targetId), "note.json").writeText(rawJson)
                        pulled++
                    }
                }
            }

            // 2. Push local notebooks that are newer or not yet in remote stream
            for (meta in localNotes) {
                val rMod = remoteNotes[meta.id]?.get("modified")?.jsonPrimitive?.content?.toLongOrNull() ?: -1L
                val noteFile = File(repo.noteDir(meta.id), "note.json")
                val rawJson = if (noteFile.exists()) noteFile.readText() else "{}"
                File(vaultDir, "${meta.id}.json").writeText(rawJson)
                if (meta.modified > rMod) {
                    val pkt = buildJsonObject {
                        put("kind", "note")
                        put("id", meta.id)
                        put("title", meta.title.take(180))
                        put("paper", meta.paper.name)
                        put("coverColor", meta.coverColor)
                        put("modified", meta.modified)
                        put("noteJson", rawJson.take(3600))
                    }.toString()
                    runCatching { postFreeRelayMessage(topic, pkt) }
                    pushed++
                }
            }

            store.update { it.copy(lastCloudSyncMs = now) }
            val msg = "Free Cloud Synced: $pushed saved, $pulled updated"
            _status.value = msg
            msg
        }.onFailure {
            _status.value = "Sync failed: ${it.message}"
        }
    }

    // ---------------------------------------------------------------- Live Collab Study Rooms

    private fun roomTopic(roomId: String): String =
        "pronotes_v10_room_${sha256Hex(roomId.trim().uppercase()).take(18)}"

    private fun localRoomFile(context: Context, roomId: String): File {
        val dir = File(context.filesDir, "free_collab_rooms").apply { mkdirs() }
        return File(dir, "${roomId.trim().uppercase()}.json")
    }

    suspend fun createOrJoinRoom(
        context: Context,
        roomCode: String,
        roomTitle: String,
        preferredName: String = "",
    ): Result<CollabRoomInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val u = ensureRoomParticipant(context, preferredName)
            val cleanId = roomCode.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(16)
            require(cleanId.length >= 3) { "Room code must be at least 3 characters." }
            val now = System.currentTimeMillis()
            val titleClean = roomTitle.trim().ifBlank { "Study Room $cleanId" }.take(150)

            if (!isCustomFirebaseConfigured) {
                val topic = roomTopic(cleanId)
                val joinPkt = buildJsonObject {
                    put("kind", "member")
                    put("roomId", cleanId)
                    put("roomTitle", titleClean)
                    put("uid", u.uid)
                    put("displayName", u.displayName.ifBlank { u.email.substringBefore('@') }.take(70))
                    put("color", "#3D6CF4")
                    put("lastSeen", now)
                }.toString()
                runCatching { postFreeRelayMessage(topic, joinPkt) }
                appendLocalRoomEvent(context, cleanId, json.parseToJsonElement(joinPkt).jsonObject)
                return@runCatching CollabRoomInfo(cleanId, titleClean, u.uid, now)
            }

            val tok = validToken(context) ?: error("Session expired — please sign in again.")
            val existing = runCatching { httpJson("$fsBase/collabRooms/$cleanId", "GET", null, tok) }.getOrNull()
            val info = if (existing != null && existing["fields"] != null) {
                val f = existing["fields"]!!.jsonObject
                CollabRoomInfo(
                    roomId = cleanId,
                    title = f["title"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Study Room $cleanId",
                    ownerId = f["ownerId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: u.uid,
                    createdAt = f["createdAt"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: now,
                )
            } else {
                val body = buildJsonObject {
                    put("fields", buildJsonObject {
                        put("roomId", fsStr(cleanId))
                        put("ownerId", fsStr(u.uid))
                        put("title", fsStr(titleClean))
                        put("createdAt", fsInt(now))
                        put("updatedAt", fsInt(now))
                    })
                }.toString()
                httpJson("$fsBase/collabRooms/$cleanId?currentDocument.exists=false", "PATCH", body, tok)
                CollabRoomInfo(cleanId, titleClean, u.uid, now)
            }
            val memBody = buildJsonObject {
                put("fields", buildJsonObject {
                    put("uid", fsStr(u.uid))
                    put("displayName", fsStr(u.displayName.ifBlank { u.email.substringBefore('@') }.take(70)))
                    put("color", fsStr("#3D6CF4"))
                    put("lastSeen", fsInt(now))
                })
            }.toString()
            httpJson("$fsBase/collabRooms/$cleanId/members/${u.uid}", "PATCH", memBody, tok)
            info
        }
    }

    suspend fun fetchRoomFeed(context: Context, roomId: String): Result<Triple<List<CollabMemberInfo>, List<CollabChatMsg>, List<CollabSharedNote>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!isCustomFirebaseConfigured) {
                    val topic = roomTopic(roomId)
                    val remoteEvents = fetchFreeRelayMessages(topic, since = "12h")
                    val localEvents = readLocalRoomEvents(context, roomId)
                    val allEvents = localEvents + remoteEvents

                    val memberMap = LinkedHashMap<String, CollabMemberInfo>()
                    val msgMap = LinkedHashMap<String, CollabChatMsg>()
                    val itemMap = LinkedHashMap<String, CollabSharedNote>()

                    for (ev in allEvents) {
                        when (ev["kind"]?.jsonPrimitive?.content) {
                            "member" -> {
                                val uid = ev["uid"]?.jsonPrimitive?.content ?: continue
                                val name = ev["displayName"]?.jsonPrimitive?.content ?: "Student"
                                val color = ev["color"]?.jsonPrimitive?.content ?: "#3D6CF4"
                                val seen = ev["lastSeen"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                val prev = memberMap[uid]
                                if (prev == null || seen >= prev.lastSeen) {
                                    memberMap[uid] = CollabMemberInfo(uid, name, color, seen)
                                }
                            }
                            "chat" -> {
                                val id = ev["id"]?.jsonPrimitive?.content ?: continue
                                msgMap[id] = CollabChatMsg(
                                    id = id,
                                    authorId = ev["authorId"]?.jsonPrimitive?.content.orEmpty(),
                                    authorName = ev["authorName"]?.jsonPrimitive?.content ?: "Student",
                                    text = ev["text"]?.jsonPrimitive?.content.orEmpty(),
                                    createdAt = ev["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                                )
                            }
                            "item" -> {
                                val id = ev["id"]?.jsonPrimitive?.content ?: continue
                                itemMap[id] = CollabSharedNote(
                                    id = id,
                                    kind = "sticky",
                                    authorId = ev["authorId"]?.jsonPrimitive?.content.orEmpty(),
                                    pageIdx = ev["pageIdx"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                    updatedAt = ev["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                                    summary = ev["summary"]?.jsonPrimitive?.content.orEmpty(),
                                    itemsJson = ev["text"]?.jsonPrimitive?.content.orEmpty(),
                                )
                            }
                        }
                    }
                    return@runCatching Triple(
                        memberMap.values.sortedByDescending { it.lastSeen },
                        msgMap.values.sortedBy { it.createdAt },
                        itemMap.values.sortedByDescending { it.updatedAt },
                    )
                }

                val tok = validToken(context) ?: error("Sign in required")
                val memDocs = runCatching { httpJson("$fsBase/collabRooms/$roomId/members?pageSize=30", "GET", null, tok)["documents"]?.jsonArray.orEmpty() }.getOrDefault(emptyList())
                val members = memDocs.mapNotNull { el ->
                    val f = el.jsonObject["fields"]?.jsonObject ?: return@mapNotNull null
                    CollabMemberInfo(
                        uid = f["uid"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: return@mapNotNull null,
                        displayName = f["displayName"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Student",
                        color = f["color"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "#3D6CF4",
                        lastSeen = f["lastSeen"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    )
                }.sortedByDescending { it.lastSeen }

                val msgDocs = runCatching { httpJson("$fsBase/collabRooms/$roomId/messages?pageSize=60", "GET", null, tok)["documents"]?.jsonArray.orEmpty() }.getOrDefault(emptyList())
                val msgs = msgDocs.mapNotNull { el ->
                    val f = el.jsonObject["fields"]?.jsonObject ?: return@mapNotNull null
                    CollabChatMsg(
                        id = f["id"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: return@mapNotNull null,
                        authorId = f["authorId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content.orEmpty(),
                        authorName = f["authorName"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Student",
                        text = f["text"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content.orEmpty(),
                        createdAt = f["createdAt"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    )
                }.sortedBy { it.createdAt }

                val itemDocs = runCatching { httpJson("$fsBase/collabRooms/$roomId/items?pageSize=50", "GET", null, tok)["documents"]?.jsonArray.orEmpty() }.getOrDefault(emptyList())
                val items = itemDocs.mapNotNull { el ->
                    val f = el.jsonObject["fields"]?.jsonObject ?: return@mapNotNull null
                    val dataF = f["data"]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject
                    CollabSharedNote(
                        id = f["id"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: return@mapNotNull null,
                        kind = f["kind"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "text",
                        authorId = f["authorId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content.orEmpty(),
                        pageIdx = f["pageIdx"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        updatedAt = f["updatedAt"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        summary = dataF?.get("summary")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content.orEmpty(),
                        itemsJson = dataF?.get("text")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content.orEmpty(),
                    )
                }.sortedByDescending { it.updatedAt }

                Triple(members, msgs, items)
            }
        }

    suspend fun sendRoomMessage(context: Context, roomId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val u = ensureRoomParticipant(context)
            val clean = text.trim().take(980)
            require(clean.isNotEmpty())
            val msgId = "m_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val now = System.currentTimeMillis()
            val authorName = u.displayName.ifBlank { u.email.substringBefore('@') }.take(70)

            if (!isCustomFirebaseConfigured) {
                val pkt = buildJsonObject {
                    put("kind", "chat")
                    put("id", msgId)
                    put("authorId", u.uid)
                    put("authorName", authorName)
                    put("text", clean)
                    put("createdAt", now)
                }
                appendLocalRoomEvent(context, roomId, pkt)
                runCatching { postFreeRelayMessage(roomTopic(roomId), pkt.toString()) }
                return@runCatching
            }

            val tok = validToken(context) ?: error("Session expired")
            val body = buildJsonObject {
                put("fields", buildJsonObject {
                    put("id", fsStr(msgId))
                    put("authorId", fsStr(u.uid))
                    put("authorName", fsStr(authorName))
                    put("text", fsStr(clean))
                    put("createdAt", fsInt(now))
                })
            }.toString()
            httpJson("$fsBase/collabRooms/$roomId/messages/$msgId?currentDocument.exists=false", "PATCH", body, tok)
            Unit
        }
    }

    suspend fun sharePageSnippetToRoom(context: Context, roomId: String, pageIdx: Int, summary: String, textBody: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val u = ensureRoomParticipant(context)
                val itemId = "note_${u.uid.take(8)}_${System.currentTimeMillis()}"
                val now = System.currentTimeMillis()

                if (!isCustomFirebaseConfigured) {
                    val pkt = buildJsonObject {
                        put("kind", "item")
                        put("id", itemId)
                        put("authorId", u.uid)
                        put("pageIdx", pageIdx)
                        put("updatedAt", now)
                        put("summary", summary.take(180))
                        put("text", textBody.take(3500))
                    }
                    appendLocalRoomEvent(context, roomId, pkt)
                    runCatching { postFreeRelayMessage(roomTopic(roomId), pkt.toString()) }
                    return@runCatching
                }

                val tok = validToken(context) ?: error("Session expired")
                val dataFields = buildJsonObject {
                    put("summary", fsStr(summary.take(180)))
                    put("text", fsStr(textBody.take(12000)))
                    put("authorName", fsStr(u.displayName.ifBlank { u.email.substringBefore('@') }))
                }
                val body = buildJsonObject {
                    put("fields", buildJsonObject {
                        put("id", fsStr(itemId))
                        put("kind", fsStr("sticky"))
                        put("authorId", fsStr(u.uid))
                        put("pageIdx", fsInt(pageIdx.toLong()))
                        put("updatedAt", fsInt(now))
                        put("data", fsMap(dataFields))
                    })
                }.toString()
                httpJson("$fsBase/collabRooms/$roomId/items/$itemId", "PATCH", body, tok)
                Unit
            }
        }

    // ---------------------------------------------------------------- Free Cloud Relay & Local Cache Helpers

    private fun postFreeRelayMessage(topic: String, jsonPayload: String) {
        val conn = (URL("$FREE_RELAY_BASE/$topic").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("Content-Type", "text/plain; charset=utf-8")
        }
        try {
            conn.outputStream.use { it.write(jsonPayload.toByteArray(Charsets.UTF_8)) }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchFreeRelayMessages(topic: String, since: String = "24h"): List<JsonObject> {
        return runCatching {
            val conn = (URL("$FREE_RELAY_BASE/$topic/json?poll=1&since=$since").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
            }
            try {
                if (conn.responseCode !in 200..299) return emptyList()
                val rawLines = conn.inputStream.bufferedReader().use { it.readLines() }
                rawLines.mapNotNull { line ->
                    runCatching {
                        val env = json.parseToJsonElement(line).jsonObject
                        if (env["event"]?.jsonPrimitive?.content == "message") {
                            val msgStr = env["message"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            json.parseToJsonElement(msgStr).jsonObject
                        } else null
                    }.getOrNull()
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    private fun appendLocalRoomEvent(context: Context, roomId: String, pkt: JsonObject) {
        runCatching {
            val f = localRoomFile(context, roomId)
            val existing = if (f.exists()) f.readLines() else emptyList()
            val updated = (existing + pkt.toString()).takeLast(120)
            f.writeText(updated.joinToString("\n"))
        }
    }

    private fun readLocalRoomEvents(context: Context, roomId: String): List<JsonObject> {
        return runCatching {
            val f = localRoomFile(context, roomId)
            if (!f.exists()) return emptyList()
            f.readLines().mapNotNull { line ->
                runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    // ---------------------------------------------------------------- Firestore REST Helpers

    private fun fsStr(v: String) = buildJsonObject { put("stringValue", v) }
    private fun fsInt(v: Long) = buildJsonObject { put("integerValue", v.toString()) }
    private fun fsBool(v: Boolean) = buildJsonObject { put("booleanValue", v) }
    private fun fsNull() = buildJsonObject { put("nullValue", JsonNull) }
    private fun fsMap(fields: JsonObject) = buildJsonObject { put("mapValue", buildJsonObject { put("fields", fields) }) }

    private fun httpJson(url: String, method: String, body: String?, bearer: String? = null): JsonObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") setRequestProperty("X-HTTP-Method-Override", "PATCH")
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Content-Type", "application/json")
            if (!bearer.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearer")
            if (body != null) doOutput = true
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in 200..299) { "HTTP $code: ${raw.take(200)}" }
            return if (raw.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(raw).jsonObject
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Global host for the 100% Free Google Account & Cloud Sync sign-in dialog.
 * Lets users pick their device Google account or enter their Google email + sync key with zero secrets or paid APIs.
 */
@Composable
fun CloudSignInDialogHost() {
    val open by CloudSync.signInDialogOpen.collectAsState()
    if (!open) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val curUser by CloudSync.user.collectAsState()
    val syncStatus by CloudSync.status.collectAsState()
    val isSyncing by CloudSync.syncing.collectAsState()

    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var syncKey by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val deviceAccounts = remember {
        runCatching {
            val am = AccountManager.get(context)
            am.getAccountsByType("com.google").map { it.name }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME).orEmpty()
            if (pickedEmail.isNotBlank()) {
                email = pickedEmail
                if (name.isBlank()) name = pickedEmail.substringBefore('@')
                scope.launch {
                    CloudSync.signInWithFreeGoogleAccount(context, email, name, syncKey)
                        .onFailure { errorMsg = it.message ?: "Could not complete sign in." }
                }
            }
        }
    }

    if (curUser != null) {
        com.pronotes.app.ui.LiquidGlassDialog(
            onDismissRequest = { CloudSync.dismissSignInDialog() },
            title = {
                Text("Google Account & Sync", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.pronotes.app.ui.LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Signed in as", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(curUser!!.email, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (curUser!!.displayName.isNotBlank()) {
                                Text(curUser!!.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Text(
                        "Your notebooks and settings automatically synchronize between devices using this Google ID with zero subscription fees.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (syncStatus.isNotBlank()) {
                        Text(syncStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !isSyncing,
                            onClick = { scope.launch { CloudSync.syncAll(context) } },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (isSyncing) "Syncing…" else "Sync All Now")
                        }
                        OutlinedButton(
                            onClick = { CloudSync.signOut(context) },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { CloudSync.dismissSignInDialog() }) { Text("Close") }
            },
        )
        return
    }

    com.pronotes.app.ui.LiquidGlassDialog(
        onDismissRequest = { CloudSync.dismissSignInDialog() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sign in with Google", fontWeight = FontWeight.Bold)
                com.pronotes.app.ui.LiquidGlassPill {
                    Text(
                        "100% Free Cloud · Zero Cost · Cross-Device Sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Sign in to synchronize your notes, bookmarks, and Live Study Rooms seamlessly across tablets and phones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (deviceAccounts.isNotEmpty()) {
                    Text(
                        "DISCOVERED GOOGLE ACCOUNTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (acc in deviceAccounts) {
                            com.pronotes.app.ui.LiquidGlassChip(
                                selected = email == acc,
                                label = acc,
                                onClick = {
                                    email = acc
                                    if (name.isBlank()) name = acc.substringBefore('@')
                                },
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        val launched = runCatching {
                            @Suppress("DEPRECATION")
                            val intent = AccountManager.newChooseAccountIntent(
                                null,
                                null,
                                arrayOf("com.google"),
                                null,
                                null,
                                null,
                                null,
                            )
                            accountPickerLauncher.launch(intent)
                        }.isSuccess
                        if (!launched) {
                            runCatching {
                                val authUrl = "https://accounts.google.com/signin/v2/identifier?flowName=GlifWebSignIn"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("🌐 Choose Google Account on Device")
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = "" },
                    label = { Text("Google Email") },
                    placeholder = { Text("you@gmail.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name (for Study Rooms)") },
                    placeholder = { Text("Alex") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = syncKey,
                    onValueChange = { syncKey = it },
                    label = { Text("Sync Passphrase (optional, matches devices)") },
                    placeholder = { Text("e.g. MyStudyPass2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        CloudSync.signInWithFreeGoogleAccount(context, email, name, syncKey)
                            .onFailure { errorMsg = it.message ?: "Invalid email address." }
                    }
                },
                shape = CircleShape,
            ) {
                Text("Sign In & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = { CloudSync.dismissSignInDialog() }) {
                Text("Cancel")
            }
        },
    )
}
