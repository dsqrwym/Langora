package org.dsqrwym.langora.infrastructure.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeviceNameResolverTest {

    @Test
    void identifiesWindowsChrome() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36";

        assertEquals("Windows . Chrome", DeviceNameResolver.resolveFromUserAgent(userAgent));
    }

    @Test
    void identifiesMacSafari() {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15";

        assertEquals("MacOS . Safari", DeviceNameResolver.resolveFromUserAgent(userAgent));
    }

    @Test
    void identifiesAndroidChrome() {
        String userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36";

        assertEquals("Android . Chrome", DeviceNameResolver.resolveFromUserAgent(userAgent));
    }

    @Test
    void returnsNullForMissingUserAgent() {
        assertNull(DeviceNameResolver.resolveFromUserAgent(null));
        assertNull(DeviceNameResolver.resolveFromUserAgent(" "));
    }
}
