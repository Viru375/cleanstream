package com.cleanstream.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cleanstream.app.databinding.ActivityMainBinding

/**
 * Main Activity — hosts the WebView, handles fullscreen video,
 * background playback lifecycle, and system-wide VPN permission exchange.
 */
class MainActivity : AppCompatActivity(), ControlsBottomSheet.Listener {

    private lateinit var binding: ActivityMainBinding
    private val youtubeHomeUrl = "https://m.youtube.com"

    /** Handles standard Android VPN system consent dialog result. */
    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            AdBlockVpnService.start(this)
            PreferencesManager.setSystemAdBlockEnabled(true)
        } else {
            PreferencesManager.setSystemAdBlockEnabled(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PreferencesManager.initialize(this)
        requestNotificationPermissionIfNeeded()
        configureWebView()
        attachSettingsFabListener()

        if (savedInstanceState == null) {
            binding.webView.loadUrl(youtubeHomeUrl)
        }

        startActiveServices()
    }

    /** Configures all WebView settings for optimal YouTube playback. */
    private fun configureWebView() {
        val webSettings: WebSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.mediaPlaybackRequiresUserGesture = false

        binding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }

        binding.webView.webViewClient = CleanWebViewClient(
            context = this,
            preferencesManager = PreferencesManager,
            adBlocker = AdBlocker,
            jsInjector = JsInjector
        )

        binding.webView.webChromeClient = CleanChromeClient(
            activity = this,
            fullscreenContainer = binding.fullscreenContainer,
            webView = binding.webView
        )
    }

    private fun attachSettingsFabListener() {
        binding.fabSettings.setOnClickListener {
            val sheet = ControlsBottomSheet()
            sheet.listener = this
            sheet.show(supportFragmentManager, ControlsBottomSheet.TAG)
        }
    }

    override fun onSettingsChanged(blockAds: Boolean, blockShorts: Boolean, backgroundPlay: Boolean) {
        binding.webView.reload()
        if (backgroundPlay) {
            BackgroundAudioService.start(this)
        } else {
            BackgroundAudioService.stop(this)
        }
    }

    /**
     * Handles user toggling the "Block Ads in All Apps (VPN)" switch.
     * If Android VPN consent is needed, launches the system dialog; otherwise starts VPN.
     */
    override fun onSystemAdBlockToggled(enabled: Boolean) {
        if (enabled) {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnConsentLauncher.launch(vpnIntent)
            } else {
                AdBlockVpnService.start(this)
            }
        } else {
            AdBlockVpnService.stop(this)
        }
    }

    private fun startActiveServices() {
        if (PreferencesManager.isBackgroundPlayEnabled()) {
            BackgroundAudioService.start(this)
        }
        if (PreferencesManager.isSystemAdBlockEnabled()) {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent == null) {
                AdBlockVpnService.start(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
        binding.webView.resumeTimers()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
        binding.webView.pauseTimers()
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
        binding.webView.destroy()
    }

    @Deprecated("Required for API < 33 back handling")
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
