package ee.tuum.banking.account;

import ee.tuum.banking.account.api.AccountResponse;
import ee.tuum.banking.account.api.BalanceResponse;
import ee.tuum.banking.account.api.CreateAccountRequest;
import ee.tuum.banking.common.ApiException;
import ee.tuum.banking.domain.Account;
import ee.tuum.banking.domain.Balance;
import ee.tuum.banking.domain.Currency;
import ee.tuum.banking.messaging.OutboxService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountMapper accountMapper;
    private final BalanceMapper balanceMapper;
    private final OutboxService outboxService;
    private final Clock clock;

    public AccountService(AccountMapper accountMapper, BalanceMapper balanceMapper,
                          OutboxService outboxService, Clock clock) {
        this.accountMapper = accountMapper;
        this.balanceMapper = balanceMapper;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        var currencies = parseCurrencies(request.currencies());
        Instant now = clock.instant();
        UUID accountId = UUID.randomUUID();
        var account = new Account(accountId, request.customerId().trim(),
                request.country().toUpperCase(Locale.ROOT), now, now);
        accountMapper.insert(account);

        var balances = currencies.stream()
                .map(currency -> new Balance(accountId, currency, BigDecimal.ZERO, 0, now, now))
                .toList();
        balances.forEach(balanceMapper::insert);

        outboxService.add("ACCOUNT", accountId, "ACCOUNT_CREATED",
                new AccountCreatedEvent(accountId, account.customerId(), account.country(), currencies));
        balances.forEach(balance -> outboxService.add("BALANCE", accountId, "BALANCE_CREATED",
                new BalanceChangedEvent(accountId, balance.currency(), balance.availableAmount(), balance.version())));
        return toResponse(account, balances);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID accountId) {
        var account = accountMapper.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account " + accountId + " was not found"));
        return toResponse(account, balanceMapper.findByAccountId(accountId));
    }

    private List<Currency> parseCurrencies(List<String> values) {
        var currencies = new LinkedHashSet<Currency>();
        for (String value : values) {
            try {
                currencies.add(Currency.valueOf(value));
            } catch (IllegalArgumentException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENCY",
                        "Unsupported currency: " + value);
            }
        }
        if (currencies.size() != values.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_CURRENCY",
                    "Currencies must not contain duplicates");
        }
        return List.copyOf(currencies);
    }

    private AccountResponse toResponse(Account account, List<Balance> balances) {
        return new AccountResponse(account.id(), account.customerId(), balances.stream()
                .map(balance -> new BalanceResponse(balance.availableAmount(), balance.currency()))
                .toList());
    }

    record AccountCreatedEvent(UUID accountId, String customerId, String country, List<Currency> currencies) {}
    record BalanceChangedEvent(UUID accountId, Currency currency, BigDecimal availableAmount, long version) {}
}
