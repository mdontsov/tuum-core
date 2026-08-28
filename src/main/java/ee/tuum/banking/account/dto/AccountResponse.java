package ee.tuum.banking.account.dto;

import ee.tuum.banking.balance.dto.BalanceResponse;

import java.util.List;
import java.util.UUID;

public record AccountResponse(UUID accountId, String customerId, List<BalanceResponse> balances) {
}
