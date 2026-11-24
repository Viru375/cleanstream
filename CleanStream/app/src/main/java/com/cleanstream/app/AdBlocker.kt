package com.cleanstream.app

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Network-level ad blocker.
 * Operates inside WebViewClient.shouldInterceptRequest — before any ad request
 * reaches the network — making it faster and more effective than DOM-based blocking alone.
 */
object AdBlocker {

    /**
     * Domains known to serve ads, tracking pixels, or analytics.
     * Requests to any of these hosts are silently dropped.
     */
    private val blockedDomains = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "imasdk.googleapis.com",   // YouTube IMA ad SDK — most critical to block
        "2mdn.net",
        "advertising.com",
        "adbrite.com",
        "quantserve.com",
        "scorecardresearch.com",
        "zedo.com",
        "adnxs.com",
        "adsafeprotected.com",
        "moatads.com",
        "omtrdc.net",
        "static.doubleclick.net",
        "ad.doubleclick.net",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com"
    )

    /**
     * URL path/query patterns that indicate ad or tracking requests
     * even when served from allowed domains like youtube.com.
     */
    private val blockedUrlPatterns = listOf(
        "/pagead/",
        "/api/stats/ads",
        "/ptracking",
        "/get_midroll_info",
        "/api/stats/qoe",
        "&adformat=",
        "youtube.com/api/stats/ads",
        "/log_event",
        "ad.youtube.com",
        "/pagead/gen_204",
        "c.amazon-adsystem.com",
        "pagead/lvz",
        "youtube.com/pagead"
    )

    /**
     * Returns true if the given URL matches any blocked domain or path pattern.
     * Used to decide whether to intercept a WebView network request.
     */
    fun shouldBlock(url: String): Boolean {
        val lowerCaseUrl = url.lowercase()
        val domainIsBlocked = blockedDomains.any { blockedDomain ->
            lowerCaseUrl.contains(blockedDomain)
        }
        if (domainIsBlocked) return true
        return blockedUrlPatterns.any { pattern -> lowerCaseUrl.contains(pattern) }
    }

    /**
     * Returns an empty HTTP response body, effectively silencing the blocked request.
     * The WebView receives a valid (but empty) response instead of a network error,
     * preventing broken-page side effects.
     */
    fun createEmptyResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        )
}
