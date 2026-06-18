package com.cartiq.assistant.service

import com.cartiq.assistant.model.User
import com.cartiq.assistant.model.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun getOrCreate(phone: String): User {
        return userRepository.findByPhone(phone)
            ?: userRepository.save(User(phone = phone))
    }

    @Transactional
    fun setLocation(phone: String, location: String): User {
        val user = getOrCreate(phone)
        return userRepository.save(user.copy(location = location))
    }
}
