package io.github.zakyislm.kiroku.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.zakyislm.kiroku.config.ConfigManager;
import io.github.zakyislm.kiroku.engine.ScreenshotEngine;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class LocalServer {
    private HttpServer server;
    private final int port = 18210;
    private final ServerListener listener;

    public interface ServerListener {
        void onCaptureRequested(String source, String matchName, String url);
        void onLogMessage(String message);
        void onCaptureCompleted(int id, String timestamp, String filePath);
    }

    public LocalServer(ServerListener listener) {
        this.listener = listener;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/capture", new CaptureHandler());
            server.createContext("/status", new StatusHandler());
            server.setExecutor(null);
            server.start();
            listener.onLogMessage("Local server started on port " + port);
        } catch (IOException e) {
            listener.onLogMessage("Failed to start local server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            listener.onLogMessage("Local server stopped");
        }
    }

    private void sendCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private class CaptureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                if (!ConfigManager.getConfig().isEnabled) {
                    String response = "{\"success\":false,\"error\":\"Monitoring is disabled in Desktop app\"}";
                    byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, respBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(respBytes);
                    }
                    return;
                }

                JsonObject json = JsonParser.parseReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();
                String url = json.has("url") ? json.get("url").getAsString() : "";
                String name = json.has("name") ? json.get("name").getAsString() : "Extension Trigger";
                String source = json.has("source") ? json.get("source").getAsString() : "extension";

                listener.onCaptureRequested(source, name, url);

                int limit = ConfigManager.getConfig().continuousCapture ? Integer.MAX_VALUE : ConfigManager.getConfig().screenshotCount;
                ScreenshotEngine.CaptureResult captureResult = ScreenshotEngine.captureScreen(
                        ConfigManager.getConfig().rootDir, 
                        name, 
                        url, 
                        limit
                );

                String response;
                if (captureResult.success) {
                    response = String.format("{\"success\":true,\"id\":%d,\"file\":\"%s\",\"timestamp\":\"%s\"}",
                            captureResult.id,
                            captureResult.file.getAbsolutePath().replace("\\", "\\\\"),
                            captureResult.timestamp);
                    listener.onCaptureCompleted(captureResult.id, captureResult.timestamp, captureResult.file.getAbsolutePath());
                } else {
                    response = String.format("{\"success\":false,\"error\":\"%s\"}", captureResult.errorMessage);
                }

                byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(respBytes);
                }
            } catch (Exception e) {
                String response = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
                byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, respBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(respBytes);
                }
            }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String response = String.format("{\"success\":true,\"monitoringEnabled\":%b}", ConfigManager.getConfig().isEnabled);
            byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        }
    }
}
