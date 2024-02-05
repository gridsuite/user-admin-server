package org.gridsuite.useradmin.server;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(UserAdminApplicationProps.class)
public class UserAdminConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
