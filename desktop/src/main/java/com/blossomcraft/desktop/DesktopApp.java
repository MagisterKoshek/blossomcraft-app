package com.blossomcraft.desktop;

import com.blossomcraft.core.ApiConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Desktop (PC) entry point for the BlossomCraft app.
 *
 * <p>The desktop app embeds the live BlossomCraft website inside a JavaFX
 * {@link WebView}, so the interface and every feature match the site exactly —
 * it <em>is</em> the site, running in a real application window. The site URL is
 * derived from the configured API base ({@link ApiConfig#getSiteUrl()}).</p>
 *
 * <p><b>Google login.</b> Google refuses OAuth inside embedded browsers, so the
 * "Войти через Google" button does not run in the WebView. The web page (which
 * detects the {@code BlossomCraftApp} user-agent marker) calls a small Java
 * bridge ({@code window.bcNative.googleLogin()}); we start a one-shot loopback
 * HTTP listener on {@code 127.0.0.1} and open the user's real system browser on
 * the website login page with a one-time {@code state} nonce and a
 * {@code redirect} pointing back at the loopback. The website performs the
 * normal Google sign-in and redirects the issued session token to the loopback;
 * we verify the nonce, inject the token into the WebView's {@code localStorage}
 * and reload — signed in with the exact session the website would issue.</p>
 *
 * <p>A {@code locationProperty} listener for {@code blossomcraft://google-login}
 * is kept as a fallback in case the bridge is unavailable.</p>
 *
 * <p>Run with:
 * {@code ./gradlew :desktop:run -Pblossomcraft.api.base=https://your-host/api}</p>
 */
public class DesktopApp extends Application {

    /** A standard desktop Chrome user agent (helps site compatibility and sign-in). */
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final String GOOGLE_LOGIN_SIGNAL = "blossomcraft://google-login";

    /** Listener lifetime and per-connection read budget. */
    private static final int OAUTH_TIMEOUT_MS = 5 * 60 * 1000;
    private static final int ACCEPT_SLICE_MS = 30 * 1000;
    private static final int SOCKET_READ_MS = 10 * 1000;

    private static final String SUCCESS_HTML =
            "<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\">"
                    + "<title>BlossomCraft</title></head>"
                    + "<body style=\"font-family:sans-serif;background:#0a0a0b;color:#e5e7eb;"
                    + "text-align:center;padding-top:80px\">"
                    + "<h2 style=\"color:#3b82f6\">BlossomCraft</h2>"
                    + "<p>Готово. Закройте эту вкладку и вернитесь в приложение.</p>"
                    + "</body></html>";

    private WebEngine engine;
    /** Bridge exposed to the page as {@code window.bcNative}. */
    private final Bridge bridge = new Bridge();
    /** Guards against starting more than one loopback listener at a time. */
    private volatile boolean oauthInProgress;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("BlossomCraft");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        WebView webView = new WebView();
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        // Append our marker so the website hands Google login off to the system browser.
        engine.setUserAgent(DESKTOP_USER_AGENT + " BlossomCraftApp");

        // Expose the native bridge on every successful page load. JS members are
        // cleared on navigation, so we re-install after each load.
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("bcNative", bridge);
                } catch (RuntimeException ignored) {
                    // bridge unavailable — the scheme fallback below still works
                }
            }
        });

        // Fallback: if the page navigates to the custom scheme (bridge missing),
        // catch the location change, open the system browser, and recover.
        engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc != null && newLoc.startsWith(GOOGLE_LOGIN_SIGNAL)) {
                startExternalGoogleLogin();
                Platform.runLater(() -> engine.load(ApiConfig.getSiteUrl() + "/login"));
            }
        });

        engine.load(ApiConfig.getSiteUrl());

        StackPane root = new StackPane(webView);
        Scene scene = new Scene(root, 1280, 820);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    /** Bridge object the web page calls to trigger the system-browser login. */
    public final class Bridge {
        /** Invoked from JS (on the FX thread) as {@code window.bcNative.googleLogin()}. */
        public void googleLogin() {
            startExternalGoogleLogin();
        }
    }

    /**
     * Starts the loopback listener and opens the system browser on the website
     * login page so the user can sign in with Google there.
     */
    private void startExternalGoogleLogin() {
        if (oauthInProgress) {
            return;
        }
        oauthInProgress = true;

        final String nonce = newNonce();
        final ServerSocket server;
        try {
            server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            oauthInProgress = false;
            return;
        }

        int port = server.getLocalPort();
        String redirect = "http://127.0.0.1:" + port + "/cb";
        String url = ApiConfig.getSiteUrl() + "/login?app_auth=1"
                + "&redirect=" + urlEncode(redirect)
                + "&state=" + urlEncode(nonce);

        Thread worker = new Thread(() -> {
            try {
                String token = awaitToken(server, nonce);
                if (token != null && token.matches("[A-Za-z0-9._-]+")) {
                    final String safe = token;
                    Platform.runLater(() -> {
                        try {
                            engine.executeScript("localStorage.setItem('auth_token','" + safe + "')");
                        } catch (RuntimeException ignored) {
                            // ignore script failures; reload still attempts recovery
                        }
                        engine.load(ApiConfig.getSiteUrl());
                    });
                }
            } finally {
                closeQuietly(server);
                oauthInProgress = false;
            }
        }, "bc-oauth-loopback");
        worker.setDaemon(true);
        worker.start();

        // Open the user's default system browser.
        try {
            getHostServices().showDocument(url);
        } catch (RuntimeException ignored) {
            // If the browser cannot be opened the listener simply times out.
        }
    }

    /**
     * Accepts loopback connections until the callback arrives or the deadline
     * passes. Browsers open speculative preconnect sockets that never send a
     * request, so we bound {@code accept()} per slice and give each connection a
     * short read timeout, ignoring anything that is not the {@code /cb} callback.
     */
    private static String awaitToken(ServerSocket server, String expectedState) {
        long deadline = System.currentTimeMillis() + OAUTH_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            int slice = (int) Math.max(1000, Math.min(ACCEPT_SLICE_MS, remaining));
            try {
                server.setSoTimeout(slice);
                Socket socket = server.accept();
                try {
                    socket.setSoTimeout(SOCKET_READ_MS);
                    String token = handleConnection(socket, expectedState);
                    if (token != null) {
                        return token;
                    }
                } catch (IOException perConnection) {
                    // preconnect / slow / malformed request — keep waiting
                } finally {
                    closeQuietly(socket);
                }
            } catch (SocketTimeoutException acceptTimeout) {
                // no connection this slice — re-check the deadline
            } catch (IOException fatal) {
                return null; // server closed or unrecoverable
            }
        }
        return null;
    }

    /**
     * Reads one HTTP request, replies, and returns the token only when the path
     * is {@code /cb} and {@code state} matches the nonce; otherwise null.
     */
    private static String handleConnection(Socket socket, String expectedState) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        String requestLine = in.readLine(); // e.g. "GET /cb?token=..&state=.. HTTP/1.1"
        if (requestLine == null) {
            return null;
        }
        String[] parts = requestLine.split(" ");
        String path = parts.length >= 2 ? parts[1] : "";
        if (!path.startsWith("/cb")) {
            writeResponse(socket, "404 Not Found", "<html><body>Not found</body></html>");
            return null;
        }

        int q = path.indexOf('?');
        String query = q >= 0 ? path.substring(q + 1) : "";
        String token = queryParam(query, "token");
        String state = queryParam(query, "state");

        writeResponse(socket, "200 OK", SUCCESS_HTML);

        if (token != null && expectedState.equals(state)) {
            return token;
        }
        return null;
    }

    private static void writeResponse(Socket socket, String status, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 " + status + "\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static String queryParam(String query, String key) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = pair.substring(0, eq);
            if (name.equals(key)) {
                try {
                    return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
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

    private static void closeQuietly(ServerSocket server) {
        try {
            server.close();
        } catch (IOException ignored) {
            // nothing to do
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing to do
        }
    }

    @Override
    public void stop() {
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
