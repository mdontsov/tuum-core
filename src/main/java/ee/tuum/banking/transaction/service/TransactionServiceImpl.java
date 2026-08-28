package ee.tuum.banking.transaction.service;

import ee.tuum.banking.account.mapper.AccountMapper;
import ee.tuum.banking.balance.mapper.BalanceMapper;
import ee.tuum.banking.exception.ApiException;
import ee.tuum.banking.domain.AccountTransaction;
import ee.tuum.banking.domain.Currency;
import ee.tuum.banking.domain.TransactionDirection;
import ee.tuum.banking.outbox.service.OutboxService;
import ee.tuum.banking.transaction.mapper.TransactionMapper;
import ee.tuum.banking.transaction.dto.CreateTransactionRequest;
import ee.tuum.banking.transaction.dto.CreatedTransactionResponse;
import ee.tuum.banking.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static ee.tuum.banking.domain.EventType.BALANCE_UPDATED;
import static ee.tuum.banking.domain.EventType.TRANSACTION_CREATED;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final AccountMapper accountMapper;
    private final BalanceMapper balanceMapper;
    private final TransactionMapper transactionMapper;
    private final OutboxService outboxService;
    private final Clock clock;

    @Transactional
    @Override
    public CreatedTransactionResponse create(CreateTransactionRequest request) {
        validateAmount(request.amount());
        Currency currency = parseCurrency(request.currency());
        TransactionDirection direction = parseDirection(request.direction());
        String description = request.description().trim();
        if (description.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DESCRIPTION_MISSING", "Description must not be blank");
        }

        if (!accountMapper.existsById(request.accountId())) {
            throw accountNotFound(request.accountId());
        }
        var balance = balanceMapper.findForUpdate(request.accountId(), currency)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "BALANCE_NOT_FOUND",
                        "Account does not have a " + currency + " balance"));

        BigDecimal balanceAfter = direction == TransactionDirection.IN
                ? balance.availableAmount().add(request.amount())
                : balance.availableAmount().subtract(request.amount());
        if (balanceAfter.signum() < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS",
                    "Insufficient funds in the " + currency + " balance");
        }

        var now = clock.instant();
        balanceMapper.updateAmount(request.accountId(), currency, balanceAfter, now);
        var transaction = new AccountTransaction(UUID.randomUUID(), request.accountId(), request.amount(),
                currency, direction, description, balanceAfter, now);
        transactionMapper.insert(transaction);

        outboxService.add("BALANCE", request.accountId(), BALANCE_UPDATED,
                new BalanceUpdatedEvent(request.accountId(), currency, balanceAfter, balance.version() + 1));
        outboxService.add("TRANSACTION", transaction.id(), TRANSACTION_CREATED,
                new TransactionCreatedEvent(transaction.id(), transaction.accountId(), transaction.amount(),
                        transaction.currency(), transaction.direction(), transaction.description(), balanceAfter));
        return toCreatedResponse(transaction);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TransactionResponse> getByAccount(UUID accountId) {
        if (!accountMapper.existsById(accountId)) {
            throw accountNotFound(accountId);
        }
        return transactionMapper.findByAccountId(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero");
        }
        if (amount.scale() > 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount may have at most four decimal places");
        }
    }

    private Currency parseCurrency(String value) {
        try {
            return Currency.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENCY", "Unsupported currency: " + value);
        }
    }

    private TransactionDirection parseDirection(String value) {
        try {
            return TransactionDirection.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DIRECTION", "Unsupported direction: " + value);
        }
    }

    private ApiException accountNotFound(UUID accountId) {
        return new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account " + accountId + " was not found");
    }

    private CreatedTransactionResponse toCreatedResponse(AccountTransaction transaction) {
        return new CreatedTransactionResponse(transaction.accountId(), transaction.id(), transaction.amount(),
                transaction.currency(), transaction.direction(), transaction.description(), transaction.balanceAfter());
    }

    private TransactionResponse toResponse(AccountTransaction transaction) {
        return new TransactionResponse(transaction.accountId(), transaction.id(), transaction.amount(),
                transaction.currency(), transaction.direction(), transaction.description());
    }

    record BalanceUpdatedEvent(UUID accountId, Currency currency, BigDecimal availableAmount, long version) {
    }

    record TransactionCreatedEvent(UUID transactionId, UUID accountId, BigDecimal amount, Currency currency,
                                   TransactionDirection direction, String description, BigDecimal balanceAfter) {
    }
}
