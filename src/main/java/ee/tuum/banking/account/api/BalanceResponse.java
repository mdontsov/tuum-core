package ee.tuum.banking.account.api;

import ee.tuum.banking.domain.Currency;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal availableAmount, Currency currency) {
}
