package ee.tuum.banking.outbox.dto;

import ee.tuum.banking.domain.EventType;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(UUID id, String aggregateType, UUID aggregateId, EventType eventType,
                          String payload, Instant occurredAt, Instant publishedAt, int attempts) {
}
