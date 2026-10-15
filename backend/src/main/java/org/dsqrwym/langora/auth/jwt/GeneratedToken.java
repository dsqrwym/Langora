package org.dsqrwym.langora.auth.jwt;

import java.time.Instant;

public record GeneratedToken(String token, Instant expiresAt) {}
