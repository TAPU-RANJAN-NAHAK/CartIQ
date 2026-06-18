package com.cartiq.assistant.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByPhone(phone: String): User?
}

@Repository
interface AlertRepository : JpaRepository<Alert, String> {
    fun findByUserIdAndActive(userId: String, active: Boolean): List<Alert>
    fun findByActive(active: Boolean): List<Alert>
}

@Repository
interface PriceSnapshotRepository : JpaRepository<PriceSnapshot, String> {
    fun findTopByProductIdAndPlatformOrderByTimestampDesc(productId: String, platform: Platform): PriceSnapshot?
}
