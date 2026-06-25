package io.github.zakyislm.kiroku.config;

import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    public List<WhitelistEntry> whitelist = new ArrayList<>();
    public int timerMinutes = 5;
    public int screenshotCount = 1;
    public String screenshotMode = "serentak"; // "serentak" | "bertahap"
    public int intervalMinutes = 2;
    public boolean isEnabled = false;
    public String rootDir = "";
    public List<String> monitoredBrowsers = new ArrayList<>();
    public boolean autoStart = false;
    public boolean runInBackground = false;
    public boolean continuousCapture = false;

    public static class WhitelistEntry {
        public String id;
        public String name;
        public String baseUrl;
        public boolean hasParameter;
        public String parameterPattern;
        public boolean enabled;

        @Override
        public String toString() {
            return name + " (" + baseUrl + ")";
        }
    }
}
