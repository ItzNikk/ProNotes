package com.pronotes.app.editor

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.data.CloudSync
import com.pronotes.app.data.CollabChatMsg
import com.pronotes.app.data.CollabMemberInfo
import com.pronotes.app.data.CollabRoomInfo
import com.pronotes.app.data.CollabSharedFile
import com.pronotes.app.data.CollabSharedNote
import com.pronotes.app.data.Repository
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.GradientTitle
import com.pronotes.app.ui.ProButtonPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveCollabRoomDialog(
    vm: EditorViewModel? = null,
    repo: Repository? = null,
    onOpenNotebook: ((String) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    onClose: () -> Unit = onDismiss,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudUser by CloudSync.user.collectAsStateWithLifecycle()

    var activeRoom by remember { mutableStateOf<CollabRoomInfo?>(null) }
    var members by remember { mutableStateOf<List<CollabMemberInfo>>(emptyList()) }
    var messages by remember { mutableStateOf<List<CollabChatMsg>>(emptyList()) }
    var sharedNotes by remember { mutableStateOf<List<CollabSharedNote>>(emptyList()) }
    var sharedFiles by remember { mutableStateOf<List<CollabSharedFile>>(emptyList()) }
    var savedRooms by remember { mutableStateOf(CloudSync.savedStudyRooms(context)) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Chat, 1 = Files & Drive

    val defaultTitle = remember(vm?.title) {
        val base = vm?.title?.takeIf { it.isNotBlank() } ?: "Study Group"
        "$base · Friends"
    }
    var roomName by remember { mutableStateOf(defaultTitle) }
    var displayName by remember(cloudUser?.displayName) {
        mutableStateOf(cloudUser?.displayName.orEmpty())
    }
    var joinCode by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var driveLinkUrl by remember { mutableStateOf("") }
    var driveLinkLabel by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val myUid = remember(cloudUser?.uid, displayName) {
        CloudSync.ensureRoomParticipant(context, displayName).uid
    }

    suspend fun refreshRoom(roomId: String) {
        CloudSync.fetchRoomFeed(context, roomId)
            .onSuccess { (memList, msgList, noteList) ->
                members = memList
                messages = msgList
                sharedNotes = noteList
            }
        CloudSync.fetchRoomFiles(context, roomId)
            .onSuccess { fileList ->
                sharedFiles = fileList
            }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val room = activeRoom ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            busy = true
            errorMsg = ""
            scope.launch {
                CloudSync.uploadFileToRoom(context, room.roomId, uri)
                    .onSuccess { uploaded ->
                        Toast.makeText(context, "Shared '${uploaded.fileName}' with friends!", Toast.LENGTH_SHORT).show()
                        refreshRoom(room.roomId)
                    }
                    .onFailure { e ->
                        errorMsg = e.message ?: "File upload failed"
                    }
                busy = false
            }
        }
    }

    LaunchedEffect(activeRoom?.roomId) {
        val rid = activeRoom?.roomId ?: return@LaunchedEffect
        while (true) {
            refreshRoom(rid)
            delay(5_000L)
        }
    }

    fun joinOrOpen(code: String, title: String) {
        busy = true
        errorMsg = ""
        scope.launch {
            CloudSync.createOrJoinRoom(
                context = context,
                roomCode = code,
                roomTitle = title,
                preferredName = displayName,
            ).onSuccess { info ->
                CloudSync.rememberStudyRoom(context, info.roomId, info.title)
                savedRooms = CloudSync.savedStudyRooms(context)
                activeRoom = info
                refreshRoom(info.roomId)
            }.onFailure {
                errorMsg = it.message ?: "Could not open study room"
            }
            busy = false
        }
    }

    AppDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GradientTitle(
                    if (activeRoom != null) activeRoom!!.title else "Study Friends & File Share",
                    MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                AppPill(
                    selected = activeRoom != null,
                    tint = if (activeRoom != null) ProButtonPalette.Emerald else ProButtonPalette.ElectricBlue,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (activeRoom != null) activeRoom!!.roomId else "100% Free",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (activeRoom == null) {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Chat live with study friends and send PDFs, notes, images, or Google Drive links for free — no paid server needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Your Study Name") },
                                placeholder = { Text("e.g., Alex") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    if (savedRooms.isNotEmpty()) {
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Recent Study Groups", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                savedRooms.forEach { saved ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                            .clickable { joinOrOpen(saved.roomId, saved.title) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(saved.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("Code: ${saved.roomId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(
                                            onClick = {
                                                CloudSync.forgetStudyRoom(context, saved.roomId)
                                                savedRooms = CloudSync.savedStudyRooms(context)
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Start a New Study Group", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                label = { Text("Group / Room Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                                onClick = { joinOrOpen(CloudSync.generateRoomCode(), roomName) },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (busy) "Starting room…" else "Create Study Room & Invite Code") }
                        }
                    }

                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Join a Friend's Room", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase().take(12); errorMsg = "" },
                                label = { Text("Friend's Room Code") },
                                placeholder = { Text("482-910") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedButton(
                                enabled = joinCode.trim().length >= 3 && !busy,
                                onClick = { joinOrOpen(joinCode, "Study Room $joinCode") },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Join Study Friends") }
                        }
                    }
                } else {
                    val room = activeRoom!!
                    val participantNames = if (members.isEmpty()) {
                        CloudSync.ensureRoomParticipant(context, displayName).displayName
                    } else {
                        members.joinToString(", ") { it.displayName }
                    }
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Online (${members.size.coerceAtLeast(1)}): $participantNames",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppChip(
                                    selected = true,
                                    label = "Invite Code",
                                    tint = ProButtonPalette.ElectricBlue,
                                    onClick = {
                                        val share = Intent(Intent.ACTION_SEND)
                                            .setType("text/plain")
                                            .putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join my Pro Notes study chat '${room.title}' with code: ${room.roomId}",
                                            )
                                        context.startActivity(Intent.createChooser(share, "Invite Study Friends"))
                                    },
                                )
                                AppChip(
                                    selected = false,
                                    label = "Refresh",
                                    onClick = { scope.launch { refreshRoom(room.roomId) } },
                                )
                                AppChip(
                                    selected = false,
                                    label = "Leave",
                                    onClick = {
                                        activeRoom = null
                                        members = emptyList()
                                        messages = emptyList()
                                        sharedNotes = emptyList()
                                        sharedFiles = emptyList()
                                    },
                                )
                            }
                        }
                    }

                    AppSegmentedControl(
                        options = listOf("Friends Chat (${messages.size})", "Files & Drive (${sharedFiles.size})"),
                        selectedIndex = activeTab,
                        onSelect = { activeTab = it },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (activeTab == 0) {
                        // Friends Chat tab with styled bubbles
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (messages.isEmpty()) {
                                    Text(
                                        "Say hi to your study friends! Share questions, formulas, or study notes below.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        messages.takeLast(20).forEach { msg ->
                                            val isMe = msg.authorId == myUid
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                            ) {
                                                Text(
                                                    text = if (isMe) "You" else msg.authorName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(
                                                            RoundedCornerShape(
                                                                topStart = 16.dp,
                                                                topEnd = 16.dp,
                                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                                bottomEnd = if (isMe) 4.dp else 16.dp,
                                                            ),
                                                        )
                                                        .background(
                                                            if (isMe) ProButtonPalette.ElectricBlue
                                                            else MaterialTheme.colorScheme.surfaceVariant,
                                                        )
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                ) {
                                                    Text(
                                                        text = msg.text,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    OutlinedTextField(
                                        value = chatInput,
                                        onValueChange = { chatInput = it },
                                        placeholder = { Text("Message study friends…") },
                                        maxLines = 3,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Button(
                                        enabled = chatInput.isNotBlank() && !busy,
                                        colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                                        onClick = {
                                            busy = true
                                            val toSend = chatInput.trim()
                                            scope.launch {
                                                CloudSync.sendRoomMessage(context, room.roomId, toSend)
                                                    .onSuccess {
                                                        chatInput = ""
                                                        refreshRoom(room.roomId)
                                                    }
                                                    .onFailure { errorMsg = it.message ?: "Failed to send message" }
                                                busy = false
                                            }
                                        },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                    ) {
                                        Icon(Icons.Outlined.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                                    }
                                }

                                val latestSnippet = messages.lastOrNull()?.text.orEmpty()
                                if (vm != null && latestSnippet.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            vm.insertTextToPage(latestSnippet)
                                            Toast.makeText(context, "Inserted latest message onto page", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("Insert Latest Message to Page") }
                                } else if (repo != null && onOpenNotebook != null && latestSnippet.isNotBlank()) {
                                    OutlinedButton(
                                        enabled = !busy,
                                        onClick = {
                                            busy = true
                                            scope.launch {
                                                val transcript = messages.joinToString("\n") { "${it.authorName}: ${it.text}" }
                                                val id = repo.importStructuredText(room.title, transcript, null)
                                                busy = false
                                                onOpenNotebook(id)
                                            }
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("Save Chat as Notebook") }
                                }
                            }
                        }
                    } else {
                        // Files & Google Drive Share tab
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Send a File Free (up to 15 MB)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Pick any PDF, photo, slides, or Pro Notes backup — your friends in this room get an instant free download link.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Button(
                                    enabled = !busy,
                                    colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.Emerald, contentColor = Color.White),
                                    onClick = { filePicker.launch(arrayOf("*/*")) },
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (busy) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Uploading…")
                                    } else {
                                        Icon(Icons.Outlined.CloudUpload, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Upload & Send File Free")
                                    }
                                }
                            }
                        }

                        AppCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Share Google Drive / Cloud Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = driveLinkLabel,
                                    onValueChange = { driveLinkLabel = it },
                                    label = { Text("File title (optional)") },
                                    placeholder = { Text("e.g., Chapter 4 Physics PDF") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = driveLinkUrl,
                                    onValueChange = { driveLinkUrl = it },
                                    label = { Text("Google Drive or File URL") },
                                    placeholder = { Text("https://drive.google.com/file/d/...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        enabled = driveLinkUrl.trim().length >= 8 && !busy,
                                        onClick = {
                                            busy = true
                                            errorMsg = ""
                                            scope.launch {
                                                CloudSync.shareDriveOrUrlToRoom(context, room.roomId, driveLinkLabel, driveLinkUrl)
                                                    .onSuccess {
                                                        driveLinkLabel = ""
                                                        driveLinkUrl = ""
                                                        refreshRoom(room.roomId)
                                                        Toast.makeText(context, "Drive link posted to room!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .onFailure { errorMsg = it.message ?: "Could not share link" }
                                                busy = false
                                            }
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Post Link") }
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/my-drive"))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                                )
                                            }
                                        },
                                        shape = CircleShape,
                                    ) {
                                        Text("Open Drive")
                                    }
                                }
                            }
                        }

                        if (sharedFiles.isNotEmpty()) {
                            AppCard(Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Shared in This Room", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    sharedFiles.forEach { file ->
                                        val sizeLabel = if (file.sizeBytes > 0L) {
                                            val kb = file.sizeBytes / 1024f
                                            if (kb >= 1024f) "%.1f MB".format(kb / 1024f) else "${kb.toInt().coerceAtLeast(1)} KB"
                                        } else if (file.isDriveLink) {
                                            "Google Drive"
                                        } else {
                                            "Link"
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                .clickable {
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, Uri.parse(file.downloadUrl))
                                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                                        )
                                                    }.onFailure {
                                                        Toast.makeText(context, "Could not open file URL", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Outlined.AttachFile,
                                                contentDescription = null,
                                                tint = if (file.isDriveLink) ProButtonPalette.ElectricBlue else ProButtonPalette.Emerald,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                Text("By ${file.authorName} · $sizeLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Icon(Icons.Outlined.OpenInNew, contentDescription = "Open / Download", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
    )
}
