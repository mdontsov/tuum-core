package ee.tuum.banking.balance.dto;

import ee.tuum.banking.domain.Currency;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal availableAmount, Currency currency) {
}
