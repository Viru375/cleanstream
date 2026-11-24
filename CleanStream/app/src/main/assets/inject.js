// inject.js — CleanStream ad/shorts/background-play injection script
// Feature flags are substituted at runtime by JsInjector.kt
const BLOCK_ADS = %%BLOCK_ADS%%;
const BLOCK_SHORTS = %%BLOCK_SHORTS%%;
const BACKGROUND_PLAY = %%BACKGROUND_PLAY%%;

let wasMutedByCleanStream = false;

// ─────────────────────────────────────────────────────────────
// 1. PROACTIVE AD STRIPPER — Hooks Fetch & Initial Response
// Removes ad definitions from /youtubei/v1/player before player sees them
// ─────────────────────────────────────────────────────────────
if (BLOCK_ADS) {
    (function setupProactiveStripper() {
        function stripAdData(data) {
            if (!data || typeof data !== 'object') return data;
            delete data.adPlacements;
            delete data.adSlots;
            delete data.playerAds;
            delete data.adBreakHeartbeatParams;
            if (data.playerConfig) delete data.playerConfig.adConfig;
            return data;
        }

        const originalFetch = window.fetch;
        if (originalFetch) {
            window.fetch = async function(...args) {
                const response = await originalFetch.apply(this, args);
                const url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url) || '';
                if (url.includes('/youtubei/v1/player')) {
                    try {
                        const clone = response.clone();
                        const json = await clone.json();
                        const cleaned = stripAdData(json);
                        return new Response(JSON.stringify(cleaned), {
                            status: response.status,
                            statusText: response.statusText,
                            headers: response.headers
                        });
                    } catch (_) {}
                }
                return response;
            };
        }

        let initialResponse = window.ytInitialPlayerResponse;
        if (initialResponse) stripAdData(initialResponse);
        Object.defineProperty(window, 'ytInitialPlayerResponse', {
            get: () => initialResponse,
            set: (val) => { initialResponse = stripAdData(val); },
            configurable: true
        });
    })();
}

// ─────────────────────────────────────────────────────────────
// 2. BACKGROUND PLAY — Override Page Visibility API
// ─────────────────────────────────────────────────────────────
if (BACKGROUND_PLAY) {
    Object.defineProperty(document, 'hidden', {
        get: () => false,
        configurable: true
    });
    Object.defineProperty(document, 'visibilityState', {
        get: () => 'visible',
        configurable: true
    });
    document.addEventListener('visibilitychange', function(e) {
        e.stopImmediatePropagation();
    }, true);
}

// ─────────────────────────────────────────────────────────────
// 3. INSTANT AD SKIPPING ENGINE (Zero Fast-Forwarding)
// ─────────────────────────────────────────────────────────────

function isAdCurrentlyActive() {
    const playerContainer = document.querySelector('.html5-video-player, #movie_player');
    const hasAdClass = Boolean(
        playerContainer &&
        playerContainer.classList &&
        (playerContainer.classList.contains('ad-showing') || playerContainer.classList.contains('ad-interrupting'))
    );
    const hasAdOverlay = Boolean(document.querySelector('.ytp-ad-player-overlay, .ad-showing, .ad-interrupting'));
    return Boolean(hasAdClass || hasAdOverlay);
}

function triggerAllSkipButtons() {
    const skipSelectors = [
        '.ytp-ad-skip-button',
        '.ytp-ad-skip-button-modern',
        '.ytp-skip-ad-button',
        '.ytp-ad-skip-button-slot button',
        'button.ytp-ad-skip-button-container',
        '.videoAdUiSkipButton',
        '.ytp-ad-overlay-close-button',
        'button[id^="skip-button:"]',
        '.ytp-ad-preview-container button'
    ];
    skipSelectors.forEach(function(selector) {
        document.querySelectorAll(selector).forEach(function(btn) {
            if (btn && typeof btn.click === 'function') {
                btn.click();
            }
        });
    });
}

