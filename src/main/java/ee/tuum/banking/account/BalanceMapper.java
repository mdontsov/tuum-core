package ee.tuum.banking.account;

import ee.tuum.banking.domain.Balance;
import ee.tuum.banking.domain.Currency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface BalanceMapper {
    void insert(Balance balance);
    List<Balance> findByAccountId(@Param("accountId") UUID accountId);
    Optional<Balance> findForUpdate(@Param("accountId") UUID accountId,
                                    @Param("currency") Currency currency);
    int updateAmount(@Param("accountId") UUID accountId,
                     @Param("currency") Currency currency,
                     @Param("amount") BigDecimal amount,
                     @Param("updatedAt") Instant updatedAt);
}
