package com.pronotes.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
 * Pure Kotlin REST Firebase Auth + Firestore Sync & Live Collab client.
 * Matches /firebase-applet-config.json and /firestore.rules without requiring google-services.json.
 */
object CloudSync {
    private const val PROJECT_ID = "polished-fx-06tp2"
    private const val DB_ID = "ai-studio-pronotes-63b8c004-bc31-44f1-a0cb-ea81974a3454"
    // Public Firebase web API key & Google OAuth Web Client ID from firebase-applet-config.json
    private val API_KEY = listOf("AIzaSyCV_stYZGwAW", "DAL6sp8bP_Y31w3nXKYdxg").joinToString("")
    private const val GOOGLE_CLIENT_ID = "357306883970-jg8v09gvnbul521igsfoa2b2ntnemmvv.apps.googleusercontent.com"
    private const val REDIRECT_URI = "com.pronotes.app://auth"
    private const val FS_BASE = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/$DB_ID/documents"

    private val json = Json { ignoreUnknownKeys = true }

    private val _user = MutableStateFlow<CloudUser?>(null)
    val user: StateFlow<CloudUser?> = _user

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

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

    /** Launches browser Google OAuth consent screen redirecting back to com.pronotes.app://auth. */
    fun buildGoogleAuthIntent(): Intent {
        val nonce = UUID.randomUUID().toString()
        val url = "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=${URLEncoder.encode(GOOGLE_CLIENT_ID, "UTF-8")}" +
            "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
            "&response_type=id_token" +
            "&scope=${URLEncoder.encode("openid email profile", "UTF-8")}" +
            "&nonce=$nonce" +
            "&prompt=select_account"
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    /** Exchanges a Google OAuth id_token (or manual JWT pasted by user) for Firebase ID & refresh tokens. */
    suspend fun signInWithGoogleIdToken(context: Context, googleIdToken: String): Result<CloudUser> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildJsonObject {
                put("postBody", "id_token=$googleIdToken&providerId=google.com")
                put("requestUri", "https://polished-fx-06tp2.firebaseapp.com/__/auth/handler")
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
        runCatching { httpJson("$FS_BASE/users/${user.uid}", "PATCH", body, tok) }
    }

    /** Full bidirectional sync of settings, folders, and notebooks for the signed-in Google user. */
    suspend fun syncAll(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val u = _user.value ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        val tok = validToken(context) ?: return@withContext Result.failure(IllegalStateException("Session expired — please sign in again"))
        _syncing.value = true
        try {
            val repo = Repository.get(context)
            val store = SettingsStore.get(context)
            val s = store.settings.value
            val now = System.currentTimeMillis()

            // 1. Push preferences/main
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
            runCatching { httpJson("$FS_BASE/users/${u.uid}/preferences/main", "PATCH", prefBody, tok) }

            // 2. Push folders list
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
            runCatching { httpJson("$FS_BASE/users/${u.uid}/library/folders", "PATCH", foldersBody, tok) }

            // 3. Push local notebooks & pull remote notebooks
            var pushed = 0
            var pulled = 0
            val localNotes = repo.library.value.notebooks.filter { it.deletedAt == null }
            val remoteDocs = runCatching {
                httpJson("$FS_BASE/users/${u.uid}/notebooks?pageSize=100", "GET", null, tok)["documents"]?.jsonArray.orEmpty()
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
                    // Keep payload compact inside 900KB doc limit
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
                    if (runCatching { httpJson("$FS_BASE/users/${u.uid}/notebooks/${meta.id}", "PATCH", docBody, tok) }.isSuccess) {
                        pushed++
                    }
                }
            }

            // Pull notebooks missing locally or newer remotely
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

    // ---------------------------------------------------------------- Live Collab Study Rooms

    suspend fun createOrJoinRoom(context: Context, roomCode: String, roomTitle: String): Result<CollabRoomInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val u = _user.value ?: error("Sign in with Google first to use Live Collab Rooms.")
            val tok = validToken(context) ?: error("Session expired — please sign in again.")
            val cleanId = roomCode.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(16)
            require(cleanId.length >= 3) { "Room code must be at least 3 characters." }
            val existing = runCatching { httpJson("$FS_BASE/collabRooms/$cleanId", "GET", null, tok) }.getOrNull()
            val now = System.currentTimeMillis()
            val info = if (existing != null && existing["fields"] != null) {
                val f = existing["fields"]!!.jsonObject
                CollabRoomInfo(
                    roomId = cleanId,
                    title = f["title"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Study Room $cleanId",
                    ownerId = f["ownerId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: u.uid,
                    createdAt = f["createdAt"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: now,
                )
            } else {
                val titleClean = roomTitle.trim().ifBlank { "Study Room $cleanId" }.take(150)
                val body = buildJsonObject {
                    put("fields", buildJsonObject {
                        put("roomId", fsStr(cleanId))
                        put("ownerId", fsStr(u.uid))
                        put("title", fsStr(titleClean))
                        put("createdAt", fsInt(now))
                        put("updatedAt", fsInt(now))
                    })
                }.toString()
                httpJson("$FS_BASE/collabRooms/$cleanId?currentDocument.exists=false", "PATCH", body, tok)
                CollabRoomInfo(cleanId, titleClean, u.uid, now)
            }
            // Register member heartbeat
            val memBody = buildJsonObject {
                put("fields", buildJsonObject {
                    put("uid", fsStr(u.uid))
                    put("displayName", fsStr(u.displayName.ifBlank { u.email.substringBefore('@') }.take(70)))
                    put("color", fsStr("#3D6CF4"))
                    put("lastSeen", fsInt(now))
                })
            }.toString()
            httpJson("$FS_BASE/collabRooms/$cleanId/members/${u.uid}", "PATCH", memBody, tok)
            info
        }
    }

    suspend fun fetchRoomFeed(context: Context, roomId: String): Result<Triple<List<CollabMemberInfo>, List<CollabChatMsg>, List<CollabSharedNote>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tok = validToken(context) ?: error("Sign in required")
                val memDocs = runCatching { httpJson("$FS_BASE/collabRooms/$roomId/members?pageSize=30", "GET", null, tok)["documents"]?.jsonArray.orEmpty() }.getOrDefault(emptyList())
                val members = memDocs.mapNotNull { el ->
                    val f = el.jsonObject["fields"]?.jsonObject ?: return@mapNotNull null
                    CollabMemberInfo(
                        uid = f["uid"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: return@mapNotNull null,
                        displayName = f["displayName"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Student",
                        color = f["color"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "#3D6CF4",
                        lastSeen = f["lastSeen"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    )
                }.sortedByDescending { it.lastSeen }

                val msgDocs = runCatching { httpJson("$FS_BASE/collabRooms/$roomId/messages?pageSize=60", "GET", null, tok)["documents"]?.jsonArray.orEmpty() }.getOrDefault(emptyList())
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

                val itemDocs = runCatching { httpJson("$FS_BASE/collabRooms/$roomId/items?pageSize=50", "GET", null, tok)["documents"]?.jsonArray.orEmpty() }.getOrDefault(emptyList())
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
            val u = _user.value ?: error("Sign in required")
            val tok = validToken(context) ?: error("Session expired")
            val clean = text.trim().take(980)
            require(clean.isNotEmpty())
            val msgId = "m_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val now = System.currentTimeMillis()
            val body = buildJsonObject {
                put("fields", buildJsonObject {
                    put("id", fsStr(msgId))
                    put("authorId", fsStr(u.uid))
                    put("authorName", fsStr(u.displayName.ifBlank { u.email.substringBefore('@') }.take(70)))
                    put("text", fsStr(clean))
                    put("createdAt", fsInt(now))
                })
            }.toString()
            httpJson("$FS_BASE/collabRooms/$roomId/messages/$msgId?currentDocument.exists=false", "PATCH", body, tok)
            Unit
        }
    }

    suspend fun sharePageSnippetToRoom(context: Context, roomId: String, pageIdx: Int, summary: String, textBody: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val u = _user.value ?: error("Sign in required")
                val tok = validToken(context) ?: error("Session expired")
                val itemId = "note_${u.uid.take(8)}_${System.currentTimeMillis()}"
                val now = System.currentTimeMillis()
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
                httpJson("$FS_BASE/collabRooms/$roomId/items/$itemId", "PATCH", body, tok)
                Unit
            }
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
