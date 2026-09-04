package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hermanoslimpieza.mobile.data.ChatDto
import com.hermanoslimpieza.mobile.ui.theme.BrandBlue
import com.hermanoslimpieza.mobile.ui.theme.BrandYellow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmScreen(state: AppUiState, onRefresh: () -> Unit, onOpen: (ChatDto) -> Unit) {
    LaunchedEffect(Unit) { onRefresh() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BrandBlue,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            title = {
                Column {
                    Text("CRM WhatsApp", fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (state.syncingCrm) "Sincronizando…" else "${state.chats.size} conversaciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f)
                    )
                }
            },
            actions = { IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") } }
        )

        if (state.syncingCrm && state.chats.isNotEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = BrandYellow, trackColor = BrandBlue)
        }
        if (state.loading && state.chats.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth(), color = BrandYellow)

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(state.chats, key = { it.jid }) { chat ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(chat) }.padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactAvatar(chat.resolvedAvatar(), chat.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                chat.name.ifBlank { chat.phone ?: "Contacto" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                formatChatListTime(chat.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (chat.unread > 0) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                chat.last_message.orEmpty().ifBlank { "Sin mensajes" },
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (chat.unread > 0) {
                                Spacer(Modifier.width(8.dp))
                                Badge(containerColor = BrandYellow, contentColor = BrandBlue) {
                                    Text(chat.unread.toString(), fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 80.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun ContactAvatar(url: String?, name: String, size: Int = 52) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = name,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(modifier = Modifier.size(size.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.trim().take(1).uppercase().ifBlank { "?" }, color = BrandBlue, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun formatChatListTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return runCatching {
        val millis = if (timestamp > 9_999_999_999L) timestamp else timestamp * 1000
        val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val today = java.time.LocalDate.now()
        when (zdt.toLocalDate()) {
            today -> zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale("es", "CO")))
            today.minusDays(1) -> "Ayer"
            else -> zdt.format(DateTimeFormatter.ofPattern("d/MM", Locale("es", "CO")))
        }
    }.getOrDefault("")
}
