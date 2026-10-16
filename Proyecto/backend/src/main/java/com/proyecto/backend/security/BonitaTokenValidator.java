package com.proyecto.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class BonitaTokenValidator {

    private final String expectedToken;

    public BonitaTokenValidator(
            @Value("${bonita.offers.api-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public boolean isValid(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }

        String receivedToken = authorizationHeader.substring("Bearer ".length()).trim();

        if (receivedToken.isEmpty()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                receivedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}