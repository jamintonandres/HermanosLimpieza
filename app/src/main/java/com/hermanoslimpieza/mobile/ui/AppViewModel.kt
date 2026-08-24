package com.hermanoslimpieza.mobile.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermanoslimpieza.mobile.data.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class AppUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: UserDto? = null,
    val today: List<AppointmentDto> = emptyList(),
    val calendar: List<AppointmentDto> = emptyList(),
    val collaborators: List<CollaboratorDto> = emptyList(),
    val chats: List<ChatDto> = emptyList(),
    val messages: List<MessageDto> = emptyList(),
    val chatContext: ChatContextDto? = null,
    val selectedChat: ChatDto? = null,
    val selectedAppointment: AppointmentDto? = null,
    val history: List<HistoryDto> = emptyList(),
    val extracted: AiExtracted? = null,
    val month: YearMonth = YearMonth.now()
)

class AppViewModel(
    private val api: HermanosApi,
    private val tokenStore: TokenStore
) : ViewModel() {
    var state by mutableStateOf(AppUiState())
        private set

    val hasSession: Boolean get() = !tokenStore.get().isNullOrBlank()

    private fun fail(t: Throwable) {
        state = state.copy(loading = false, error = t.message ?: "Error de conexión.")
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.login(email.trim(), password) }
                .onSuccess { r ->
                    if (r.ok && !r.token.isNullOrBlank()) {
                        tokenStore.save(r.token)
                        state = state.copy(loading = false, user = r.user)
                        onSuccess()
                        refreshCore()
                    } else {
                        state = state.copy(loading = false, error = r.error ?: "No se pudo iniciar sesión.")
                    }
                }.onFailure(::fail)
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { api.logout() }
            tokenStore.clear()
            state = AppUiState()
            onDone()
        }
    }

    fun refreshCore() {
        loadToday()
        loadCollaborators()
        loadCalendar(state.month)
    }

    fun loadToday(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            runCatching { api.todayServices(date.toString()) }
                .onSuccess { r ->
                    state = if (r.ok) state.copy(loading = false, today = r.appointments)
                    else state.copy(loading = false, error = r.error)
                }.onFailure(::fail)
        }
    }

    fun loadCollaborators() {
        viewModelScope.launch {
            runCatching { api.collaborators() }
                .onSuccess { r ->
                    if (r.ok) state = state.copy(collaborators = r.collaborators)
                }
        }
    }

    fun loadCalendar(month: YearMonth) {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        state = state.copy(month = month)
        viewModelScope.launch {
            runCatching { api.calendar(start.toString(), end.toString()) }
                .onSuccess { r ->
                    if (r.ok) state = state.copy(calendar = r.appointments)
                    else state = state.copy(error = r.error)
                }.onFailure(::fail)
        }
    }

    fun loadAppointment(id: Long, onLoaded: () -> Unit = {}) {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.appointmentDetail(id) }
                .onSuccess { r ->
                    if (r.ok && r.appointment != null) {
                        state = state.copy(
                            loading = false,
                            selectedAppointment = r.appointment,
                            history = r.history
                        )
                        onLoaded()
                    } else state = state.copy(loading = false, error = r.error)
                }.onFailure(::fail)
        }
    }

    fun loadChats() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.chats() }
                .onSuccess { r ->
                    state = if (r.ok) state.copy(loading = false, chats = r.chats)
                    else state.copy(loading = false, error = r.error)
                }.onFailure(::fail)
        }
    }

    fun openChat(chat: ChatDto, onLoaded: () -> Unit = {}) {
        state = state.copy(selectedChat = chat, extracted = null, loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.messages(chat.jid, 30) }
                .onSuccess { r ->
                    if (r.ok) {
                        state = state.copy(
                            loading = false,
                            messages = r.messages,
                            chatContext = r.context
                        )
                        runCatching { api.markRead(chat.jid) }
                        onLoaded()
                    } else state = state.copy(loading = false, error = r.error)
                }.onFailure(::fail)
        }
    }

    fun refreshChat() {
        state.selectedChat?.let { openChat(it) }
    }

    fun sendMessage(text: String, onSent: () -> Unit = {}) {
        val chat = state.selectedChat ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { api.sendMessage(chat.jid, text.trim()) }
                .onSuccess { r ->
                    if (r.ok) {
                        onSent()
                        openChat(chat)
                    } else state = state.copy(error = r.error)
                }.onFailure(::fail)
        }
    }

    fun analyzeChat() {
        val chat = state.selectedChat ?: return
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.analyze(chat.jid) }
                .onSuccess { r ->
                    state = if (r.ok && r.data != null)
                        state.copy(loading = false, extracted = r.data.toAiExtracted())
                    else state.copy(loading = false, error = r.error ?: "La IA no devolvió datos.")
                }.onFailure(::fail)
        }
    }

    fun createAppointment(
        draft: ServiceDraft,
        onSuccess: (Long) -> Unit
    ) {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                api.createAppointment(
                    phone = draft.phone,
                    countryCode = draft.countryCode,
                    clientName = draft.clientName,
                    serviceDescription = draft.serviceDescription,
                    assignedUserId = draft.assignedUserId,
                    scheduledDate = draft.date,
                    timeSlot = draft.timeSlot,
                    price = draft.price,
                    address = draft.address,
                    city = draft.city,
                    notes = draft.notes
                )
            }.onSuccess { r ->
                if (r.ok && r.appointment_id != null) {
                    state = state.copy(loading = false)
                    loadToday()
                    loadCalendar(state.month)
                    onSuccess(r.appointment_id)
                } else state = state.copy(loading = false, error = r.error ?: r.warning)
            }.onFailure(::fail)
        }
    }
}

data class ServiceDraft(
    val phone: String = "",
    val countryCode: String = "57",
    val clientName: String = "",
    val serviceDescription: String = "",
    val assignedUserId: Long = 0,
    val date: String = LocalDate.now().toString(),
    val timeSlot: String = "08:00",
    val price: String = "",
    val address: String = "",
    val city: String = "",
    val notes: String = ""
)
