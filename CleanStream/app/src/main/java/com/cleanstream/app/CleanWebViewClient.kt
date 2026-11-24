package com.cleanstream.app

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebViewClient that intercepts network requests for ad blocking
 * and injects CSS/JS at the right page-lifecycle moments for cosmetic filtering.
 *
 * Two-layer approach:
 *   Layer 1 (shouldInterceptRequest) — blocks ad requests at the network level
 *   Layer 2 (onPageStarted/Finished) — injects CSS/JS for DOM cosmetic filtering
 */
class CleanWebViewClient(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val adBlocker: AdBlocker,
    private val jsInjector: JsInjector
) : WebViewClient() {

    /**
     * Layer 1: Network-level blocking.
     * Called for every sub-resource request the WebView makes.
     * Returning a non-null WebResourceResponse short-circuits the real network call.
     */
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val requestUrl = request.url.toString()
        val shouldBlockRequest = preferencesManager.isAdBlockEnabled() &&
                adBlocker.shouldBlock(requestUrl)
        return if (shouldBlockRequest) {
            adBlocker.createEmptyResponse()
        } else {
            super.shouldInterceptRequest(view, request)
        }
    }

    /**
     * Layer 2a: CSS injected early (before page renders) to prevent
     * a flash of ad content while the DOM is being built.
     */
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val cssContent = jsInjector.loadInjectionCss(context)
        val cssAsJsScript = jsInjector.buildCssInjectionJs(cssContent)
        view.evaluateJavascript(cssAsJsScript, null)
    }

    /**
     * Layer 2b: Full JS script injected after page load for
     * MutationObserver setup, Shorts redirection, and ad auto-skipping.
     */
    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        val fullScript = jsInjector.buildFullInjectionScript(
            context = context,
            blockAds = preferencesManager.isAdBlockEnabled(),
            blockShorts = preferencesManager.isShortsBlockEnabled(),
            backgroundPlay = preferencesManager.isBackgroundPlayEnabled()
        )
        view.evaluateJavascript(fullScript, null)
    }

    /**
     * Keep navigation within the WebView for YouTube and Google sign-in URLs.
     * Block all other external URLs to prevent the app from becoming a general browser.
     */
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val urlString = request.url.toString()
        val isInternalNavigation = urlString.contains("youtube.com") ||
                urlString.contains("youtu.be") ||
                urlString.contains("accounts.google.com") ||
                urlString.contains("google.com/signin")
        // Returning false means: "WebView, you handle this URL"
        return !isInternalNavigation
    }
}
