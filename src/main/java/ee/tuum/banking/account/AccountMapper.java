package ee.tuum.banking.account;

import ee.tuum.banking.domain.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface AccountMapper {
    void insert(Account account);
    Optional<Account> findById(@Param("id") UUID id);
    boolean existsById(@Param("id") UUID id);
}
