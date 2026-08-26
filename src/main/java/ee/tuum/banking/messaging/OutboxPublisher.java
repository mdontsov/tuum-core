package ee.tuum.banking.messaging;

import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;

@Component
public class OutboxPublisher {
    private final OutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final int batchSize;

    public OutboxPublisher(OutboxMapper outboxMapper, RabbitTemplate rabbitTemplate, Clock clock,
                           @Value("${banking.outbox.batch-size:100}") int batchSize) {
        this.outboxMapper = outboxMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${banking.outbox.fixed-delay-ms:500}",
            initialDelayString = "${banking.outbox.fixed-delay-ms:500}")
    @Transactional
    public void publishBatch() {
        for (OutboxEvent event : outboxMapper.lockUnpublished(batchSize)) {
            publish(event);
            outboxMapper.markPublished(event.id(), clock.instant());
        }
    }

    private void publish(OutboxEvent event) {
        var message = MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(event.id().toString())
                .setHeader("eventId", event.id().toString())
                .setHeader("eventType", event.eventType())
                .setHeader("aggregateType", event.aggregateType())
                .setHeader("aggregateId", event.aggregateId().toString())
                .setTimestamp(java.util.Date.from(event.occurredAt()))
                .build();
        rabbitTemplate.invoke(operations -> {
            operations.send(RabbitConfiguration.ACCOUNT_EVENTS_EXCHANGE,
                    event.eventType().toLowerCase(Locale.ROOT).replace('_', '.'), message);
            operations.waitForConfirmsOrDie(5_000);
            return true;
        });
    }
}
