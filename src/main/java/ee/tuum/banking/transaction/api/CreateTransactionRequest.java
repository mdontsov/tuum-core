package ee.tuum.banking.transaction.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull UUID accountId,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String direction,
        @NotBlank @Size(max = 500) String description) {
}
