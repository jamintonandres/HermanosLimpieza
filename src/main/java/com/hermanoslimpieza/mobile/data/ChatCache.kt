package com.hermanoslimpieza.mobile.data

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.security.MessageDigest

/**
 * Caché persistente y descartable del CRM.
 * Android puede borrar cacheDir si necesita espacio; Chatwoot sigue siendo la fuente de verdad.
 */
class ChatCache(context: Context) {
    private val dir = File(context.cacheDir, "crm_v110").apply { mkdirs() }
    private val gson = Gson()

    private data class CachedChats(val savedAt: Long, val chats: List<ChatDto>)
    private data class CachedMessages(
        val savedAt: Long,
        val context: ChatContextDto?,
        val messages: List<MessageDto>
    )

    fun loadChats(): List<ChatDto> {
        val file = File(dir, "chats.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            gson.fromJson(file.readText(), CachedChats::class.java)?.chats ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun saveChats(chats: List<ChatDto>) {
        runCatching {
            File(dir, "chats.json").writeText(
                gson.toJson(CachedChats(System.currentTimeMillis(), chats))
            )
        }
    }

    fun loadMessages(jid: String): Pair<ChatContextDto?, List<MessageDto>> {
        val file = File(dir, "messages_${safeKey(jid)}.json")
        if (!file.exists()) return null to emptyList()
        return runCatching {
            val cached = gson.fromJson(file.readText(), CachedMessages::class.java)
            cached?.context to (cached?.messages ?: emptyList())
        }.getOrDefault(null to emptyList())
    }

    fun saveMessages(jid: String, context: ChatContextDto?, messages: List<MessageDto>) {
        runCatching {
            File(dir, "messages_${safeKey(jid)}.json").writeText(
                gson.toJson(CachedMessages(System.currentTimeMillis(), context, messages))
            )
        }
    }

    fun clear() {
        runCatching { dir.listFiles()?.forEach { it.delete() } }
    }

    private fun safeKey(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }
}
