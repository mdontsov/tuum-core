package ee.tuum.banking.transaction.dto;

import ee.tuum.banking.domain.Currency;
import ee.tuum.banking.domain.TransactionDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(UUID accountId, UUID transactionId, BigDecimal amount,
                                  Currency currency, TransactionDirection direction, String description) {
}
