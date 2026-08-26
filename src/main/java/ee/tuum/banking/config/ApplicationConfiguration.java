package ee.tuum.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
