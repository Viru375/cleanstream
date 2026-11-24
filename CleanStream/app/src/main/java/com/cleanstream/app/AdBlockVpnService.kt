package com.cleanstream.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * System-wide Ad-Blocking VPN Service.
 *
 * Why this works without root:
 * Android allows an app to register a local VpnService. By configuring
 * a virtual network TUN interface and routing DNS traffic through
 * zero-log ad-blocking resolvers (such as AdGuard DNS), all DNS queries
 * made by third-party apps, games, mobile browsers, and system services
 * are automatically filtered. Ad domains resolve to 0.0.0.0 (blocked).
 */
class AdBlockVpnService : VpnService() {

    private var activeVpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpnTunnel()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        establishVpnTunnel()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnTunnel()
    }

    /**
     * Configures the TUN interface and sets the ad-blocking DNS servers.
     * Functions are kept modular and under 25 lines.
     */
    private fun establishVpnTunnel() {
        try {
            val vpnBuilder = Builder()
                .setSession("CleanStream System AdBlock")
                .addAddress(LOCAL_TUN_IP, LOCAL_TUN_PREFIX)
                .addDnsServer(DNS_PRIMARY_IPV4)
                .addDnsServer(DNS_SECONDARY_IPV4)
                .addRoute(DNS_PRIMARY_IPV4, 32)
                .addRoute(DNS_SECONDARY_IPV4, 32)

            // Configure PendingIntent so user can return to CleanStream from notification
            val configureIntent = Intent(this, MainActivity::class.java)
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            vpnBuilder.setConfigureIntent(PendingIntent.getActivity(this, 0, configureIntent, pendingFlags))

            activeVpnInterface = vpnBuilder.establish()
            Log.d(TAG, "System-wide AdBlock VPN tunnel established successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface", e)
            stopSelf()
        }
    }

    /** Gracefully releases the VPN file descriptor and stops foreground state. */
    private fun stopVpnTunnel() {
        try {
            activeVpnInterface?.close()
            activeVpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN descriptor", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Creates the notification channel and starts foreground state. */
    private fun startForegroundNotification() {
        createNotificationChannelIfNeeded()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CleanStream System Protection")
            .setContentText("Ad-blocking active across all apps and system")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /** Required for Android O (API 26) and above. */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System AdBlock VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of system-wide ad and tracker protection"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "AdBlockVpnService"
        private const val CHANNEL_ID = "cleanstream_system_vpn"
        private const val NOTIFICATION_ID = 2002
        private const val ACTION_STOP_VPN = "com.cleanstream.app.STOP_VPN"

        private const val LOCAL_TUN_IP = "10.0.0.2"
        private const val LOCAL_TUN_PREFIX = 32

        // AdGuard Default Ad-Blocking DNS (Blocks ads, trackers, phishing)
        private const val DNS_PRIMARY_IPV4 = "94.140.14.14"
        private const val DNS_SECONDARY_IPV4 = "94.140.15.15"

        fun start(context: Context) {
            val intent = Intent(context, AdBlockVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdBlockVpnService::class.java).apply {
                action = ACTION_STOP_VPN
            }
            context.startService(intent)
        }
    }
}
