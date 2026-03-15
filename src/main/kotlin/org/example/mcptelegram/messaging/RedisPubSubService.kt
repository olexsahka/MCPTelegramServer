package org.example.mcptelegram.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.mcptelegram.messaging.dto.TelegramUpdateEvent
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class RedisPubSubService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisMessageListenerContainer: RedisMessageListenerContainer,
    private val objectMapper: ObjectMapper
) {
    companion object {
        const val CHANNEL = "telegram.updates"
    }

    fun publish(event: TelegramUpdateEvent) {
        val json = objectMapper.writeValueAsString(event)
        redisTemplate.convertAndSend(CHANNEL, json)
    }

    fun subscribe(handler: (TelegramUpdateEvent) -> Unit) {
        val listener = MessageListener { message: Message, _ ->
            val json = String(message.body)
            val event = objectMapper.readValue<TelegramUpdateEvent>(json)
            handler(event)
        }
        redisMessageListenerContainer.addMessageListener(listener, ChannelTopic(CHANNEL))
    }
}
