package org.example.mcptelegram.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.mcptelegram.messaging.dto.DialogDto
import org.example.mcptelegram.messaging.dto.MessageDto
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class DialogCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val DIALOG_TTL_MINUTES = 5L
        private const val MESSAGES_TTL_MINUTES = 1L
        private fun dialogKey(chatId: Long) = "dialog:$chatId"
        private fun messagesKey(chatId: Long) = "messages:$chatId:last"
    }

    fun cacheDialog(chatId: Long, dialog: DialogDto) {
        val json = objectMapper.writeValueAsString(dialog)
        redisTemplate.opsForValue().set(dialogKey(chatId), json, DIALOG_TTL_MINUTES, TimeUnit.MINUTES)
    }

    fun getCachedDialog(chatId: Long): DialogDto? {
        val json = redisTemplate.opsForValue().get(dialogKey(chatId)) ?: return null
        return objectMapper.readValue(json)
    }

    fun cacheLastMessages(chatId: Long, messages: List<MessageDto>) {
        val json = objectMapper.writeValueAsString(messages)
        redisTemplate.opsForValue().set(messagesKey(chatId), json, MESSAGES_TTL_MINUTES, TimeUnit.MINUTES)
    }

    fun getCachedLastMessages(chatId: Long): List<MessageDto>? {
        val json = redisTemplate.opsForValue().get(messagesKey(chatId)) ?: return null
        return objectMapper.readValue(json)
    }

    fun invalidateDialog(chatId: Long) {
        redisTemplate.delete(dialogKey(chatId))
        redisTemplate.delete(messagesKey(chatId))
    }
}
