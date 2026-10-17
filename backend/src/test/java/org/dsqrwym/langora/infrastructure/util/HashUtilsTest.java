package org.dsqrwym.langora.infrastructure.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashUtilsTest {

    @Test
    void producesStandardSha256Digest() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                HashUtils.sha256Hex("abc")
        );
    }

    @Test
    void producesStandardHmacSha256Digest() {
        assertEquals(
                "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                HashUtils.hmacSha256Hex("key", "The quick brown fox jumps over the lazy dog")
        );
    }
}
