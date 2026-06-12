package com.blossomcraft.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.blossomcraft.core.ApiConfig;

import java.security.SecureRandom;

/**
 * Single-screen Android app that embeds the live BlossomCraft website in a
 * full-screen {@link WebView}. The interface and every feature match the site
 * exactly because it <em>is</em> the site. The URL comes from
 * {@link ApiConfig#getSiteUrl()} (set from the {@code api_base_url} resource in
 * {@link BlossomApp}).
 *
 * <p><b>Google login.</b> Google refuses OAuth inside embedded WebViews, so the
 * "Войти через Google" button does not run in the WebView. Instead the web page
 * (which detects the {@code BlossomCraftApp} user-agent marker) navigates to
 * {@code blossomcraft://google-login}. We intercept that, open the user's real
 * system browser on the website login page with a one-time {@code state} nonce,
 * and the website redirects the issued session token back to us via the
 * {@code blossomcraft://auth?token=..&state=..} deep link. We verify the nonce,
 * inject the token into the WebView's {@code localStorage} and reload — the user
 * is now signed in, using the exact same session the website would issue.</p>
 */
public class WebActivity extends AppCompatActivity {

    private static final String OAUTH_PREFS = "bc_oauth";
    private static final String OAUTH_STATE_KEY = "state";
    /** Where the website sends the token back to (matches the manifest deep link). */
    private static final String APP_REDIRECT = "blossomcraft://auth";

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    /** Token captured on a cold start, injected once the page has finished loading. */
    private String pendingToken;

    private final ActivityResultLauncher<Intent> fileChooser =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback == null) {
                    return;
                }
                Uri[] uris = null;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        uris = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            uris[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        uris = new Uri[]{data.getData()};
                    }
                }
                filePathCallback.onReceiveValue(uris);
                filePathCallback = null;
            });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // Present a standard Chrome user agent (without the "wv" WebView marker)
        // and append our own marker so the website can detect that it is running
        // inside the app and hand Google login off to the system browser.
        String ua = settings.getUserAgentString();
        if (ua != null) {
            settings.setUserAgentString(ua.replace("; wv", "") + " BlossomCraftApp");
        }

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request != null ? request.getUrl() : null;
                if (url != null && "blossomcraft".equals(url.getScheme())) {
                    if ("google-login".equals(url.getHost())) {
                        startExternalGoogleLogin();
                        return true;
                    }
                    if ("auth".equals(url.getHost())) {
                        // Defensive: handle the token even if it ever lands in the WebView.
                        handleAuthRedirect(new Intent(Intent.ACTION_VIEW, url), true);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (pendingToken != null) {
                    String token = pendingToken;
                    pendingToken = null;
                    injectToken(token);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                try {
                    fileChooser.launch(intent);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Deny-by-default: only grant web permissions to our own trusted
                // HTTPS origin, never to arbitrary cross-origin frames.
                Uri origin = request.getOrigin();
                String trustedHost = Uri.parse(ApiConfig.getSiteUrl()).getHost();
                if (origin != null && trustedHost != null
                        && "https".equalsIgnoreCase(origin.getScheme())
                        && trustedHost.equalsIgnoreCase(origin.getHost())) {
                    request.grant(request.getResources());
                } else {
                    request.deny();
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // If we were launched (cold start) by the auth deep link, capture the token
        // and let onPageFinished inject it once the site has loaded.
        handleAuthRedirect(getIntent(), false);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(ApiConfig.getSiteUrl());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // The activity is already alive (singleTask) and showing the site, so the
        // token can be injected straight into the current page.
        handleAuthRedirect(intent, true);
    }

    /**
     * Opens the system browser on the website login page so the user can sign in
     * with Google there (where Google permits OAuth). A one-time {@code state}
     * nonce is stored to verify the redirect that comes back.
     */
    private void startExternalGoogleLogin() {
        String nonce = newNonce();
        getSharedPreferences(OAUTH_PREFS, MODE_PRIVATE)
                .edit().putString(OAUTH_STATE_KEY, nonce).apply();

        String url = ApiConfig.getSiteUrl() + "/login?app_auth=1"
                + "&redirect=" + Uri.encode(APP_REDIRECT)
                + "&state=" + Uri.encode(nonce);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
            // No browser available — nothing more we can do here.
        }
    }

    /**
     * Validates and consumes a {@code blossomcraft://auth?token=..&state=..}
     * redirect. Returns {@code true} when a valid token was accepted.
     *
     * @param pageReady {@code true} if the WebView already shows the site (inject
     *                  now); {@code false} on cold start (defer to onPageFinished).
     */
    private boolean handleAuthRedirect(Intent intent, boolean pageReady) {
        if (intent == null) {
            return false;
        }
        Uri data = intent.getData();
        if (data == null
                || !"blossomcraft".equals(data.getScheme())
                || !"auth".equals(data.getHost())) {
            return false;
        }
        String token = data.getQueryParameter("token");
        String state = data.getQueryParameter("state");

        SharedPreferences prefs = getSharedPreferences(OAUTH_PREFS, MODE_PRIVATE);
        String expected = prefs.getString(OAUTH_STATE_KEY, null);
        prefs.edit().remove(OAUTH_STATE_KEY).apply();

        // Consume the intent so a relaunch/config change cannot replay it.
        intent.setData(null);

        if (token == null || expected == null || !expected.equals(state)) {
            return false;
        }
        // The session token is hex (bin2hex). Reject anything outside a safe set
        // before it is embedded into a JS string literal.
        if (!token.matches("[A-Za-z0-9._-]+")) {
            return false;
        }

        if (pageReady) {
            injectToken(token);
        } else {
            pendingToken = token;
        }
        return true;
    }

    /** Writes the session token into the site's localStorage and reloads. */
    private void injectToken(String token) {
        String js = "(function(){try{localStorage.setItem('auth_token','" + token + "');}catch(e){}})();";
        webView.evaluateJavascript(js, value -> webView.loadUrl(ApiConfig.getSiteUrl()));
    }

    private static String newNonce() {
        byte[] buf = new byte[16];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder(buf.length * 2);
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }
}
