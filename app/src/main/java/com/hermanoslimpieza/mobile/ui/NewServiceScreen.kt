package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hermanoslimpieza.mobile.data.AiExtracted
import java.time.LocalDate

private val slots = listOf(
    "08:00","09:00","10:00","11:00","12:00",
    "13:00","14:00","15:00","16:00","17:00"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewServiceScreen(
    state: AppUiState,
    initial: AiExtracted?,
    onSave: (ServiceDraft) -> Unit
) {
    var phone by remember(initial) { mutableStateOf(initial?.phone.orEmpty()) }
    var countryCode by remember { mutableStateOf("57") }
    var name by remember(initial) { mutableStateOf(initial?.clientName.orEmpty()) }
    var service by remember(initial) { mutableStateOf(initial?.serviceDescription.orEmpty()) }
    var collaborator by remember { mutableLongStateOf(0L) }
    var date by remember(initial) {
        mutableStateOf(
            initial?.appointmentDate?.takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) }
                ?: LocalDate.now().toString()
        )
    }
    var slot by remember(initial) {
        mutableStateOf(initial?.appointmentTime?.take(5)?.takeIf { it in slots } ?: "08:00")
    }
    var price by remember(initial) { mutableStateOf(initial?.price.orEmpty()) }
    var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }
    var city by remember(initial) { mutableStateOf(initial?.city.orEmpty()) }
    var notes by remember { mutableStateOf("") }
    var collabExpanded by remember { mutableStateOf(false) }
    var slotExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(if (initial == null) "Nuevo servicio" else "Servicio desde CRM") })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it.filter(Char::isDigit).take(3) },
                        label = { Text("País") },
                        prefix = { Text("+") },
                        modifier = Modifier.width(90.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp *") },
                        modifier = Modifier.weight(1f),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = collabExpanded,
                    onExpandedChange = { collabExpanded = !collabExpanded }
                ) {
                    val selected = state.collaborators.firstOrNull { it.id == collaborator }?.name.orEmpty()
                    OutlinedTextField(
                        value = selected,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Colaborador *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(collabExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = collabExpanded,
                        onDismissRequest = { collabExpanded = false }
                    ) {
                        state.collaborators.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    collaborator = c.id
                                    collabExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha YYYY-MM-DD *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = slotExpanded,
                    onExpandedChange = { slotExpanded = !slotExpanded }
                ) {
                    OutlinedTextField(
                        value = slot,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horario *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = slotExpanded,
                        onDismissRequest = { slotExpanded = false }
                    ) {
                        slots.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    slot = s
                                    slotExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item { Field(notes, { notes = it }, "Notas", singleLine = false) }
            item {
                Button(
                    onClick = {
                        onSave(
                            ServiceDraft(
                                phone = phone,
                                countryCode = countryCode,
                                clientName = name,
                                serviceDescription = service,
                                assignedUserId = collaborator,
                                date = date,
                                timeSlot = slot,
                                price = price,
                                address = address,
                                city = city,
                                notes = notes
                            )
                        )
                    },
                    enabled = !state.loading &&
                        phone.isNotBlank() && name.isNotBlank() && service.isNotBlank() &&
                        address.isNotBlank() && collaborator > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Guardar servicio")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3
    )
}
