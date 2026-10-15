package org.dsqrwym.langora.infrastructure.util;

import java.util.Locale;

public final class DeviceNameResolver {
    private static final int MAX_DEVICE_NAME_LENGTH = 150;

    private DeviceNameResolver() {
        throw new UnsupportedOperationException("DeviceNameResolver is a utility class and cannot be instantiated");
    }

    /**
     * Resolves the device name from the user agent.
     *
     * @param userAgent user agent obtained from the request
     * @return resolved device name or null if userAgent is null or blank
     */
    public static String resolveFromUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }

        userAgent = userAgent.toLowerCase(Locale.ROOT);

        String platform = getPlatform(userAgent);
        String browser = getBrowser(userAgent);
        String result = platform + " . " + browser;

        return TextUtils.safeSubstring(result, MAX_DEVICE_NAME_LENGTH);
    }

    private static String getPlatform(String userAgent) {
        if (userAgent.contains("android")) {
            return "Android";
        }

        if (userAgent.contains("ios")) {
            return "iOS";
        }

        if (userAgent.contains("ipad")) {
            return "iPad";
        }

        if (userAgent.contains("windows")) {
            return "Windows";
        }

        if (userAgent.contains("mac") || userAgent.contains("darwin") || userAgent.contains("mac os") || userAgent.contains("macintosh")) {
            return "MacOS";
        }

        if (userAgent.contains("linux")) {
            return "Linux";
        }

        return "Unknown device";
    }

    private static String getBrowser(String userAgent) {
        if (userAgent.contains("edg/")) {
            return "Edge";
        }
        if (userAgent.contains("chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("crios/")) {
            return "Chrome iOS";
        }
        if (userAgent.contains("firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("safari/")) {
            return "Safari";
        }
        if (userAgent.contains("opera/") || userAgent.contains("opr/")) {
            return "Opera";
        }
        return "Browser";
    }
}
