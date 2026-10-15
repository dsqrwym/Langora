package org.dsqrwym.langora.infrastructure.util;

import jakarta.validation.constraints.NotNull;

public final class TextUtils {
    private TextUtils() {
        throw new UnsupportedOperationException("TextUtils is a utility class and cannot be instantiated");
    }

    /**
     * Removing all line breaks and trailing/leading whitespace.
     *
     * @param text the string to sanitizeText
     * @return the sanitized string
     */
    public static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        return text.trim().replaceAll("[\\r\\n]+", "");
    }

    /**
     * Check the input string and return the default value if it is null.
     *
     * @param input        the input string
     * @param defaultValue the default value
     * @return the input string if it is not null, otherwise the default value
     */
    public @NotNull
    static String defaultIfNull(String input, String defaultValue) {
        return input != null ? input : defaultValue;
    }

    /**
     * Check the input string and return the default value if it is blank.
     *
     * @param input        the input string
     * @param defaultValue the default value
     * @return the input string if it is not blank, otherwise the default value
     */
    public static String defaultIfBlank(String input, String defaultValue) {
        return input != null && !input.isBlank() ? input : defaultValue;
    }

    /**
     * Safely substrings a string.
     * If the string length is less than or equal to the maximum length, it returns the original string;
     * otherwise, it safely truncates it to the specified maximum length.
     *
     * @param str       the string to substring
     * @param maxLength the maximum allowed length
     * @return the safe substring, or null if the input is null
     */
    public static String safeSubstring(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}
