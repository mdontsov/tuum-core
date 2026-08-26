package ee.tuum.banking.messaging;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType,
                          String payload, Instant occurredAt, Instant publishedAt, int attempts) {
}
