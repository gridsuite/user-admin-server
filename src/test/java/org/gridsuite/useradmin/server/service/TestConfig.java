package org.gridsuite.useradmin.server.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@TestConfiguration
public class TestConfig {
    public static final Clock FIXED_CLOCK = Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Primary
    @Bean
    public Clock clock() {
        return FIXED_CLOCK;
    }
}
