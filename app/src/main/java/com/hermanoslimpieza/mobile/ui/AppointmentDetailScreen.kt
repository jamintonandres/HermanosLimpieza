package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermanoslimpieza.mobile.ui.theme.BrandBlue
import com.hermanoslimpieza.mobile.ui.theme.BrandYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(state: AppUiState, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val a = state.selectedAppointment
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BrandBlue,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            title = { Text(if (a == null) "Servicio" else "Servicio #${a.id}", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
            actions = {
                if (a != null && state.user?.role == "admin") {
                    IconButton(onClick = { onEdit(a.id) }) { Icon(Icons.Default.Edit, "Editar servicio") }
                }
            }
        )
        if (a == null) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = BrandYellow)
            else Text("No se pudo cargar el servicio.", Modifier.padding(16.dp))
            return
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(a.client_name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
                            StatusPill(a.status)
                        }
                        Text("📞 ${a.client_phone}")
                        Text("🕒 ${formatDateTime(a.scheduled_start)}")
                        Text("🧼 ${a.service_description}")
                        Text("📍 ${a.address.orEmpty()}${a.city?.let { ", $it" } ?: ""}")
                        Text("👤 ${a.assigned_user_name.orEmpty()}")
                        Text("💵 ${money(a.price)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = BrandBlue)
                        if (!a.notes.isNullOrBlank()) {
                            HorizontalDivider()
                            Text("Notas", fontWeight = FontWeight.Bold)
                            Text(a.notes)
                        }
                    }
                }
            }
            if (state.user?.role == "admin") {
                item {
                    Button(
                        onClick = { onEdit(a.id) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandYellow, contentColor = BrandBlue)
                    ) {
                        Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Editar servicio", fontWeight = FontWeight.Black)
                    }
                }
            }
            if (state.history.isNotEmpty()) {
                item { Text("Historial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = BrandBlue) }
                state.history.forEachIndexed { index, h ->
                    item(key = "h$index") {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(h.new_status, fontWeight = FontWeight.Bold)
                                h.note?.let { Text(it) }
                                Text("${h.created_at} · ${h.changed_by_name.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
