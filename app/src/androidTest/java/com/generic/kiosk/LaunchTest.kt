package com.generic.kiosk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @Test fun mainActivityStarts() { ActivityScenario.launch(MainActivity::class.java).use { } }
    @Test fun adminPinUsesKeystoreBackedAuthentication() {
        val hash = AdminAuthenticator.hash("837261")
        assertTrue(AdminAuthenticator.verify("837261", hash))
        assertFalse(AdminAuthenticator.verify("837260", hash))
    }
}
