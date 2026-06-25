package io.github.zakyislm.kiroku.engine;

import io.github.zakyislm.kiroku.utils.FormatUtils;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ScreenshotEngine {

    public static class CaptureResult {
        public File file;
        public int id;
        public String timestamp;
        public boolean success;
        public String errorMessage;
    }

    private static final Map<String, Integer> sessionCaptureCounts = new HashMap<>();

    public static synchronized void resetSessionTracking() {
        sessionCaptureCounts.clear();
    }

    public static String determineSessionId(String title, String url) {
        String combined = "";
        if (url != null && !url.isEmpty()) {
            combined += url;
        }
        if (title != null && !title.isEmpty()) {
            combined += " " + title;
        }
        if (combined.isEmpty()) {
            return "";
        }

        // Clean common parentheses count badges like (3), (10+), (1)
        combined = combined.replaceAll("\\(\\d+\\+?\\)", "");

        // 1. Try to extract common patterns like Google Meet code or Zoom meeting ID
        // Google Meet: abc-defg-hij (case-insensitive)
        java.util.regex.Pattern meetPattern = java.util.regex.Pattern.compile("[a-zA-Z]{3}-[a-zA-Z]{4}-[a-zA-Z]{3}");
        java.util.regex.Matcher meetMatcher = meetPattern.matcher(combined);
        if (meetMatcher.find()) {
            return "meet:" + meetMatcher.group().toLowerCase();
        }

        // Zoom: j/1234567890 or zoom meeting title with numbers
        java.util.regex.Pattern zoomPattern = java.util.regex.Pattern.compile("\\b\\d{9,11}\\b");
        java.util.regex.Matcher zoomMatcher = zoomPattern.matcher(combined);
        if (zoomMatcher.find()) {
            return "zoom:" + zoomMatcher.group();
        }

        // 2. Fallback normalization
        String key = combined.toLowerCase().trim();
        // Remove browser suffixes
        String[] suffixes = { " - google chrome", " - microsoft edge", " - mozilla firefox", " - brave", " - opera" };
        for (String suffix : suffixes) {
            if (key.endsWith(suffix)) {
                key = key.substring(0, key.length() - suffix.length());
            }
        }
        // Clean URL protocols
        key = key.replace("https://", "").replace("http://", "").replace("www.", "");

        if (key.length() > 60) {
            key = key.substring(0, 60);
        }
        return key.trim();
    }

    public static synchronized CaptureResult captureScreen(String rootDirPath) {
        return captureScreen(rootDirPath, null, null, Integer.MAX_VALUE);
    }

    public static synchronized CaptureResult captureScreen(String rootDirPath, String title, String url, int limit) {
        CaptureResult result = new CaptureResult();

        // Check session capture limit (Force Capture or manual overrides bypass this check)
        boolean isManual = (title == null && url == null) || "Force Capture".equals(title);
        if (!isManual) {
            String sessionId = determineSessionId(title, url);
            if (!sessionId.isEmpty()) {
                int count = sessionCaptureCounts.getOrDefault(sessionId, 0);
                if (count >= limit) {
                    result.success = false;
                    result.errorMessage = "Session capture limit reached (" + limit + ")";
                    return result;
                }
                sessionCaptureCounts.put(sessionId, count + 1);
            }
        }

        try {
            File rootDir = new File(rootDirPath);
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }

            LocalDateTime now = LocalDateTime.now();

            // Format folder path: /Kiroku/{YYYY}/{MM}/{DD}/
            String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String month = now.format(DateTimeFormatter.ofPattern("MM"));
            String day = now.format(DateTimeFormatter.ofPattern("dd"));

            File dateFolder = new File(rootDir, year + File.separator + month + File.separator + day);
            if (!dateFolder.exists()) {
                dateFolder.mkdirs();
            }

            // Sync/get current ID
            int currentId = 0;
            File todaySysFile = new File(dateFolder, "system.Kiroku");

            if (todaySysFile.exists()) {
                currentId = readIdFromFile(todaySysFile);
            } else {
                // Search previous folders for latest ID
                currentId = findLatestId(rootDir);
            }

            int newId = currentId + 1;

            // Capture screen
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            Robot robot = new Robot();
            BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

            // Format filename: Kiroku_HH_MM_SS_{ID}.png
            String hh = now.format(DateTimeFormatter.ofPattern("HH"));
            String mm = now.format(DateTimeFormatter.ofPattern("mm"));
            String ss = now.format(DateTimeFormatter.ofPattern("ss"));
            String fileName = String.format("Kiroku_%s_%s_%s_%d.png", hh, mm, ss, newId);

            File outputFile = new File(dateFolder, fileName);
            ImageIO.write(screenFullImage, "png", outputFile);

            // Write new ID to today's system.Kiroku
            byte[] sysBytes = FormatUtils.writeSystemFile(newId);
            try (FileOutputStream fos = new FileOutputStream(todaySysFile)) {
                fos.write(sysBytes);
            }

            result.file = outputFile;
            result.id = newId;
            result.timestamp = String.format("%s:%s:%s", hh, mm, ss);
            result.success = true;
            return result;
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            return result;
        }
    }

    private static int readIdFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = fis.readAllBytes();
            return FormatUtils.parseSystemFile(bytes);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int findLatestId(File rootDir) {
        int maxId = 0;
        try {
            maxId = scanForLatestId(rootDir);
        } catch (Exception e) {
            // Ignore
        }
        return maxId;
    }

    private static int scanForLatestId(File dir) {
        int maxId = 0;
        File[] files = dir.listFiles();
        if (files == null)
            return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                int id = scanForLatestId(file);
                if (id > maxId) {
                    maxId = id;
                }
            } else if (file.getName().equals("system.Kiroku")) {
                int id = readIdFromFile(file);
                if (id > maxId) {
                    maxId = id;
                }
            }
        }
        return maxId;
    }
}
