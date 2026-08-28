package ee.tuum.banking.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.tuum.banking.domain.EventType;
import ee.tuum.banking.outbox.dto.OutboxEvent;
import ee.tuum.banking.outbox.mapper.OutboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void add(String aggregateType, UUID aggregateId, EventType eventType, Object payload) {
        try {
            outboxMapper.insert(new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId, eventType,
                    objectMapper.writeValueAsString(payload), clock.instant(), null, 0));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
    }
}
