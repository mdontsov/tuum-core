package ee.tuum.banking.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountTransaction(UUID id, UUID accountId, BigDecimal amount, Currency currency,
                                 TransactionDirection direction, String description,
                                 BigDecimal balanceAfter, Instant createdAt) {
}
