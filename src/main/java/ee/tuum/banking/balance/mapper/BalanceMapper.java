package ee.tuum.banking.balance.mapper;

import ee.tuum.banking.domain.Balance;
import ee.tuum.banking.domain.Currency;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ee.tuum.banking.balance.BalanceDynamicSqlSupport.accountId;
import static ee.tuum.banking.balance.BalanceDynamicSqlSupport.availableAmount;
import static ee.tuum.banking.balance.BalanceDynamicSqlSupport.balances;
import static ee.tuum.banking.balance.BalanceDynamicSqlSupport.createdAt;
import static ee.tuum.banking.balance.BalanceDynamicSqlSupport.currency;
import static ee.tuum.banking.balance.BalanceDynamicSqlSupport.updatedAt;
import static ee.tuum.banking.balance.BalanceDynamicSqlSupport.version;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Mapper
public interface BalanceMapper {
    BasicColumn[] SELECT_LIST = BasicColumn.columnList(accountId, currency, availableAmount, version, createdAt, updatedAt);

    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int insertStatement(InsertStatementProvider<Balance> insertStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    List<Balance> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    Optional<Balance> selectOne(SelectStatementProvider selectStatement);

    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int update(UpdateStatementProvider updateStatement);

    default void insert(Balance balance) {
        var insertStatement = SqlBuilder.insert(balance)
                .into(balances)
                .map(accountId).toProperty("accountId")
                .map(currency).toProperty("currency")
                .map(availableAmount).toProperty("availableAmount")
                .map(version).toProperty("version")
                .map(createdAt).toProperty("createdAt")
                .map(updatedAt).toProperty("updatedAt")
                .build()
                .render(RenderingStrategies.MYBATIS3);
        insertStatement(insertStatement);
    }

    default List<Balance> findByAccountId(UUID accountIdValue) {
        var selectStatement = SqlBuilder.select(SELECT_LIST)
                .from(balances)
                .where(accountId, isEqualTo(accountIdValue))
                .orderBy(currency)
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return selectMany(selectStatement);
    }

    default Optional<Balance> findForUpdate(UUID accountIdValue, Currency currencyValue) {
        var selectStatement = SqlBuilder.select(SELECT_LIST)
                .from(balances)
                .where(accountId, isEqualTo(accountIdValue))
                .and(currency, isEqualTo(currencyValue))
                .forUpdate()
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return selectOne(selectStatement);
    }

    default int updateAmount(UUID accountIdValue, Currency currencyValue, BigDecimal amountValue, Instant updatedAtValue) {
        var updateStatement = SqlBuilder.update(balances)
                .set(availableAmount).equalTo(amountValue)
                .set(version).equalToConstant("version + 1")
                .set(updatedAt).equalTo(updatedAtValue)
                .where(accountId, isEqualTo(accountIdValue))
                .and(currency, isEqualTo(currencyValue))
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return update(updateStatement);
    }
}
