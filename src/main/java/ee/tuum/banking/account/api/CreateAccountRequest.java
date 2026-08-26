package ee.tuum.banking.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String customerId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "must be a two-letter country code") String country,
        @NotEmpty List<@NotBlank String> currencies) {
}
