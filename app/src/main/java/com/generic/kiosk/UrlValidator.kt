package com.generic.kiosk

import java.net.URI

object UrlValidator {
    fun validate(raw: String): String? {
        val value = raw.trim()
        if (value.isBlank() || value.any { it.isWhitespace() }) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        if (uri.rawUserInfo != null) return null
        return uri.toASCIIString()
    }
}
