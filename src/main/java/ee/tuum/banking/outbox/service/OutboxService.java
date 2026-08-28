package ee.tuum.banking.outbox.service;

import ee.tuum.banking.domain.EventType;

import java.util.UUID;

public interface OutboxService {
    void add(String aggregateType, UUID aggregateId, EventType eventType, Object payload);
}
