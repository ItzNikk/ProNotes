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
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ProButtonPalette
import kotlinx.coroutines.launch

@Composable
fun LiveCollabRoomDialog(
    vm: EditorViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudUser by CloudSync.user.collectAsStateWithLifecycle()
    val activeRoom by CloudSync.activeRoom.collectAsStateWithLifecycle()
    var joinCode by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("${vm.title} · Study Room") }
    var sharedNoteInput by remember { mutableStateOf("") }
    var readOnly by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    AppDialog(
        onDismissRequest = onDismiss,
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
                        if (activeRoom != null) "Code ${activeRoom!!.code}" else "Multi-Device",
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
                        Text("Sign in with Google first to create or join live study rooms across devices.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = { CloudSync.requestSignIn() },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sign in with Google")
                    }
                } else if (activeRoom == null) {
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
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Read-only for participants", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                AppToggle(checked = readOnly, onCheckedChange = { readOnly = it })
                            }
                            Button(
                                enabled = !busy,
                                onClick = {
                                    busy = true
                                    errorMsg = ""
                                    scope.launch {
                                        CloudSync.createRoom(context, vm.noteId, roomName, readOnly)
                                            .onFailure { errorMsg = it.message ?: "Failed to create room" }
                                        busy = false
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (busy) "Starting room…" else "Start Live Room") }
                        }
                    }

                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Join by Code", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase().take(8); errorMsg = "" },
                                label = { Text("6-Character Room Code") },
                                placeholder = { Text("ABC123") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedButton(
                                enabled = joinCode.length >= 4 && !busy,
                                onClick = {
                                    busy = true
                                    errorMsg = ""
                                    scope.launch {
                                        CloudSync.joinRoom(context, joinCode)
                                            .onFailure { errorMsg = it.message ?: "Room not found" }
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
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(room.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Room Code: ${room.code} · Host: ${room.hostName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Participants (${room.participants.size}): ${room.participants.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppChip(
                                    selected = false,
                                    label = "Copy / Share Invite",
                                    onClick = {
                                        val share = Intent(Intent.ACTION_SEND)
                                            .setType("text/plain")
                                            .putExtra(Intent.EXTRA_TEXT, "Join my Pro Notes study room '${room.title}' with code: ${room.code}")
                                        context.startActivity(Intent.createChooser(share, "Share Room Code"))
                                    },
                                )
                                AppChip(
                                    selected = false,
                                    label = "Leave Room",
                                    onClick = { CloudSync.leaveRoom() },
                                )
                            }
                        }
                    }

                    AppCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Shared Live Scratchpad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                room.sharedSnippet.ifBlank { "No live notes posted yet. Post summary points or questions below!" },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                value = sharedNoteInput,
                                onValueChange = { sharedNoteInput = it },
                                placeholder = { Text("Share an update or study bullet…") },
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
                                        scope.launch {
                                            CloudSync.pushRoomSnippet(context, sharedNoteInput)
                                                .onSuccess { sharedNoteInput = "" }
                                                .onFailure { errorMsg = it.message ?: "Failed" }
                                            busy = false
                                        }
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Broadcast") }
                                OutlinedButton(
                                    enabled = room.sharedSnippet.isNotBlank(),
                                    onClick = {
                                        vm.insertTextToPage(room.sharedSnippet)
                                        Toast.makeText(context, "Inserted room notes onto page", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Insert to Page") }
                            }
                        }
                    }
                }

                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
