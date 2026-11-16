package com.pronotes.app.editor

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.data.CloudSync
import com.pronotes.app.data.CollabChatMsg
import com.pronotes.app.data.CollabMemberInfo
import com.pronotes.app.data.CollabRoomInfo
import com.pronotes.app.data.CollabSharedNote
import com.pronotes.app.data.Repository
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppPill
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

    val defaultTitle = remember(vm?.title) {
        val base = vm?.title?.takeIf { it.isNotBlank() } ?: "Pro Notes"
        "$base · Study Room"
    }
    var roomName by remember { mutableStateOf(defaultTitle) }
    var displayName by remember(cloudUser?.displayName) {
        mutableStateOf(cloudUser?.displayName.orEmpty())
    }
    var joinCode by remember { mutableStateOf("") }
    var sharedNoteInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    suspend fun refreshRoom(roomId: String) {
        CloudSync.fetchRoomFeed(context, roomId)
            .onSuccess { (memList, msgList, noteList) ->
                members = memList
                messages = msgList
                sharedNotes = noteList
            }
    }

    LaunchedEffect(activeRoom?.roomId) {
        val rid = activeRoom?.roomId ?: return@LaunchedEffect
        while (true) {
            refreshRoom(rid)
            delay(6_000L)
        }
    }

    AppDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Live Study Room",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                AppPill(
                    selected = activeRoom != null,
                    tint = if (activeRoom != null) ProButtonPalette.Emerald else ProButtonPalette.ElectricBlue,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (activeRoom != null) "Code ${activeRoom!!.roomId}" else "Multi-Device",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (cloudUser == null) {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "You can join immediately with a display name or sign in with your Google Account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Your Display Name (optional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedButton(
                                onClick = { CloudSync.openSignInDialog() },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sign in with Google")
                            }
                        }
                    }
                }

                if (activeRoom == null) {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Create Room", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                label = { Text("Room Title") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                enabled = !busy,
                                onClick = {
                                    busy = true
                                    errorMsg = ""
                                    scope.launch {
                                        val code = CloudSync.generateRoomCode()
                                        CloudSync.createOrJoinRoom(
                                            context = context,
                                            roomCode = code,
                                            roomTitle = roomName,
                                            preferredName = displayName,
                                        ).onSuccess { info ->
                                            activeRoom = info
                                            refreshRoom(info.roomId)
                                        }.onFailure {
                                            errorMsg = it.message ?: "Failed to create room"
                                        }
                                        busy = false
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (busy) "Starting room..." else "Start Live Room") }
                        }
                    }

                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Join by Code", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase().take(12); errorMsg = "" },
                                label = { Text("Room Code") },
                                placeholder = { Text("482-910") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedButton(
                                enabled = joinCode.trim().length >= 3 && !busy,
                                onClick = {
                                    busy = true
                                    errorMsg = ""
                                    scope.launch {
                                        CloudSync.createOrJoinRoom(
                                            context = context,
                                            roomCode = joinCode,
                                            roomTitle = roomName,
                                            preferredName = displayName,
                                        ).onSuccess { info ->
                                            activeRoom = info
                                            refreshRoom(info.roomId)
                                        }.onFailure {
                                            errorMsg = it.message ?: "Room not found"
                                        }
                                        busy = false
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Join Study Room") }
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
                            Text(room.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Room Code: ${room.roomId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Participants (${members.size.coerceAtLeast(1)}): $participantNames",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppChip(
                                    selected = false,
                                    label = "Share Invite",
                                    onClick = {
                                        val share = Intent(Intent.ACTION_SEND)
                                            .setType("text/plain")
                                            .putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join my Pro Notes study room '${room.title}' with code: ${room.roomId}",
                                            )
                                        context.startActivity(Intent.createChooser(share, "Share Room Code"))
                                    },
                                )
                                AppChip(
                                    selected = false,
                                    label = "Refresh",
                                    onClick = {
                                        scope.launch { refreshRoom(room.roomId) }
                                    },
                                )
                                AppChip(
                                    selected = false,
                                    label = "Leave Room",
                                    onClick = {
                                        activeRoom = null
                                        members = emptyList()
                                        messages = emptyList()
                                        sharedNotes = emptyList()
                                    },
                                )
                            }
                        }
                    }

                    val latestSnippet = sharedNotes.firstOrNull()?.itemsJson?.takeIf { it.isNotBlank() }
                        ?: messages.lastOrNull()?.text.orEmpty()

                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Shared Live Scratchpad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (messages.isEmpty() && sharedNotes.isEmpty()) {
                                Text(
                                    "No live notes posted yet. Post summary points or questions below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    sharedNotes.take(4).forEach { item ->
                                        Text(
                                            text = item.itemsJson.ifBlank { item.summary },
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                    messages.takeLast(5).forEach { msg ->
                                        Text(
                                            text = "${msg.authorName}: ${msg.text}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = sharedNoteInput,
                                onValueChange = { sharedNoteInput = it },
                                placeholder = { Text("Share an update or study bullet...") },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = sharedNoteInput.isNotBlank() && !busy,
                                    onClick = {
                                        busy = true
                                        val toSend = sharedNoteInput
                                        scope.launch {
                                            val pageIndex = vm?.pageIndex ?: 0
                                            CloudSync.sharePageSnippetToRoom(
                                                context = context,
                                                roomId = room.roomId,
                                                pageIdx = pageIndex,
                                                summary = room.title,
                                                textBody = toSend,
                                            )
                                            CloudSync.sendRoomMessage(context, room.roomId, toSend)
                                                .onSuccess {
                                                    sharedNoteInput = ""
                                                    refreshRoom(room.roomId)
                                                }
                                                .onFailure { errorMsg = it.message ?: "Failed to broadcast" }
                                            busy = false
                                        }
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Broadcast") }
                                if (vm != null) {
                                    OutlinedButton(
                                        enabled = latestSnippet.isNotBlank(),
                                        onClick = {
                                            vm.insertTextToPage(latestSnippet)
                                            Toast.makeText(context, "Inserted room notes onto page", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Insert to Page") }
                                } else if (repo != null && onOpenNotebook != null) {
                                    OutlinedButton(
                                        enabled = latestSnippet.isNotBlank() && !busy,
                                        onClick = {
                                            busy = true
                                            scope.launch {
                                                val id = repo.importStructuredText(room.title, latestSnippet, null)
                                                busy = false
                                                onOpenNotebook(id)
                                            }
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Open as Note") }
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
