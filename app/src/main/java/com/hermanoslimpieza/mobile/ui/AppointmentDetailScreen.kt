package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(state: AppUiState, onBack: () -> Unit) {
    val a = state.selectedAppointment
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (a == null) "Servicio" else "Servicio #${a.id}") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            }
        )
        if (a == null) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            else Text("No se pudo cargar el servicio.", Modifier.padding(16.dp))
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(a.client_name, style = MaterialTheme.typography.headlineSmall)
                            StatusPill(a.status)
                        }
                        Text("📞 ${a.client_phone}")
                        Text("🕒 ${formatDateTime(a.scheduled_start)}")
                        Text("🧼 ${a.service_description}")
                        Text("📍 ${a.address.orEmpty()}${a.city?.let { ", $it" } ?: ""}")
                        Text("👤 ${a.assigned_user_name.orEmpty()}")
                        Text("💵 ${money(a.price)}", style = MaterialTheme.typography.titleMedium)
                        if (!a.notes.isNullOrBlank()) Text("Notas: ${a.notes}")
                    }
                }
            }
            if (state.history.isNotEmpty()) {
                item { Text("Historial", style = MaterialTheme.typography.titleMedium) }
                state.history.forEachIndexed { index, h ->
                    item(key = "h$index") {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(h.new_status)
                                h.note?.let { Text(it) }
                                Text(
                                    "${h.created_at} · ${h.changed_by_name.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
