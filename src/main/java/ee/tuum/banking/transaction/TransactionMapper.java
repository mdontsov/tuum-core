package ee.tuum.banking.transaction;

import ee.tuum.banking.domain.AccountTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TransactionMapper {
    void insert(AccountTransaction transaction);
    List<AccountTransaction> findByAccountId(@Param("accountId") UUID accountId);
}
