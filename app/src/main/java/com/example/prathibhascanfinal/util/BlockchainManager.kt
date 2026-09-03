package com.example.prathibhascanfinal.util

import java.security.MessageDigest
import java.util.UUID

object BlockchainManager {

    fun generateAthleteID(email: String, timestamp: Long): String {
        val rawData = "$email|$timestamp|${UUID.randomUUID()}"
        val hash = hashString(rawData)
        return "ATH-${hash.take(3).uppercase()}-${hash.substring(4, 7).uppercase()}-BC-${hash.takeLast(3).uppercase()}"
    }

    fun generateAcademyID(name: String): String {
        val hash = hashString(name + System.currentTimeMillis())
        return "ACA-${hash.take(4).uppercase()}-${hash.takeLast(4).uppercase()}"
    }

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    fun verifyIdentity(id: String): Boolean {
        // In a real blockchain, this would verify against a ledger
        return id.startsWith("ATH-") || id.startsWith("ACA-") || id.startsWith("INST-")
    }
}
