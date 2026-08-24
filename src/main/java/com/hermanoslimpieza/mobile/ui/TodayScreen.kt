package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermanoslimpieza.mobile.data.AppointmentDto
import com.hermanoslimpieza.mobile.ui.theme.BrandBlue
import com.hermanoslimpieza.mobile.ui.theme.BrandYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(state: AppUiState, onRefresh: () -> Unit, onOpen: (Long) -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BrandBlue,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            title = {
                Column {
                    Text("Hola${state.user?.name?.let { ", ${it.substringBefore(" ")}" } ?: ""}", fontWeight = FontWeight.ExtraBold)
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO"))).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f)
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") }
                IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Salir") }
            }
        )

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Servicios", state.today.size.toString(), Modifier.weight(1f))
            MetricCard("Completados", state.today.count { it.status == "completed" }.toString(), Modifier.weight(1f))
        }

        Text(
            "Servicios de hoy",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = BrandBlue
        )

        if (state.loading && state.today.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth(), color = BrandYellow)
        if (state.today.isEmpty() && !state.loading) {
            Text("No hay servicios programados para hoy.", Modifier.padding(20.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.today, key = { it.id }) { appt -> AppointmentCard(appt, onOpen) }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = BrandBlue)
        }
    }
}

@Composable
fun AppointmentCard(appt: AppointmentDto, onOpen: (Long) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(appt.id) },
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(appt.client_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
                    Text(formatDateTime(appt.scheduled_start), fontWeight = FontWeight.SemiBold)
                }
                StatusPill(appt.status)
            }
            Spacer(Modifier.height(10.dp))
            Text(appt.service_description, style = MaterialTheme.typography.bodyLarge)
            if (!appt.address.isNullOrBlank()) Text("📍 ${appt.address}${appt.city?.let { ", $it" } ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!appt.assigned_user_name.isNullOrBlank()) Text("👤 ${appt.assigned_user_name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Servicio #${appt.id}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(money(appt.price), fontWeight = FontWeight.Black, color = BrandBlue)
            }
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
    Surface(
        color = if (status == "completed") BrandYellow else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BrandBlue)
    }
}

fun money(value: Double): String = "$" + "%,.0f".format(Locale.US, value)
fun formatDateTime(raw: String): String = runCatching {
    java.time.LocalDateTime.parse(raw.replace(" ", "T")).format(DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale("es", "CO")))
}.getOrDefault(raw)
