package ee.tuum.banking.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class OutboxService {
    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxService(OutboxMapper outboxMapper, ObjectMapper objectMapper, Clock clock) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void add(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        try {
            outboxMapper.insert(new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId, eventType,
                    objectMapper.writeValueAsString(payload), clock.instant(), null, 0));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
    }
}
