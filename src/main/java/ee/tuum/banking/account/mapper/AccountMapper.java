package ee.tuum.banking.account.mapper;

import ee.tuum.banking.domain.Account;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.CommonCountMapper;

import java.util.Optional;
import java.util.UUID;

import static ee.tuum.banking.account.AccountDynamicSqlSupport.accounts;
import static ee.tuum.banking.account.AccountDynamicSqlSupport.country;
import static ee.tuum.banking.account.AccountDynamicSqlSupport.createdAt;
import static ee.tuum.banking.account.AccountDynamicSqlSupport.customerId;
import static ee.tuum.banking.account.AccountDynamicSqlSupport.id;
import static ee.tuum.banking.account.AccountDynamicSqlSupport.updatedAt;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Mapper
public interface AccountMapper extends CommonCountMapper {
    BasicColumn[] SELECT_LIST = BasicColumn.columnList(id, customerId, country, createdAt, updatedAt);

    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int insertStatement(InsertStatementProvider<Account> insertStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    Optional<Account> selectOne(SelectStatementProvider selectStatement);

    default void insert(Account account) {
        var insertStatement = SqlBuilder.insert(account)
                .into(accounts)
                .map(id).toProperty("id")
                .map(customerId).toProperty("customerId")
                .map(country).toProperty("country")
                .map(createdAt).toProperty("createdAt")
                .map(updatedAt).toProperty("updatedAt")
                .build()
                .render(RenderingStrategies.MYBATIS3);
        insertStatement(insertStatement);
    }

    default Optional<Account> findById(UUID accountId) {
        var selectStatement = SqlBuilder.select(SELECT_LIST)
                .from(accounts)
                .where(id, isEqualTo(accountId))
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return selectOne(selectStatement);
    }

    default boolean existsById(UUID accountId) {
        var countStatement = SqlBuilder.select(SqlBuilder.count())
                .from(accounts)
                .where(id, isEqualTo(accountId))
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return count(countStatement) > 0;
    }
}
