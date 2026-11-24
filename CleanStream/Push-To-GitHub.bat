@echo off
setlocal
title CleanStream — Push to GitHub

echo ======================================================================
echo                CleanStream: Push Repository to GitHub
echo ======================================================================
echo.

cd /d "%~dp0"

echo [*] Staging all CleanStream Android project files...
git add -A

git diff-index --quiet HEAD --
if %ERRORLEVEL% NEQ 0 (
    echo [*] Committing local changes...
    git commit -m "CleanStream Android App: Zero-ad player, background audio & system VPN"
) else (
    echo [*] All local files already committed.
)

echo.
echo [*] Current Remote Target:
git remote -v
echo.
echo [*] Attempting to push to branch main on GitHub...
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================================================
    echo [SUCCESS] All files pushed to GitHub successfully!
    echo.
    echo GitHub Actions CI is now building your Android APK automatically!
    echo Track build and download APK here:
    echo https://github.com/virup0787/cleanstream/actions
    echo ======================================================================
) else (
    echo.
    echo ======================================================================
    echo [!] Push failed because repository 'cleanstream' does not exist yet!
    echo.
    echo Quick 10-second fix:
    echo 1. Open this link in your browser:
    echo    https://github.com/new?name=cleanstream
    echo 2. Click the green "Create repository" button at the bottom.
    echo    (Do NOT check 'Add a README file')
    echo 3. Run this script (Push-To-GitHub.bat) again!
    echo ======================================================================
)

echo.
pause
