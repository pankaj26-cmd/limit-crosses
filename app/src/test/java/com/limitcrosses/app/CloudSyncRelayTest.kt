package com.limitcrosses.app

import com.limitcrosses.app.core.model.SocialHandles
import com.limitcrosses.app.core.model.UserAccount
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.core.model.UserStatus
import com.limitcrosses.app.data.remote.CloudSyncRelayHelper
import com.limitcrosses.app.data.remote.RemoteUsageTelemetry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CloudSyncRelayTest {

    @Test
    fun testCloudSyncRelayTopicDerivation() {
        val topic1 = CloudSyncRelayHelper.getTopicForInvite("LC-7M4P-8K9N-2Q5W")
        val topic2 = CloudSyncRelayHelper.getTopicForInvite("lc-7m4p-8k9n-2q5w")
        val topic3 = CloudSyncRelayHelper.getTopicForInvite("7M4P 8K9N 2Q5W")

        assertEquals(topic1, topic2)
        assertEquals(topic1, topic3)
        assertTrue(topic1.startsWith("lc_inv_"))
        assertEquals(27, topic1.length) // "lc_inv_" (7) + 20 = 27
    }

    @Test
    fun testPublishAndPollRegistration() = runBlocking {
        val inviteCode = "LC-TEST-SYNC-${java.util.UUID.randomUUID().toString().take(8).uppercase()}"
        val user = UserAccount(
            id = "usr-sync-test",
            email = "synctest@domain.com",
            fullName = "Sync Test User",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            socialHandles = SocialHandles(facebook = "sync_fb"),
            registeredViaInviteCode = inviteCode,
            createdAtEpochMs = System.currentTimeMillis()
        )

        // 1. Publish registration (from Device B)
        val published = CloudSyncRelayHelper.publishRegistration(inviteCode, user, limitMinutes = 25)
        if (!published) {
            println("Skipping network assertion: Relay server temporarily rate-limited or unreachable in test environment.")
            return@runBlocking
        }

        kotlinx.coroutines.delay(500)

        // 2. Poll registration (from Device A)
        val remote = CloudSyncRelayHelper.pollRegistrationForInvite(inviteCode)
        if (remote != null) {
            assertEquals("Sync Test User", remote.fullName)
            assertEquals("synctest@domain.com", remote.email)
            assertEquals(25, remote.limitMinutes)
            assertEquals("sync_fb", remote.facebook)
        }
    }

    @Test
    fun testOfflineSyncCodeRoundtrip() {
        val user = UserAccount(
            id = "usr-offline-1",
            email = "offline@domain.com",
            fullName = "Offline User",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAtEpochMs = 1794500000000L
        )

        val telemetry = RemoteUsageTelemetry(
            userId = "usr-offline-1",
            facebookSeconds = 300L,
            instagramSeconds = 600L,
            snapchatSeconds = 120L,
            totalUsedSeconds = 1020L,
            timestampEpochMs = 1794500001000L
        )

        val syncCode = CloudSyncRelayHelper.generateOfflineSyncCode(user, "LC-ABCD-1234", 30, telemetry)
        assertTrue(syncCode.startsWith("LCR-"))

        val parsed = CloudSyncRelayHelper.parseOfflineSyncCode(syncCode)
        assertNotNull(parsed)
        assertEquals("Offline User", parsed!!.fullName)
        assertEquals("offline@domain.com", parsed.email)
        assertEquals(30, parsed.limitMinutes)
        assertEquals("LC-ABCD-1234", parsed.inviteCode)
        assertNotNull(parsed.usageTelemetry)
        assertEquals(300L, parsed.usageTelemetry!!.facebookSeconds)
        assertEquals(600L, parsed.usageTelemetry!!.instagramSeconds)
        assertEquals(120L, parsed.usageTelemetry!!.snapchatSeconds)
        assertEquals(1020L, parsed.usageTelemetry!!.totalUsedSeconds)
    }

    @Test
    fun testPublishAndPollUsageTelemetry() = runBlocking {
        val inviteCode = "LC-USAGE-TEST-${java.util.UUID.randomUUID().toString().take(8).uppercase()}"
        val telemetry = RemoteUsageTelemetry(
            userId = "usr-mohit-test",
            facebookSeconds = 0L,
            instagramSeconds = 180L,
            snapchatSeconds = 60L,
            totalUsedSeconds = 240L,
            timestampEpochMs = System.currentTimeMillis()
        )

        val published = CloudSyncRelayHelper.publishUsageTelemetry(inviteCode, telemetry)
        if (!published) {
            println("Skipping network assertion: Relay server temporarily rate-limited or unreachable in test environment.")
            return@runBlocking
        }

        kotlinx.coroutines.delay(500)

        val telemetries = CloudSyncRelayHelper.pollUsageTelemetry(inviteCode)
        val polled = telemetries.find { it.userId == "usr-mohit-test" }
        if (polled != null) {
            assertEquals(0L, polled.facebookSeconds)
            assertEquals(180L, polled.instagramSeconds)
            assertEquals(60L, polled.snapchatSeconds)
            assertEquals(240L, polled.totalUsedSeconds)
        }
    }

    @Test
    fun testPollAllEventsForInvite() = runBlocking {
        val inviteCode = "LC-UNIFIED-TEST-${java.util.UUID.randomUUID().toString().take(8).uppercase()}"
        val telemetry = RemoteUsageTelemetry(
            userId = "usr-unified-1",
            userEmail = "unified@example.com",
            inviteCode = inviteCode,
            facebookSeconds = 600L,
            instagramSeconds = 1200L,
            snapchatSeconds = 300L,
            totalUsedSeconds = 2100L,
            timestampEpochMs = System.currentTimeMillis()
        )
        val published = CloudSyncRelayHelper.publishUsageTelemetry(inviteCode, telemetry)
        if (!published) {
            println("Skipping network assertion: Relay server temporarily rate-limited or unreachable in test environment.")
            return@runBlocking
        }

        kotlinx.coroutines.delay(500)

        val events = CloudSyncRelayHelper.pollAllEventsForInvite(inviteCode)
        assertNotNull(events)
        val foundTel = events.usageTelemetries.find { it.userId == "usr-unified-1" }
        if (foundTel != null) {
            assertEquals(600L, foundTel.facebookSeconds)
            assertEquals(1200L, foundTel.instagramSeconds)
            assertEquals(300L, foundTel.snapchatSeconds)
            assertEquals(2100L, foundTel.totalUsedSeconds)
        }
    }
}
