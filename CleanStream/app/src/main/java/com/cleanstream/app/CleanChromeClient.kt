package com.cleanstream.app

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout

private const val LOG_TAG = "CleanStream"

/**
 * Custom WebChromeClient that enables:
 * 1. Full-screen video playback (toggling between WebView and a full-screen overlay)
 * 2. Console message forwarding from JavaScript to Logcat (for debugging inject.js)
 * 3. Permission grants for media access (future-proofing for video calls etc.)
 */
class CleanChromeClient(
    private val activity: Activity,
    private val fullscreenContainer: FrameLayout,
    private val webView: WebView
) : WebChromeClient() {

    /** Holds the full-screen view returned by the browser engine so we can remove it later. */
    private var activeFullscreenView: View? = null

    /**
     * Called when a video enters full-screen mode.
     * We hide the WebView and place the full-screen video view in a dedicated container
     * so the video fills the entire screen, including behind system bars.
     */
    override fun onShowCustomView(fullscreenView: View, callback: CustomViewCallback) {
        activeFullscreenView = fullscreenView
        webView.visibility = View.GONE
        fullscreenContainer.visibility = View.VISIBLE
        fullscreenContainer.addView(fullscreenView)
        hideSystemBars()
    }

    /**
     * Called when the user exits full-screen mode (back button or player control).
     * Restores the WebView and removes the full-screen overlay.
     */
    override fun onHideCustomView() {
        activeFullscreenView?.let { fullscreenView ->
            fullscreenContainer.removeView(fullscreenView)
        }
        activeFullscreenView = null
        fullscreenContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE
        showSystemBars()
    }

    /** Grants media permissions automatically to avoid blocking YouTube's player. */
    override fun onPermissionRequest(request: PermissionRequest) {
        request.grant(request.resources)
    }

    /** Bridges console.log / console.error from inject.js to Android Logcat. */
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        val logMessage = "${consoleMessage.message()} [${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}]"
        when (consoleMessage.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> Log.e(LOG_TAG, logMessage)
            ConsoleMessage.MessageLevel.WARNING -> Log.w(LOG_TAG, logMessage)
            else -> Log.d(LOG_TAG, logMessage)
        }
        return true
    }

    private fun hideSystemBars() {
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }

    private fun showSystemBars() {
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}
