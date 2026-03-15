package org.example.mcptelegram.persistence

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.persistence.entity.UserEntity
import org.example.mcptelegram.persistence.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `should save and find user by telegramId`() {
        val user = UserEntity(telegramId = 123456789L, firstName = "John", username = "johndoe")
        userRepository.save(user)

        val found = userRepository.findByTelegramId(123456789L)
        assertTrue(found.isPresent)
        assertEquals("John", found.get().firstName)
        assertEquals("johndoe", found.get().username)
    }

    @Test
    fun `should return empty for non-existing telegramId`() {
        val found = userRepository.findByTelegramId(999999999L)
        assertFalse(found.isPresent)
    }
}
