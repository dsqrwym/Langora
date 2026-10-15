package org.dsqrwym.langora.infrastructure.bootstrap;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@Order(1)
public class BannerCheckerRunner implements ApplicationRunner {
    private static final String BANNER_PATH = "/banner.txt";
    private static final int LAST_LINE = 8;
    private static final String EXPECTED_SHA256 = "f940e6def3a69670247ec5672577c88736fdf32a5a75f425b11fe327665a151a";

    @Override
    public void run(@NonNull ApplicationArguments args) {
        checkBanner();
    }

    public static void checkBanner() {
        try (InputStream inputStream = BannerCheckerRunner.class.getResourceAsStream(BANNER_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException();
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                for (int i = 0; i < LAST_LINE; i++) {
                    String line = reader.readLine();
                    if (line == null) {
                        throw new IllegalStateException();
                    }
                    digest.update(line.getBytes(StandardCharsets.UTF_8));
                    if (i < LAST_LINE - 1) {
                        digest.update((byte) '\n');
                    }
                }

                byte[] hashBytes = digest.digest();

                if (!EXPECTED_SHA256.contentEquals(HexFormat.of().formatHex(hashBytes))) {
                    throw new IllegalStateException();
                }
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
