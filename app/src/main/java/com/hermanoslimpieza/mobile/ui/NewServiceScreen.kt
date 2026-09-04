package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hermanoslimpieza.mobile.data.AiExtracted
import com.hermanoslimpieza.mobile.data.AppointmentDto
import com.hermanoslimpieza.mobile.ui.theme.BrandBlue
import com.hermanoslimpieza.mobile.ui.theme.BrandYellow
import java.time.LocalDate
import java.time.LocalDateTime

private val slots = listOf(
    "08:00", "09:00", "10:00", "11:00", "12:00",
    "13:00", "14:00", "15:00", "16:00", "17:00"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceFormScreen(
    state: AppUiState,
    title: String,
    initialAi: AiExtracted?,
    appointment: AppointmentDto?,
    onBack: (() -> Unit)? = null,
    onSave: (ServiceDraft) -> Unit
) {
    val parsedDateTime = remember(appointment?.scheduled_start) {
        appointment?.scheduled_start?.let { runCatching { LocalDateTime.parse(it.replace(" ", "T")) }.getOrNull() }
    }

    var phone by remember(initialAi, appointment) { mutableStateOf(appointment?.client_phone ?: initialAi?.phone.orEmpty()) }
    var countryCode by remember { mutableStateOf("57") }
    var name by remember(initialAi, appointment) { mutableStateOf(appointment?.client_name ?: initialAi?.clientName.orEmpty()) }
    var service by remember(initialAi, appointment) { mutableStateOf(appointment?.service_description ?: initialAi?.serviceDescription.orEmpty()) }
    var collaborator by remember(appointment) { mutableLongStateOf(appointment?.assigned_user_id ?: 0L) }
    var date by remember(initialAi, appointment) {
        mutableStateOf(
            parsedDateTime?.toLocalDate()?.toString()
                ?: initialAi?.appointmentDate?.takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) }
                ?: LocalDate.now().toString()
        )
    }
    var slot by remember(initialAi, appointment) {
        mutableStateOf(
            parsedDateTime?.toLocalTime()?.toString()?.take(5)
                ?: initialAi?.appointmentTime?.take(5)?.takeIf { it in slots }
                ?: "08:00"
        )
    }
    var price by remember(initialAi, appointment) {
        mutableStateOf(appointment?.price?.let { if (it == 0.0) "" else "%.0f".format(it) } ?: initialAi?.price.orEmpty())
    }
    var address by remember(initialAi, appointment) { mutableStateOf(appointment?.address ?: initialAi?.address.orEmpty()) }
    var city by remember(initialAi, appointment) { mutableStateOf(appointment?.city ?: initialAi?.city.orEmpty()) }
    var notes by remember(appointment) { mutableStateOf(appointment?.notes.orEmpty()) }
    var collabExpanded by remember { mutableStateOf(false) }
    var slotExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BrandBlue,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            title = { Text(title, fontWeight = FontWeight.ExtraBold) },
            navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    if (appointment == null) "Datos del servicio" else "Actualiza los datos necesarios",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandBlue,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it.filter(Char::isDigit).take(3) },
                        label = { Text("País") },
                        prefix = { Text("+") },
                        modifier = Modifier.width(90.dp),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }
            item { Field(name, { name = it }, "Cliente *") }
            item { Field(address, { address = it }, "Dirección *") }
            item { Field(city, { city = it }, "Ciudad") }
            item { Field(service, { service = it }, "Servicio *", singleLine = false) }
            item {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Valor") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            item {
                ExposedDropdownMenuBox(expanded = collabExpanded, onExpandedChange = { collabExpanded = !collabExpanded }) {
                    val selected = state.collaborators.firstOrNull { it.id == collaborator }?.name.orEmpty()
                    OutlinedTextField(
                        value = selected,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Colaborador *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(collabExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(expanded = collabExpanded, onDismissRequest = { collabExpanded = false }) {
                        state.collaborators.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { collaborator = c.id; collabExpanded = false })
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha YYYY-MM-DD *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                ExposedDropdownMenuBox(expanded = slotExpanded, onExpandedChange = { slotExpanded = !slotExpanded }) {
                    OutlinedTextField(
                        value = slot,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horario *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(expanded = slotExpanded, onDismissRequest = { slotExpanded = false }) {
                        slots.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { slot = s; slotExpanded = false })
                        }
                    }
                }
            }
            item { Field(notes, { notes = it }, "Notas", singleLine = false) }
            item {
                Button(
                    onClick = {
                        onSave(ServiceDraft(phone, countryCode, name, service, collaborator, date, slot, price, address, city, notes))
                    },
                    enabled = !state.loading && phone.isNotBlank() && name.isNotBlank() && service.isNotBlank() && address.isNotBlank() && collaborator > 0,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandYellow, contentColor = BrandBlue)
                ) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = BrandBlue)
                    else Text(if (appointment == null) "Guardar servicio" else "Guardar cambios", fontWeight = FontWeight.Black)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(16.dp)
    )
}
