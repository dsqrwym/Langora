package org.dsqrwym.langora.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "langora.bootstrap.super-admin")
public record SuperAdminBootstrapProperties (
    boolean enabled,
     String email,
     String username,
     String password,
     boolean overrideExistingPassword
){}
