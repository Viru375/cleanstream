@echo off
setlocal enabledelayedexpansion
title CleanStream APK Builder

echo ======================================================================
echo                CleanStream Android APK Builder
echo ======================================================================
echo.

:: 1. Check for Android SDK in standard locations
set "SDK_FOUND="
if defined ANDROID_HOME (
    if exist "%ANDROID_HOME%\platforms" set "SDK_FOUND=%ANDROID_HOME%"
)
if not defined SDK_FOUND (
    if exist "%LOCALAPPDATA%\Android\Sdk\platforms" (
        set "SDK_FOUND=%LOCALAPPDATA%\Android\Sdk"
    )
)

:: 2. Check for Android Studio bundled JBR/JDK
set "STUDIO_JDK="
if exist "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe" (
    set "STUDIO_JDK=C:\Program Files\Android\Android Studio\jbr"
)

:: If Studio JDK exists, prioritize it for Gradle compatibility (Java 17/21)
if defined STUDIO_JDK (
    set "JAVA_HOME=%STUDIO_JDK%"
    set "PATH=%STUDIO_JDK%\bin;%PATH%"
    echo [*] Using Android Studio JDK: %STUDIO_JDK%
)

:: 3. Check if Android SDK was located
if defined SDK_FOUND (
    echo [*] Located Android SDK: %SDK_FOUND%
    echo sdk.dir=%SDK_FOUND:\=\\% > local.properties
    echo [*] Updated local.properties with SDK path.
    echo.
    echo [*] Compiling CleanStream debug APK...
    call "%~dp0gradle.bat" :app:assembleDebug
    
    if exist "%~dp0app\build\outputs\apk\debug\app-debug.apk" (
        echo.
        echo ======================================================================
        echo [SUCCESS] APK compiled successfully!
        echo APK Path: %~dp0app\build\outputs\apk\debug\app-debug.apk
        echo.
        echo Transfer this .apk to your phone via USB, Google Drive, or WhatsApp,
        echo then tap the file on your phone to install and test CleanStream!
        echo ======================================================================
        pause
        exit /b 0
    ) else (
        echo [!] Gradle build finished but APK was not found. Check logs above.
        pause
        exit /b 1
    )
)

:: 4. If SDK is not detected locally
echo [!] Android SDK was not detected in:
echo     - %LOCALAPPDATA%\Android\Sdk
echo     - ANDROID_HOME environment variable
echo.
echo ----------------------------------------------------------------------
echo  TWO EASY WAYS TO GET YOUR TEST APK:
echo ----------------------------------------------------------------------
echo.
echo  OPTION 1: Open in Android Studio (Recommended if you develop apps)
echo    1. Open Android Studio.
echo    2. Click File ^> Open ^> Select the "CleanStream" folder.
echo    3. Click Menu ^> Build ^> Build Bundle(s) / APK(s) ^> Build APK(s).
echo    4. Click "locate" when notified to open the generated APK file!
echo.
echo  OPTION 2: Cloud 1-Click Build via GitHub Actions (Zero Setup)
echo    1. Push this "CleanStream" folder to a GitHub repository.
echo    2. Go to the "Actions" tab on GitHub and click "Build Android APK".
echo    3. In ~90 seconds, download "CleanStream-v1.0.0-Debug-APK.zip",
echo       unzip and install the .apk on your phone!
echo ----------------------------------------------------------------------
echo.
pause
