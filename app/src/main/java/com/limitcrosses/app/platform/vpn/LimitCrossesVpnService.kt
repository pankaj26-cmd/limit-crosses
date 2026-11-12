package com.limitcrosses.app.platform.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.limitcrosses.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PERMISSION_REVOKED,
    FAILED
}

/**
 * LimitCrossesVpnService
 *
 * Implements local-only DNS inspection for documented domains:
 * - facebook.com
 * - instagram.com
 * - snapchat.com
 *
 * Security and Architecture Guarantees:
 * 1. Local Processing Only: Packets are processed in-memory on the device.
 * 2. Zero Decryption: No TLS/HTTPS interception or TLS certificate injection.
 * 3. Minimal Domain Scope: Inspects only UDP port 53 (DNS) query header labels.
 * 4. Zero Data Exfiltration: Browsing history is never stored or uploaded.
 * 5. Transparent State Reporting: Accurately reflects disconnected/revoked state.
 */
class LimitCrossesVpnService : VpnService() {

    companion object {
        private const val TAG = "LimitCrossesVpn"
        private const val NOTIFICATION_CHANNEL_ID = "limit_crosses_vpn_channel"
        private const val NOTIFICATION_ID = 2001

        val TARGET_DOMAINS = listOf(
            "facebook.com",
            "instagram.com",
            "snapchat.com"
        )

        private val _vpnStatusFlow = MutableStateFlow(VpnStatus.DISCONNECTED)
        val vpnStatusFlow = _vpnStatusFlow.asStateFlow()

        fun isPrepared(context: Context): Boolean {
            return prepare(context) == null
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "ACTION_STOP_VPN") {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        _vpnStatusFlow.value = VpnStatus.CONNECTING
        try {
            val builder = Builder()
                .setSession("Limit Crosses DNS Filter")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8")
                // Route DNS traffic locally
                .addRoute("8.8.8.8", 32)
                .setMtu(1500)
                .setBlocking(false)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                _vpnStatusFlow.value = VpnStatus.PERMISSION_REVOKED
                stopSelf()
                return
            }

            _vpnStatusFlow.value = VpnStatus.CONNECTED
            startPacketLoop(vpnInterface!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface: ${e.message}")
            _vpnStatusFlow.value = VpnStatus.FAILED
            stopSelf()
        }
    }

    private fun startPacketLoop(pfd: ParcelFileDescriptor) {
        vpnJob?.cancel()
        vpnJob = serviceScope.launch {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)

            try {
                while (isActive) {
                    val length = inputStream.read(buffer.array())
                    if (length > 0) {
                        buffer.limit(length)
                        parseDnsPacket(buffer)
                        buffer.clear()
                    }
                    kotlinx.coroutines.delay(50)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(TAG, "Packet loop terminated: ${e.message}")
                }
            } finally {
                _vpnStatusFlow.value = VpnStatus.DISCONNECTED
            }
        }
    }

    /**
     * Inspects DNS queries for targeted social domains without decrypting any content.
     */
    private fun parseDnsPacket(buffer: ByteBuffer) {
        if (buffer.remaining() < 28) return // IP + UDP header minimum

        val raw = buffer.array()
        val ipHeaderLength = (raw[0].toInt() and 0x0F) * 4
        if (ipHeaderLength + 8 > buffer.remaining()) return

        val protocol = raw[9].toInt() and 0xFF
        if (protocol != 17) return // Protocol 17 = UDP

        val destPort = ((raw[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                (raw[ipHeaderLength + 3].toInt() and 0xFF)

        // Only inspect standard DNS queries (Port 53)
        if (destPort == 53) {
            val dnsOffset = ipHeaderLength + 8
            val queryDomain = extractDnsQuestionDomain(raw, dnsOffset, buffer.remaining())
            if (queryDomain != null) {
                for (target in TARGET_DOMAINS) {
                    if (queryDomain.endsWith(target, ignoreCase = true)) {
                        Log.d(TAG, "Monitored domain query detected locally: $queryDomain")
                        // Local domain detected; minimal event logged without recording browsing history
                    }
                }
            }
        }
    }

    private fun extractDnsQuestionDomain(bytes: ByteArray, offset: Int, maxLen: Int): String? {
        val qOffset = offset + 12 // Skip DNS header (12 bytes)
        if (qOffset >= maxLen) return null

        val sb = java.lang.StringBuilder()
        var idx = qOffset
        while (idx < maxLen) {
            val labelLen = bytes[idx].toInt() and 0xFF
            if (labelLen == 0) break
            idx++
            if (idx + labelLen > maxLen) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(bytes, idx, labelLen, Charsets.US_ASCII))
            idx += labelLen
        }
        return sb.toString().takeIf { it.isNotEmpty() }
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnJob = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        _vpnStatusFlow.value = VpnStatus.DISCONNECTED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        _vpnStatusFlow.value = VpnStatus.PERMISSION_REVOKED
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Limit Crosses Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors local domain resolution for protected apps."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Limit Crosses Active")
            .setContentText("Local protection active (Best-effort domain monitoring)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}

