package com.google.devfest.safeaichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.devfest.safeaichat.data.Message
import com.google.devfest.safeaichat.data.SafetyLevel
import com.google.devfest.safeaichat.data.SafetySettings
import com.google.devfest.safeaichat.ui.ChatViewModel
import com.google.devfest.safeaichat.ui.theme.SafeAIChatDemoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeAIChatDemoTheme {
                ChatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safe AI Chat Demo") },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status indicators
            if (uiState.showPrivacyIndicator || uiState.showOfflineIndicator) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState.showPrivacyIndicator) {
                            StatusChip(
                                icon = Icons.Default.Lock,
                                text = "On-Device",
                                color = Color(0xFF1B5E20)
                            )
                        }
                        if (uiState.showOfflineIndicator) {
                            StatusChip(
                                icon = Icons.Default.CloudOff,
                                text = "Offline-Ready",
                                color = Color(0xFF01579B)
                            )
                        }
                        StatusChip(
                            icon = Icons.Default.Shield,
                            text = "Protected",
                            color = Color(0xFF4A148C)
                        )
                    }
                }
            }

            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message)
                }

                if (uiState.isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }

            // Input field
            MessageInput(
                onSendMessage = { viewModel.sendMessage(it) },
                enabled = uiState.isInitialized && !uiState.isLoading
            )
        }

        // Settings dialog
        if (showSettings) {
            SettingsDialog(
                currentSettings = uiState.safetySettings,
                onDismiss = { showSettings = false },
                onSettingsChanged = { viewModel.updateSafetySettings(it) }
            )
        }
    }
}

@Composable
fun StatusChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            color = when {
                message.wasFiltered -> MaterialTheme.colorScheme.errorContainer
                message.isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = when {
                    message.wasFiltered -> MaterialTheme.colorScheme.onErrorContainer
                    message.isUser -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInput(onSendMessage: (String) -> Unit, enabled: Boolean) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask me anything...") },
                enabled = enabled,
                maxLines = 4
            )

            FilledIconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentSettings: SafetySettings,
    onDismiss: () -> Unit,
    onSettingsChanged: (SafetySettings) -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Safety Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Configure content filtering",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Safety level selector
                Text("Safety Level", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SafetyLevel.values().forEach { level ->
                        FilterChip(
                            selected = settings.level == level,
                            onClick = { settings = settings.copy(level = level) },
                            label = { Text(level.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Individual filters
                SwitchRow(
                    text = "Block Harassment",
                    checked = settings.blockHarassment,
                    onCheckedChange = { settings = settings.copy(blockHarassment = it) }
                )
                SwitchRow(
                    text = "Block Hate Speech",
                    checked = settings.blockHateSpeech,
                    onCheckedChange = { settings = settings.copy(blockHateSpeech = it) }
                )
                SwitchRow(
                    text = "Block Sexual Content",
                    checked = settings.blockSexualContent,
                    onCheckedChange = { settings = settings.copy(blockSexualContent = it) }
                )
                SwitchRow(
                    text = "Block Dangerous Content",
                    checked = settings.blockDangerousContent,
                    onCheckedChange = { settings = settings.copy(blockDangerousContent = it) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSettingsChanged(settings)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
