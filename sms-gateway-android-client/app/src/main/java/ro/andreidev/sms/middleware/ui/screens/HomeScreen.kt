package ro.andreidev.sms.middleware.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ro.andreidev.sms.middleware.data.Settings
import ro.andreidev.sms.middleware.data.db.AppDatabase
import ro.andreidev.sms.middleware.data.db.MessageEntity
import ro.andreidev.sms.middleware.service.GatewayForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val scope = rememberCoroutineScope()
    val snap by settings.flow.collectAsState(
        initial = Settings.Snapshot("", "", "", "", true, true, true)
    )
    val serviceRunning by GatewayForegroundService.isRunning.collectAsState()
    var previousServiceRunning by remember { mutableStateOf<Boolean?>(null) }
    val messages by remember {
        val dao = AppDatabase.get(context).messages()
        dao.observeRecent()
    }.collectAsState(initial = emptyList())

    LaunchedEffect(serviceRunning) {
        val previous = previousServiceRunning
        if (previous != null && previous != serviceRunning) {
            Toast.makeText(
                context,
                if (serviceRunning) "Service started" else "Service stopped",
                Toast.LENGTH_SHORT
            ).show()
        }
        previousServiceRunning = serviceRunning
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Middleware") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Backend: ${snap.serverUrl.ifBlank { "not configured" }}")
                    Text("Device: ${snap.deviceName}")
                    Text("Device ID: ${snap.deviceId.ifBlank { "(not generated yet)" }}")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = snap.isConfigured && !serviceRunning,
                    onClick = { GatewayForegroundService.start(context) }
                ) { Text("Start service") }
                OutlinedButton(
                    enabled = serviceRunning,
                    onClick = { GatewayForegroundService.stop(context) }
                ) {
                    Text("Stop service")
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                enabled = messages.isNotEmpty(),
                onClick = {
                    scope.launch {
                        AppDatabase.get(context).messages().clear()
                    }
                }
            ) {
                Text("Clear local messages")
            }
            Spacer(Modifier.height(16.dp))
            Text("Recent messages")
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(messages, key = { it.id }) { msg -> MessageRow(msg) }
            }
        }
    }
}

@Composable
private fun MessageRow(msg: MessageEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "${msg.direction} · ${msg.status} · ${msg.phoneNumber}",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
            )
            Text(msg.content, maxLines = 3)
            msg.reason?.let { Text("Reason: $it", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        }
    }
}
