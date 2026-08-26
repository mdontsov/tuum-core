package ee.tuum.banking.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Balance(UUID accountId, Currency currency, BigDecimal availableAmount,
                      long version, Instant createdAt, Instant updatedAt) {
}
