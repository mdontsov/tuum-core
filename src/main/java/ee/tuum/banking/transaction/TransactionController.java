package ee.tuum.banking.transaction;

import ee.tuum.banking.transaction.api.CreateTransactionRequest;
import ee.tuum.banking.transaction.api.CreatedTransactionResponse;
import ee.tuum.banking.transaction.api.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedTransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.create(request);
    }

    @GetMapping
    public List<TransactionResponse> getByAccount(@RequestParam UUID accountId) {
        return transactionService.getByAccount(accountId);
    }
}
