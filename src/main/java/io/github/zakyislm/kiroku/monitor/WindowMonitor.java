package io.github.zakyislm.kiroku.monitor;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import io.github.zakyislm.kiroku.config.AppConfig;
import io.github.zakyislm.kiroku.config.ConfigManager;
import io.github.zakyislm.kiroku.engine.ScreenshotEngine;
import io.github.zakyislm.kiroku.utils.BrowserDetector;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WindowMonitor {
    private final MonitorListener listener;
    private Thread monitorThread;
    private boolean running = false;

    private String lastActiveTitle = "";
    private final Set<String> triggeredSessionIds = new HashSet<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Browser suffixes are dynamically determined by BrowserDetector

    public interface MonitorListener {
        void onWindowMatchFound(String matchName, String windowTitle);

        void onCaptureCompleted(int id, String timestamp, String filePath);

        void onLogMessage(String message);
    }

    public WindowMonitor(MonitorListener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        if (running)
            return;
        running = true;

        monitorThread = new Thread(() -> {
            listener.onLogMessage("Window monitor thread started");
            while (running) {
                try {
                    checkActiveWindow();
                    Thread.sleep(1500); // Poll every 1.5 seconds
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    listener.onLogMessage("Error in window monitor: " + e.getMessage());
                }
            }
        }, "Kiroku-WindowMonitor");

        monitorThread.start();
    }

    public synchronized void stop() {
        if (!running)
            return;
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
        triggeredSessionIds.clear();
        ScreenshotEngine.resetSessionTracking();
        listener.onLogMessage("Window monitor thread stopped");
    }

    private void checkActiveWindow() {
        AppConfig config = ConfigManager.getConfig();
        if (!config.isEnabled)
            return;

        char[] buffer = new char[1024];
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null)
            return;

        User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);
        String windowTitle = Native.toString(buffer).trim();

        if (windowTitle.isEmpty())
            return;

        // Check if title has changed to avoid spamming logs
        if (!windowTitle.equals(lastActiveTitle)) {
            lastActiveTitle = windowTitle;
        }

        // 1. Detect if foreground window matches any of the enabled browsers
        boolean browserMatch = false;
        String cleanPageTitle = windowTitle;

        java.util.List<BrowserDetector.DetectedBrowser> detectedBrowsers = BrowserDetector.detectBrowsers();
        for (String browserName : config.monitoredBrowsers) {
            BrowserDetector.DetectedBrowser found = null;
            for (BrowserDetector.DetectedBrowser b : detectedBrowsers) {
                if (b.name.equalsIgnoreCase(browserName)) {
                    found = b;
                    break;
                }
            }

            if (found != null && windowTitle.endsWith(found.suffix)) {
                browserMatch = true;
                cleanPageTitle = windowTitle.substring(0, windowTitle.length() - found.suffix.length());
                break;
            }
        }

        if (!browserMatch)
            return;

        // 2. Try to match cleanPageTitle against enabled whitelist entries
        AppConfig.WhitelistEntry matchedEntry = null;
        for (AppConfig.WhitelistEntry entry : config.whitelist) {
            if (!entry.enabled)
                continue;

            String nameLower = entry.name.toLowerCase();
            String hostLower = entry.baseUrl.toLowerCase().replace("www.", "");
            String pageLower = cleanPageTitle.toLowerCase();

            boolean matchesKeyword = pageLower.contains(nameLower) ||
                    pageLower.contains(hostLower) ||
                    (hostLower.contains("meet") && pageLower.contains("meet")) ||
                    (hostLower.contains("zoom") && pageLower.contains("zoom")) ||
                    (hostLower.contains("teams") && pageLower.contains("teams")) ||
                    (hostLower.contains("quizizz") && pageLower.contains("quizizz"));

            if (matchesKeyword) {
                matchedEntry = entry;
                break;
            }
        }

        if (matchedEntry == null)
            return;

        // 3. Prevent duplicate triggering for the same session ID
        String sessionId = ScreenshotEngine.determineSessionId(windowTitle, null);
        if (!sessionId.isEmpty() && triggeredSessionIds.contains(sessionId)) {
            return;
        }

        triggeredSessionIds.add(sessionId);
        listener.onWindowMatchFound(matchedEntry.name, windowTitle);

        scheduleCaptures(matchedEntry.name, windowTitle);
    }

    private void scheduleCaptures(String matchName, String windowTitle) {
        AppConfig config = ConfigManager.getConfig();
        int delayMin = config.timerMinutes;
        int count = config.continuousCapture ? 9999 : config.screenshotCount;
        String mode = config.continuousCapture ? "bertahap" : config.screenshotMode;
        int intervalMin = config.intervalMinutes;

        listener.onLogMessage(String.format(
                "Scheduling screenshot for match: '%s' (Delay: %d min, Count: %d, Mode: %s, Continuous: %b)",
                matchName, delayMin, count, mode, config.continuousCapture));

        scheduler.schedule(() -> {
            try {
                if ("serentak".equalsIgnoreCase(mode)) {
                    listener.onLogMessage("Executing simultaneous captures (" + count + " times)...");
                    for (int i = 0; i < count; i++) {
                        executeCapture(windowTitle, i + 1, count);
                        if (i < count - 1) {
                            Thread.sleep(1000);
                        }
                    }
                } else {
                    listener.onLogMessage("Starting sequential captures (1/" + count + ")...");
                    executeCapture(windowTitle, 1, count);

                    for (int i = 2; i <= count; i++) {
                        final int index = i;
                        scheduler.schedule(() -> {
                            listener.onLogMessage(
                                    String.format("Executing sequential capture (%d/%d)...", index, count));
                            executeCapture(windowTitle, index, count);
                        }, (long) (index - 1) * intervalMin, TimeUnit.MINUTES);
                    }
                }
            } catch (Exception e) {
                listener.onLogMessage("Capture execution scheduling failed: " + e.getMessage());
            }
        }, delayMin, TimeUnit.MINUTES);
    }

    private void executeCapture(String windowTitle, int index, int total) {
        AppConfig config = ConfigManager.getConfig();
        int limit = config.continuousCapture ? Integer.MAX_VALUE : config.screenshotCount;
        ScreenshotEngine.CaptureResult res = ScreenshotEngine.captureScreen(config.rootDir, windowTitle, null, limit);
        if (res.success) {
            listener.onLogMessage(String.format("Saved: %s (#%d) [%d/%d]", res.file.getName(), res.id, index, total));
            listener.onCaptureCompleted(res.id, res.timestamp, res.file.getAbsolutePath());
        } else {
            listener.onLogMessage("Error: Screenshot failed: " + res.errorMessage);
        }
    }
}
