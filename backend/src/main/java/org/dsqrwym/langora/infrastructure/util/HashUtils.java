package org.dsqrwym.langora.infrastructure.util;

import org.dsqrwym.langora.infrastructure.web.exception.InternalServerException;
import org.jspecify.annotations.NonNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for SHA-256 hashing and HMAC-SHA256 signing.
 */
public final class HashUtils {
    private HashUtils() {
    }

    /**
     * Hash a string with SHA-256 and return lowercase hexadecimal representation.
     *
     * @return String
     */
    public static @NonNull String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerException();
        }
    }

    /**
     * Computes HMAC-SHA256 using the secret as UTF-8 bytes.
     * Returns the result as a lowercase hex string.
     */
    public static String hmacSha256Hex(String secret, String message) {
        return hmacSha256Hex(
                secret.getBytes(StandardCharsets.UTF_8),
                message
        );
    }

    /**
     * Computes HMAC-SHA256 with the given raw key bytes.
     * Returns the result as a lowercase hex string.
     */
    public static @NonNull String hmacSha256Hex(byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] result = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new InternalServerException();
        }
    }
}
