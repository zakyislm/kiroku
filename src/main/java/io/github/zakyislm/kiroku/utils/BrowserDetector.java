package io.github.zakyislm.kiroku.utils;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BrowserDetector {
    public static class DetectedBrowser {
        public String name;       // Friendly Name, e.g. "Chrome"
        public String suffix;     // Window title suffix, e.g. " - Google Chrome"
        public String path;       // Executable path, e.g. "C:\...\chrome.exe"

        public DetectedBrowser(String name, String suffix, String path) {
            this.name = name;
            this.suffix = suffix;
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DetectedBrowser)) return false;
            DetectedBrowser that = (DetectedBrowser) o;
            return name.equalsIgnoreCase(that.name);
        }

        @Override
        public int hashCode() {
            return name.toLowerCase().hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static List<DetectedBrowser> detectBrowsers() {
        Set<DetectedBrowser> browsers = new LinkedHashSet<>();
        
        // Registry path for start menu internet clients
        String regPath = "SOFTWARE\\Clients\\StartMenuInternet";
        
        // HKLM Check
        try {
            if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, regPath)) {
                String[] keys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, regPath);
                for (String key : keys) {
                    processBrowserRegistryKey(WinReg.HKEY_LOCAL_MACHINE, regPath + "\\" + key, browsers);
                }
            }
        } catch (Exception e) {
            // Ignore registry errors
        }
        
        // HKCU Check
        try {
            if (Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, regPath)) {
                String[] keys = Advapi32Util.registryGetKeys(WinReg.HKEY_CURRENT_USER, regPath);
                for (String key : keys) {
                    processBrowserRegistryKey(WinReg.HKEY_CURRENT_USER, regPath + "\\" + key, browsers);
                }
            }
        } catch (Exception e) {
            // Ignore registry errors
        }

        // Fallback common installation checks (in case registry access is restricted)
        addFallbackBrowser(browsers, "Chrome", " - Google Chrome", "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        addFallbackBrowser(browsers, "Chrome", " - Google Chrome", "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
        addFallbackBrowser(browsers, "Edge", " - Microsoft Edge", "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        addFallbackBrowser(browsers, "Firefox", " - Mozilla Firefox", "C:\\Program Files\\Mozilla Firefox\\firefox.exe");
        addFallbackBrowser(browsers, "Brave", " - Brave", "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe");
        addFallbackBrowser(browsers, "Brave", " - Brave", System.getProperty("user.home") + "\\AppData\\Local\\BraveSoftware\\Brave-Browser\\Application\\brave.exe");
        addFallbackBrowser(browsers, "Opera", " - Opera", System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Opera\\launcher.exe");

        return new ArrayList<>(browsers);
    }

    private static void processBrowserRegistryKey(WinReg.HKEY root, String keyPath, Set<DetectedBrowser> list) {
        try {
            // Retrieve Friendly Name (Default value of the key)
            String friendlyName = "";
            if (Advapi32Util.registryValueExists(root, keyPath, "")) {
                friendlyName = Advapi32Util.registryGetStringValue(root, keyPath, "");
            }
            if (friendlyName == null || friendlyName.isEmpty()) {
                friendlyName = keyPath.substring(keyPath.lastIndexOf("\\") + 1);
            }

            String name = cleanBrowserName(friendlyName);

            // Retrieve command line exe path
            String commandPath = "";
            String commandKey = keyPath + "\\shell\\open\\command";
            if (Advapi32Util.registryKeyExists(root, commandKey)) {
                commandPath = Advapi32Util.registryGetStringValue(root, commandKey, "");
            }

            if (commandPath != null && !commandPath.isEmpty()) {
                // Strip quotes and arguments
                if (commandPath.startsWith("\"")) {
                    int nextQuote = commandPath.indexOf("\"", 1);
                    if (nextQuote != -1) {
                        commandPath = commandPath.substring(1, nextQuote);
                    }
                } else {
                    int spaceIdx = commandPath.indexOf(" ");
                    if (spaceIdx != -1) {
                        commandPath = commandPath.substring(0, spaceIdx);
                    }
                }
                
                File exeFile = new File(commandPath);
                if (exeFile.exists()) {
                    String suffix = " - " + name;
                    if (name.equalsIgnoreCase("Firefox")) {
                        suffix = " - Mozilla Firefox";
                    } else if (name.equalsIgnoreCase("Chrome")) {
                        suffix = " - Google Chrome";
                    } else if (name.equalsIgnoreCase("Edge")) {
                        suffix = " - Microsoft Edge";
                    }
                    
                    list.add(new DetectedBrowser(name, suffix, commandPath));
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void addFallbackBrowser(Set<DetectedBrowser> list, String name, String suffix, String path) {
        File file = new File(path);
        if (file.exists()) {
            list.add(new DetectedBrowser(name, suffix, path));
        }
    }

    private static String cleanBrowserName(String friendlyName) {
        String lower = friendlyName.toLowerCase();
        if (lower.contains("chrome")) return "Chrome";
        if (lower.contains("edge")) return "Edge";
        if (lower.contains("firefox")) return "Firefox";
        if (lower.contains("brave")) return "Brave";
        if (lower.contains("opera")) return "Opera";
        if (lower.contains("vivaldi")) return "Vivaldi";
        
        return friendlyName.replace("Browser", "").replace("Stable", "").replace("Launcher", "").trim();
    }
}