function invokePlayerSkipAdApi() {
    const player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
    if (player && typeof player.skipAd === 'function') {
        try {
            player.skipAd();
        } catch (_) {}
    }
}

function instantSkipYouTubeAd() {
    if (!BLOCK_ADS) return;

    const video = document.querySelector('video');
    const adActive = isAdCurrentlyActive();

    if (video) {
        if (video.playbackRate > 2.0) {
            video.playbackRate = 1.0;
        }

        if (adActive) {
            if (!video.muted) {
                video.muted = true;
                wasMutedByCleanStream = true;
            }

            if (isFinite(video.duration) && video.duration > 0) {
                video.currentTime = video.duration;
            } else {
                video.currentTime = 999999;
            }

            invokePlayerSkipAdApi();
            triggerAllSkipButtons();
        } else if (wasMutedByCleanStream) {
            video.muted = false;
            wasMutedByCleanStream = false;
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 4. SHORTS REDIRECTOR
// ─────────────────────────────────────────────────────────────
function redirectShortsToWatch() {
    if (!BLOCK_SHORTS) return;

    const currentPath = window.location.pathname;
    if (!currentPath.startsWith('/shorts/')) return;

    const videoId = currentPath.replace('/shorts/', '').split('?')[0];
    if (videoId) {
        window.location.replace('/watch?v=' + videoId);
    }
}

// ─────────────────────────────────────────────────────────────
// 5. COSMETIC DOM FILTER — Element Selectors
// ─────────────────────────────────────────────────────────────
const AD_ELEMENT_SELECTORS = [
    '.ytm-promoted-sparkles-web-renderer',
    '.ytm-banner-promo-renderer',
    'ytm-paid-content-overlay-renderer',
    '.ytp-ad-player-overlay',
    '.ytp-ad-overlay-container',
    '.ytp-ad-text-overlay',
    '#player-ads',
    'ytm-companion-slot',
    '.ytm-promoted-video-renderer',
    'ytm-display-ad-renderer',
    'ytm-action-companion-ad-renderer',
    'ytm-companion-ad-renderer',
    '#masthead-ad',
    'yt-mealbar-promo-renderer',
    '.ytd-ad-slot-renderer'
];

const SHORTS_ELEMENT_SELECTORS = [
    'ytm-reel-shelf-renderer',
    'ytm-pivot-bar-item-renderer[tab-identifier="FEshorts"]',
    '[is-shorts]',
    'ytd-reel-shelf-renderer',
    'ytd-rich-shelf-renderer[is-shorts]',
    '#shorts-container',
    'ytm-rich-section-renderer:has(ytm-reel-shelf-renderer)'
];

function hideMatchingElements(selectorList) {
    selectorList.forEach(function(cssSelector) {
        try {
            document.querySelectorAll(cssSelector).forEach(function(element) {
                element.style.setProperty('display', 'none', 'important');
            });
        } catch (_) {}
    });
}

function runAllFilters() {
    if (BLOCK_ADS) {
        hideMatchingElements(AD_ELEMENT_SELECTORS);
        instantSkipYouTubeAd();
    }
    if (BLOCK_SHORTS) {
        hideMatchingElements(SHORTS_ELEMENT_SELECTORS);
        redirectShortsToWatch();
    }
}

runAllFilters();

document.addEventListener('timeupdate', function(e) {
    if (e.target && e.target.tagName === 'VIDEO') {
        instantSkipYouTubeAd();
    }
}, true);

const domChangeObserver = new MutationObserver(function() {
    runAllFilters();
});
domChangeObserver.observe(document.documentElement, {
    childList: true,
    subtree: true
});

window.addEventListener('yt-navigate-finish', function() {
    redirectShortsToWatch();
    runAllFilters();
});

setInterval(instantSkipYouTubeAd, 100);

console.log('[CleanStream] Proactive + instant ad-skip active.');
