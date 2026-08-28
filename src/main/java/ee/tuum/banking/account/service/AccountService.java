package ee.tuum.banking.account.service;

import ee.tuum.banking.account.dto.AccountRequest;
import ee.tuum.banking.account.dto.AccountResponse;

import java.util.UUID;

public interface AccountService {

    AccountResponse create(AccountRequest request);

    AccountResponse get(UUID accountId);
}
