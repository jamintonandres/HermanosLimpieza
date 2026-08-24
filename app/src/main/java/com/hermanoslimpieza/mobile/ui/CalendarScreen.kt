package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: AppUiState,
    onMonth: (YearMonth) -> Unit,
    onOpen: (Long) -> Unit
) {
    val monthLabel = state.month.atDay(1).format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CO"))
    )
    val grouped = state.calendar.groupBy { it.scheduled_start.take(10) }.toSortedMap()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(monthLabel.replaceFirstChar { it.uppercase() }) },
            navigationIcon = {
                IconButton(onClick = { onMonth(state.month.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, "Mes anterior")
                }
            },
            actions = {
                IconButton(onClick = { onMonth(state.month.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, "Mes siguiente")
                }
            }
        )

        if (grouped.isEmpty()) {
            Text("No hay servicios este mes.", Modifier.padding(20.dp))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (date, appointments) ->
                    item(key = "h$date") {
                        Text(
                            prettyDate(date),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(appointments, key = { "a${it.id}" }) { appt ->
                        AppointmentCard(appt, onOpen)
                    }
                }
            }
        }
    }
}

private fun prettyDate(raw: String): String = runCatching {
    java.time.LocalDate.parse(raw).format(
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO"))
    ).replaceFirstChar { it.uppercase() }
}.getOrDefault(raw)
