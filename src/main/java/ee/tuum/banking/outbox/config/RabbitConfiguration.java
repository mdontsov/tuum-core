package ee.tuum.banking.outbox.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    public static final String ACCOUNT_EVENTS_EXCHANGE = "account.events";

    @Bean
    TopicExchange accountEventsExchange() {
        return new TopicExchange(ACCOUNT_EVENTS_EXCHANGE, true, false);
    }
}
