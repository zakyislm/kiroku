package io.github.zakyislm.kiroku.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zakyislm.kiroku.utils.FormatUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File(System.getProperty("user.home"), ".kiroku");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "config.Kiroku");

    private static AppConfig currentConfig;

    public static AppConfig getConfig() {
        if (currentConfig == null) {
            loadConfig();
        }
        return currentConfig;
    }

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            currentConfig = createDefaultConfig();
            saveConfig();
            return;
        }

        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            byte[] bytes = fis.readAllBytes();
            String json = FormatUtils.parseConfigFile(bytes);
            if (json != null) {
                try {
                    currentConfig = GSON.fromJson(json, AppConfig.class);
                } catch (Exception e) {
                    System.err.println(
                            "XOR decryption parsed config is invalid JSON, attempting plain payload fallback...");
                    // Try parsing as plain payload inside the KIRO structure
                    String plainPayload = FormatUtils.parseConfigFilePlain(bytes);
                    if (plainPayload != null) {
                        try {
                            currentConfig = GSON.fromJson(plainPayload, AppConfig.class);
                            if (currentConfig != null) {
                                System.out.println(
                                        "Successfully migrated plain payload KIRO config to obfuscated format.");
                                saveConfig();
                            }
                        } catch (Exception ex) {
                            System.err.println("Plain payload KIRO config parsing failed: " + ex.getMessage());
                        }
                    }
                }
            } else {
                // Fallback: try parsing the entire file as plain JSON
                String plainJson = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                try {
                    currentConfig = GSON.fromJson(plainJson, AppConfig.class);
                    if (currentConfig != null && currentConfig.whitelist != null) {
                        System.out.println("Successfully migrated legacy raw JSON config to obfuscated format.");
                        saveConfig();
                    }
                } catch (Exception ex) {
                    System.err.println("Legacy raw JSON parsing failed: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }

        if (currentConfig == null) {
            currentConfig = createDefaultConfig();
        }
    }

    public static void saveConfig() {
        if (currentConfig == null) {
            currentConfig = createDefaultConfig();
        }

        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }

            String json = GSON.toJson(currentConfig);
            byte[] bytes = FormatUtils.writeConfigFile(json);

            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                fos.write(bytes);
            }
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    private static AppConfig createDefaultConfig() {
        AppConfig config = new AppConfig();
        config.rootDir = new File(System.getProperty("user.home"), "Pictures" + File.separator + "Kiroku")
                .getAbsolutePath();
        config.monitoredBrowsers = new ArrayList<>(Arrays.asList("Chrome", "Edge"));
        config.whitelist = new ArrayList<>();

        // Add some default presets
        config.whitelist.add(createPreset("Google Meet", "meet.google.com", "^\\/[a-z]{3}-[a-z]{4}-[a-z]{3}"));
        config.whitelist.add(createPreset("Zoom", "zoom.us", "^\\/j\\/\\d{9,11}"));

        config.timerMinutes = 5;
        config.screenshotCount = 1;
        config.screenshotMode = "serentak";
        config.intervalMinutes = 2;
        config.isEnabled = false;
        config.autoStart = false;
        config.runInBackground = false;
        config.continuousCapture = false;
        return config;
    }

    private static AppConfig.WhitelistEntry createPreset(String name, String baseUrl, String pattern) {
        AppConfig.WhitelistEntry entry = new AppConfig.WhitelistEntry();
        entry.id = Long.toString(System.currentTimeMillis()) + "_" + (int) (Math.random() * 1000);
        entry.name = name;
        entry.baseUrl = baseUrl;
        entry.hasParameter = pattern != null && !pattern.isEmpty();
        entry.parameterPattern = pattern;
        entry.enabled = true;
        return entry;
    }
}
