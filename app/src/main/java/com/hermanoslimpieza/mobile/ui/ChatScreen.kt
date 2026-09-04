package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermanoslimpieza.mobile.data.MessageDto
import com.hermanoslimpieza.mobile.ui.theme.BrandBlue
import com.hermanoslimpieza.mobile.ui.theme.BrandYellow
import com.hermanoslimpieza.mobile.ui.theme.IncomingBubble
import com.hermanoslimpieza.mobile.ui.theme.OutgoingBubble
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSend: (String, () -> Unit) -> Unit,
    onAnalyze: () -> Unit,
    onCreateFromAi: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BrandBlue,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ContactAvatar(
                        state.chatContext?.resolvedAvatar() ?: state.selectedChat?.resolvedAvatar(),
                        state.chatContext?.name ?: state.selectedChat?.name.orEmpty(),
                        size = 42
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.chatContext?.name ?: state.selectedChat?.name ?: "Chat", fontWeight = FontWeight.Bold, maxLines = 1)
                        val phone = state.chatContext?.phone ?: state.selectedChat?.phone
                        if (!phone.isNullOrBlank()) {
                            Text(phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .7f))
                        }
                    }
                }
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
            actions = { IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") } }
        )

        if (state.syncingCrm) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = BrandYellow, trackColor = BrandBlue)
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onAnalyze,
                enabled = !state.loading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = BrandBlue)
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(6.dp))
                Text("Analizar", fontWeight = FontWeight.Bold)
            }
            if (state.extracted != null) {
                Button(
                    onClick = onCreateFromAi,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandYellow, contentColor = BrandBlue)
                ) { Text("Crear servicio", fontWeight = FontWeight.Black) }
            }
        }

        state.extracted?.let { ai ->
            ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Datos detectados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
                    if (ai.clientName.isNotBlank()) Text("Cliente · ${ai.clientName}")
                    if (ai.phone.isNotBlank()) Text("Teléfono · ${ai.phone}")
                    if (ai.address.isNotBlank()) Text("Dirección · ${ai.address}")
                    if (ai.city.isNotBlank()) Text("Ciudad · ${ai.city}")
                    if (ai.serviceDescription.isNotBlank()) Text("Servicio · ${ai.serviceDescription}")
                    if (ai.price.isNotBlank()) Text("Valor · ${ai.price}")
                    if (ai.appointmentDate.isNotBlank()) Text("Fecha · ${ai.appointmentDate}")
                    if (ai.appointmentTime.isNotBlank()) Text("Hora · ${ai.appointmentTime}")
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (state.loading && state.messages.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = BrandYellow)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(state.messages, key = { it.id.ifBlank { "${it.timestamp}-${it.text}" } }) { m -> MessageBubble(m) }
        }

        Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje") },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { onSend(message) { message = "" } },
                    enabled = message.isNotBlank(),
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandYellow, contentColor = BrandBlue)
                ) { Icon(Icons.Default.Send, "Enviar") }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.from_me) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.from_me) 18.dp else 5.dp,
                bottomEnd = if (message.from_me) 5.dp else 18.dp
            ),
            color = if (message.from_me) OutgoingBubble else IncomingBubble,
            shadowElevation = if (message.from_me) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 315.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val body = message.text?.ifBlank { if (message.hasMedia()) "📎 Archivo adjunto" else "" }
                    ?: if (message.hasMedia()) "📎 Archivo adjunto" else ""
                Text(body, color = if (message.from_me) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(3.dp))
                Text(
                    formatMessageTime(message.timestamp, message.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.from_me) MaterialTheme.colorScheme.onPrimary.copy(alpha = .68f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long, backendTime: String?): String {
    if (!backendTime.isNullOrBlank()) return backendTime.uppercase()
    if (timestamp <= 0) return ""
    return runCatching {
        val millis = if (timestamp > 9_999_999_999L) timestamp else timestamp * 1000
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a", Locale("es", "CO")))
    }.getOrDefault("")
}
