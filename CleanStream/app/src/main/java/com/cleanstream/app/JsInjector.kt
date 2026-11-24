package com.cleanstream.app

import android.content.Context

/**
 * Handles loading and assembling the JavaScript and CSS injection scripts
 * from the app's assets folder. Separates asset-reading concerns from
 * WebView and business logic concerns.
 */
object JsInjector {

    /** Reads the raw JS injection script from assets/inject.js */
    fun loadInjectionScript(context: Context): String =
        context.assets.open("inject.js").bufferedReader().use { it.readText() }

    /** Reads the raw CSS rules from assets/inject.css */
    fun loadInjectionCss(context: Context): String =
        context.assets.open("inject.css").bufferedReader().use { it.readText() }

    /**
     * Wraps a CSS string in a JavaScript snippet that injects it into <head>.
     * This allows injecting CSS via WebView.evaluateJavascript() at page start,
     * preventing any flash of unstyled/ad content before the page finishes loading.
     */
    fun buildCssInjectionJs(cssContent: String): String {
        // Escape backticks and backslashes so the CSS is safe inside a JS template literal
        val escapedCss = cssContent
            .replace("\\", "\\\\")
            .replace("`", "\\`")
        return """
            (function() {
                const existingStyle = document.getElementById('cleanstream-css');
                if (existingStyle) return; // Avoid duplicate injection
                const styleElement = document.createElement('style');
                styleElement.id = 'cleanstream-css';
                styleElement.textContent = `$escapedCss`;
                (document.head || document.documentElement).appendChild(styleElement);
            })();
        """.trimIndent()
    }

    /**
     * Builds the full injection script by substituting the feature-flag placeholders
     * in inject.js with the current user preferences. This keeps feature toggling
     * in Kotlin rather than requiring a separate JS config file.
     */
    fun buildFullInjectionScript(
        context: Context,
        blockAds: Boolean,
        blockShorts: Boolean,
        backgroundPlay: Boolean
    ): String {
        val rawScript = loadInjectionScript(context)
        return rawScript
            .replace("%%BLOCK_ADS%%", blockAds.toString())
            .replace("%%BLOCK_SHORTS%%", blockShorts.toString())
            .replace("%%BACKGROUND_PLAY%%", backgroundPlay.toString())
    }
}
