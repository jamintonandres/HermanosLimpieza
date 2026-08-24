package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hermanoslimpieza.mobile.data.AppointmentDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onOpen: (Long) -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Servicios de hoy")
                    Text(
                        LocalDate.now().format(
                            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO"))
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") }
                IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Salir") }
            }
        )
        if (state.loading && state.today.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (state.today.isEmpty() && !state.loading) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Text("No hay servicios programados para hoy.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.today, key = { it.id }) { appt ->
                    AppointmentCard(appt, onOpen)
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(appt: AppointmentDto, onOpen: (Long) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(appt.id) }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(appt.client_name, style = MaterialTheme.typography.titleMedium)
                StatusPill(appt.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(formatDateTime(appt.scheduled_start), style = MaterialTheme.typography.labelLarge)
            Text(appt.service_description, style = MaterialTheme.typography.bodyMedium)
            if (!appt.address.isNullOrBlank()) Text("📍 ${appt.address}${appt.city?.let { ", $it" } ?: ""}")
            if (!appt.assigned_user_name.isNullOrBlank()) Text("👤 ${appt.assigned_user_name}")
            Text("Valor: ${money(appt.price)}", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val label = when (status) {
        "assigned_unconfirmed" -> "Creado"
        "assigned_confirmed" -> "Aceptado"
        "completed" -> "Completado"
        "cancelled" -> "Cancelado"
        else -> status
    }
    SuggestionChip(onClick = {}, label = { Text(label) })
}

fun money(value: Double): String = "\$" + "%,.0f".format(Locale.US, value)
fun formatDateTime(raw: String): String = runCatching {
    val dt = java.time.LocalDateTime.parse(raw.replace(" ", "T"))
    dt.format(DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale("es", "CO")))
}.getOrDefault(raw)
