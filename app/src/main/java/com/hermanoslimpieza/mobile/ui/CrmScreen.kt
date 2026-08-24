package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hermanoslimpieza.mobile.data.ChatDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onOpen: (ChatDto) -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("CRM WhatsApp") },
            actions = {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        )
        if (state.loading && state.chats.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn {
            items(state.chats, key = { it.jid }) { chat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(chat) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactAvatar(chat.avatar, chat.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(chat.name.ifBlank { chat.phone ?: "Contacto" }, style = MaterialTheme.typography.titleSmall)
                            if (chat.unread > 0) Badge { Text(chat.unread.toString()) }
                        }
                        Text(
                            chat.last_message.orEmpty(),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ContactAvatar(url: String?, name: String) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = name,
            modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.extraLarge),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.trim().take(1).uppercase().ifBlank { "?" })
            }
        }
    }
}
