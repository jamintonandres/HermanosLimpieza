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

data class AppUiState(
    val loading: Boolean = false,
    val syncingCrm: Boolean = false,
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
    private val tokenStore: TokenStore,
    private val chatCache: ChatCache
) : ViewModel() {
    var state by mutableStateOf(AppUiState())
        private set

    val hasSession: Boolean get() = !tokenStore.get().isNullOrBlank()

    private fun fail(t: Throwable) {
        state = state.copy(
            loading = false,
            syncingCrm = false,
            error = t.message ?: "Error de conexión."
        )
    }

    fun clearError() { state = state.copy(error = null) }

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
                    } else state = state.copy(loading = false, error = r.error ?: "No se pudo iniciar sesión.")
                }.onFailure(::fail)
        }
    }

    fun loadMe() {
        viewModelScope.launch {
            runCatching { api.me() }.onSuccess { r ->
                if (r.ok) state = state.copy(user = r.user)
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { api.logout() }
            tokenStore.clear()
            chatCache.clear()
            state = AppUiState()
            onDone()
        }
    }

    fun refreshCore() {
        loadMe()
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
            runCatching { api.collaborators() }.onSuccess { r ->
                if (r.ok) state = state.copy(collaborators = r.collaborators)
            }
        }
    }

    fun loadCalendar(month: YearMonth) {
        state = state.copy(month = month)
        viewModelScope.launch {
            runCatching { api.calendar(month.atDay(1).toString(), month.atEndOfMonth().toString()) }
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
        val cached = chatCache.loadChats()
        state = if (cached.isNotEmpty()) {
            state.copy(chats = cached, loading = false, syncingCrm = true, error = null)
        } else {
            state.copy(loading = true, syncingCrm = true, error = null)
        }
        viewModelScope.launch {
            runCatching { api.chats() }
                .onSuccess { r ->
                    if (r.ok) {
                        chatCache.saveChats(r.chats)
                        state = state.copy(loading = false, syncingCrm = false, chats = r.chats)
                    } else state = state.copy(loading = false, syncingCrm = false, error = r.error)
                }.onFailure(::fail)
        }
    }

    /** Abre primero el caché y sincroniza Chatwoot después. */
    fun openChat(chat: ChatDto, onLoaded: () -> Unit = {}) {
        val (cachedContext, cachedMessages) = chatCache.loadMessages(chat.jid)
        state = state.copy(
            selectedChat = chat,
            extracted = null,
            messages = cachedMessages,
            chatContext = cachedContext,
            loading = cachedMessages.isEmpty(),
            syncingCrm = true,
            error = null
        )
        onLoaded()

        viewModelScope.launch {
            runCatching { api.messages(chat.jid, 50) }
                .onSuccess { r ->
                    if (r.ok) {
                        chatCache.saveMessages(chat.jid, r.context, r.messages)
                        state = state.copy(
                            loading = false,
                            syncingCrm = false,
                            messages = r.messages,
                            chatContext = r.context
                        )
                        runCatching { api.markRead(chat.jid) }
                    } else state = state.copy(loading = false, syncingCrm = false, error = r.error)
                }.onFailure(::fail)
        }
    }

    fun refreshChat() { state.selectedChat?.let { openChat(it) } }

    fun sendMessage(text: String, onSent: () -> Unit = {}) {
        val chat = state.selectedChat ?: return
        if (text.isBlank()) return

        val optimistic = MessageDto(
            id = "local-${System.currentTimeMillis()}",
            text = text.trim(),
            from_me = true,
            timestamp = System.currentTimeMillis() / 1000
        )
        val optimisticMessages = state.messages + optimistic
        state = state.copy(messages = optimisticMessages)
        chatCache.saveMessages(chat.jid, state.chatContext, optimisticMessages)
        onSent()

        viewModelScope.launch {
            runCatching { api.sendMessage(chat.jid, text.trim()) }
                .onSuccess { r ->
                    if (r.ok) {
                        runCatching { api.messages(chat.jid, 50) }.onSuccess { fresh ->
                            if (fresh.ok) {
                                chatCache.saveMessages(chat.jid, fresh.context, fresh.messages)
                                state = state.copy(messages = fresh.messages, chatContext = fresh.context)
                            }
                        }
                    } else {
                        state = state.copy(
                            messages = state.messages.filterNot { it.id == optimistic.id },
                            error = r.error ?: "No se pudo enviar el mensaje."
                        )
                    }
                }.onFailure { t ->
                    state = state.copy(messages = state.messages.filterNot { it.id == optimistic.id })
                    fail(t)
                }
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

    fun createAppointment(draft: ServiceDraft, onSuccess: (Long) -> Unit) {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                api.createAppointment(
                    draft.phone, draft.countryCode, draft.clientName, draft.serviceDescription,
                    draft.assignedUserId, draft.date, draft.timeSlot, draft.price,
                    draft.address, draft.city, draft.notes
                )
            }.onSuccess { r ->
                if (r.ok && r.appointment_id != null) {
                    state = state.copy(loading = false)
                    loadToday(); loadCalendar(state.month)
                    onSuccess(r.appointment_id)
                } else state = state.copy(loading = false, error = r.error ?: r.warning)
            }.onFailure(::fail)
        }
    }

    fun updateAppointment(id: Long, draft: ServiceDraft, onSuccess: (Long) -> Unit) {
        if (state.user?.role != "admin") {
            state = state.copy(error = "Solo el administrador puede editar servicios desde la app.")
            return
        }
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                api.updateAppointment(
                    id, draft.phone, draft.countryCode, draft.clientName, draft.serviceDescription,
                    draft.assignedUserId, draft.date, draft.timeSlot, draft.price,
                    draft.address, draft.city, draft.notes
                )
            }.onSuccess { r ->
                if (r.ok) {
                    state = state.copy(loading = false)
                    loadToday(); loadCalendar(state.month); loadAppointment(id)
                    onSuccess(id)
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
