# CleanStream Android App (Native Ad-Blocking & System Protection)

CleanStream is a native Android application designed to provide zero-ad streaming, background audio playback, and system-wide ad and tracker protection across all Android apps and games without requiring root or MicroG.

---

## Key Features

1. **YouTube Ad-Free Player Engine:**
   - **Proactive Interception:** Rewrites `/youtubei/v1/player` and `ytInitialPlayerResponse` before the player can load ad schedules.
   - **Zero Fast-Forwarding / Instant Skipping:** If an ad slipstream is initiated, it instantly skips to the end (`currentTime = duration`) and mutes audio during the transition, preventing the fast-forward effect.
   - **Background Audio Service:** Keeps audio playing when switching apps or locking the device screen.
   - **Shorts Auto-Redirect & Element Hiding:** Re-routes `/shorts/` to normal player view and removes promotional banners.

2. **System-Wide & In-App Ad Blocker (Non-Root VPN):**
   - **Local TUN Interface:** Runs on `10.0.0.2/32` loopback, intercepting DNS traffic from all third-party apps, games, and browsers.
   - **Zero-Log AdGuard DNS:** Resolves ad server requests (`doubleclick`, `googlesyndication`, `appsflyer`, etc.) to `0.0.0.0`.
   - **Zero Root Needed:** Uses standard Android `VpnService` API.

3. **Native Private DNS Shortcut:**
   - One-tap button copies `dns.adguard-dns.com` to clipboard and opens Android Network Settings for battery-free, OS-level ad-blocking.

---

## How to Build the APK

### Option A: Cloud CI Build via GitHub Actions (Zero Setup Required)
1. Push this folder to a GitHub repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of CleanStream"
   git remote add origin https://github.com/<your-username>/cleanstream.git
   git push -u origin main
   ```
2. On GitHub, navigate to **Actions** > **Build Android APK**.
3. Once the build completes (~90 seconds), download **CleanStream-v1.0.0-Debug-APK** under the workflow artifacts.
4. Transfer or download the `.apk` directly onto your mobile phone.

### Option B: Local Build via Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose this `CleanStream` folder.
3. Wait for Gradle sync to finish.
4. Go to **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. Once built, click **locate** in the popup notification to find `app-debug.apk` in:
   `app/build/outputs/apk/debug/app-debug.apk`

### Option C: 1-Click Batch Script
If you have Android Studio or the Android SDK installed:
Double-click `Build-APK.bat` in this folder. It will configure `local.properties` and compile `app-debug.apk`.

---

## How to Install & Test on Your Mobile Phone

1. **Transfer the APK to Your Phone:**
   - Send `app-debug.apk` to your phone via USB cable, Google Drive, WhatsApp, or email.
2. **Enable Unknown App Installation:**
   - Tap the APK file in your phone's File Manager or Google Drive.
   - If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** and toggle **Allow from this source**.
3. **Install & Launch:**
   - Tap **Install**. Once finished, open **CleanStream**.
4. **Testing Checklist:**
   - [ ] **Ad-Free Video:** Search and play popular videos on YouTube. Notice ads do not play at all (no 16x fast-forward, no delay).
   - [ ] **Pause/Play Verification:** Tap pause during a video. Verify playback stays paused without auto-resuming.
   - [ ] **Background Audio:** Play a video, swipe up to return to Android Home or lock the screen. Audio continues playing with notification controls.
   - [ ] **System-Wide AdBlock (All Apps):** Tap the gear icon (FAB) > toggle **Block Ads in All Apps (VPN)** > accept the Android VPN permission dialog. Open an ad-supported game or app to verify ad banners are blocked.
   - [ ] **Native Private DNS:** Tap **Configure Android Private DNS** in the controls sheet, paste `dns.adguard-dns.com` into Android Private DNS settings.
