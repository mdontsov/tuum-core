package ee.tuum.banking.domain;

import java.time.Instant;
import java.util.UUID;

public record Account(UUID id, String customerId, String country, Instant createdAt, Instant updatedAt) {
}
