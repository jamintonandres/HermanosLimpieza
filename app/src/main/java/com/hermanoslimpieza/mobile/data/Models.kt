package com.hermanoslimpieza.mobile.data

import com.google.gson.JsonObject

data class UserDto(
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val role: String = ""
)

data class LoginResponse(
    val ok: Boolean = false,
    val token: String? = null,
    val expires_at: String? = null,
    val user: UserDto? = null,
    val error: String? = null
)

data class MeResponse(
    val ok: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null
)

data class BasicResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val warning: String? = null
)

data class CollaboratorDto(val id: Long = 0, val name: String = "")

data class CollaboratorsResponse(
    val ok: Boolean = false,
    val collaborators: List<CollaboratorDto> = emptyList(),
    val error: String? = null
)

data class AppointmentDto(
    val id: Long = 0,
    val client_id: Long = 0,
    val client_name: String = "",
    val client_phone: String = "",
    val service_description: String = "",
    val assigned_user_id: Long? = null,
    val assigned_user_name: String? = null,
    val scheduled_start: String = "",
    val scheduled_end: String = "",
    val price: Double = 0.0,
    val status: String = "",
    val address: String? = null,
    val city: String? = null,
    val notes: String? = null,
    val created_origin: String? = null
)

data class AppointmentsResponse(
    val ok: Boolean = false,
    val date: String? = null,
    val start: String? = null,
    val end: String? = null,
    val appointments: List<AppointmentDto> = emptyList(),
    val error: String? = null
)

data class AppointmentDetailResponse(
    val ok: Boolean = false,
    val appointment: AppointmentDto? = null,
    val history: List<HistoryDto> = emptyList(),
    val error: String? = null
)

data class HistoryDto(
    val old_status: String? = null,
    val new_status: String = "",
    val note: String? = null,
    val changed_by_name: String? = null,
    val created_at: String = ""
)

data class CreateAppointmentResponse(
    val ok: Boolean = false,
    val appointment_id: Long? = null,
    val message: String? = null,
    val warning: String? = null,
    val error: String? = null
)

data class ChatDto(
    val jid: String = "",
    val name: String = "",
    val contact_name: String? = null,
    val phone: String? = null,
    val avatar: String? = null,
    val avatar_url: String? = null,
    val last_message: String? = null,
    val timestamp: Long = 0,
    val unread: Int = 0
) {
    fun resolvedAvatar(): String? = avatar_url?.takeIf { it.isNotBlank() }
        ?: avatar?.takeIf { it.isNotBlank() }
}

data class ChatsResponse(
    val ok: Boolean = false,
    val chats: List<ChatDto> = emptyList(),
    val error: String? = null
)

data class MessageDto(
    val id: String = "",
    val text: String? = null,
    val from_me: Boolean = false,
    val timestamp: Long = 0,
    val time: String? = null,
    val type: String? = null,
    val media: Boolean? = false,
    val has_media: Boolean? = false
) {
    fun hasMedia(): Boolean = media == true || has_media == true
}

data class ChatContextDto(
    val name: String? = null,
    val phone: String? = null,
    val avatar: String? = null,
    val avatar_url: String? = null
) {
    fun resolvedAvatar(): String? = avatar_url?.takeIf { it.isNotBlank() }
        ?: avatar?.takeIf { it.isNotBlank() }
}

data class MessagesResponse(
    val ok: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
    val context: ChatContextDto? = null,
    val error: String? = null
)

data class AnalyzeResponse(
    val ok: Boolean = false,
    val data: JsonObject? = null,
    val provider: String? = null,
    val model: String? = null,
    val cached: Boolean? = null,
    val error: String? = null
)

data class AiExtracted(
    val clientName: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val serviceDescription: String = "",
    val price: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = ""
)

fun JsonObject.toAiExtracted(): AiExtracted {
    fun s(vararg keys: String): String {
        for (key in keys) {
            val value = get(key)
            if (value != null && !value.isJsonNull) {
                return runCatching { value.asString }.getOrDefault("")
            }
        }
        return ""
    }
    return AiExtracted(
        clientName = s("client_name", "name"),
        phone = s("phone"),
        address = s("address"),
        city = s("city"),
        serviceDescription = s("service_description", "service"),
        price = s("price"),
        appointmentDate = s("appointment_date", "date"),
        appointmentTime = s("appointment_time", "time")
    )
}
