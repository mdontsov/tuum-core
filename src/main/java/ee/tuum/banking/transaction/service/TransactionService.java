package ee.tuum.banking.transaction.service;

import ee.tuum.banking.transaction.dto.CreateTransactionRequest;
import ee.tuum.banking.transaction.dto.CreatedTransactionResponse;
import ee.tuum.banking.transaction.dto.TransactionResponse;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    CreatedTransactionResponse create(CreateTransactionRequest request);

    List<TransactionResponse> getByAccount(UUID accountId);
}
