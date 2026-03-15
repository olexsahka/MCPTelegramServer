package org.example.mcptelegram.messaging

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.messaging.dto.MessageDto
import org.example.mcptelegram.messaging.dto.TelegramUpdateEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RedisPubSubServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var redisPubSubService: RedisPubSubService

    @Test
    fun `should publish and receive event via pub sub`() {
        val latch = CountDownLatch(1)
        var receivedEvent: TelegramUpdateEvent? = null

        redisPubSubService.subscribe { event ->
            receivedEvent = event
            latch.countDown()
        }

        val messageDto = MessageDto(messageId = 1L, chatId = 100L, senderName = "Alice", text = "Hello", sentAt = LocalDateTime.now())
        val event = TelegramUpdateEvent(type = TelegramUpdateEvent.EventType.NEW_MESSAGE, message = messageDto)

        Thread.sleep(100) // small delay for subscription to register
        redisPubSubService.publish(event)

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Event was not received in time")
        assertNotNull(receivedEvent)
        assertEquals(TelegramUpdateEvent.EventType.NEW_MESSAGE, receivedEvent!!.type)
        assertEquals("Hello", receivedEvent!!.message!!.text)
    }
}
