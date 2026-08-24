package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hermanoslimpieza.mobile.data.MessageDto

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

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ContactAvatar(
                        state.chatContext?.avatar ?: state.selectedChat?.avatar,
                        state.chatContext?.name ?: state.selectedChat?.name.orEmpty()
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(state.chatContext?.name ?: state.selectedChat?.name ?: "Chat")
                        state.chatContext?.phone?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            },
            actions = {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = onAnalyze, enabled = !state.loading) {
                Icon(Icons.Default.Analytics, null)
                Spacer(Modifier.width(6.dp))
                Text("Analizar")
            }
            if (state.extracted != null) {
                Button(onClick = onCreateFromAi) { Text("Crear servicio") }
            }
        }

        state.extracted?.let { ai ->
            ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Datos detectados por IA", style = MaterialTheme.typography.titleSmall)
                    if (ai.clientName.isNotBlank()) Text("Cliente: ${ai.clientName}")
                    if (ai.phone.isNotBlank()) Text("Teléfono: ${ai.phone}")
                    if (ai.address.isNotBlank()) Text("Dirección: ${ai.address}")
                    if (ai.city.isNotBlank()) Text("Ciudad: ${ai.city}")
                    if (ai.serviceDescription.isNotBlank()) Text("Servicio: ${ai.serviceDescription}")
                    if (ai.price.isNotBlank()) Text("Valor: ${ai.price}")
                    if (ai.appointmentDate.isNotBlank()) Text("Fecha: ${ai.appointmentDate}")
                    if (ai.appointmentTime.isNotBlank()) Text("Hora: ${ai.appointmentTime}")
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.messages, key = { it.id.ifBlank { "${it.timestamp}-${it.text}" } }) { m ->
                MessageBubble(m)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensaje") },
                maxLines = 4
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = { onSend(message) { message = "" } },
                enabled = message.isNotBlank()
            ) { Icon(Icons.Default.Send, "Enviar") }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.from_me) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = if (message.from_me) 4.dp else 1.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                message.text?.ifBlank { if (message.media == true) "📎 Archivo" else "" }
                    ?: if (message.media == true) "📎 Archivo" else "",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
