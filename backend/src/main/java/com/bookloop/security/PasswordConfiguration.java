package com.bookloop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Constructor values: salt bytes, hash bytes, parallelism, memory in KiB, iterations.
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }
}
