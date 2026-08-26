package ee.tuum.banking.account.api;

import java.util.List;
import java.util.UUID;

public record AccountResponse(UUID accountId, String customerId, List<BalanceResponse> balances) {
}
