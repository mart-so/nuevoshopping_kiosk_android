package com.generic.kiosk

import org.junit.Assert.*
import org.junit.Test
class CoreTest {
    @Test fun acceptsHttpAndHttpsOnly() {
        assertEquals("https://example.org/path", UrlValidator.validate(" https://example.org/path "))
        assertEquals("http://192.168.1.1", UrlValidator.validate("http://192.168.1.1"))
        listOf("javascript:alert(1)", "file:///etc/passwd", "intent://x", "https://", "https://example.org/a b", "https://u:p@example.org").forEach { assertNull(it, UrlValidator.validate(it)) }
    }
}
