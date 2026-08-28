package ee.tuum.banking.transaction.mapper;

import ee.tuum.banking.domain.AccountTransaction;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import java.util.List;
import java.util.UUID;

import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.accountId;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.accountTransactions;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.amount;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.balanceAfter;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.createdAt;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.currency;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.description;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.direction;
import static ee.tuum.banking.transaction.TransactionDynamicSqlSupport.id;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Mapper
public interface TransactionMapper {
    BasicColumn[] SELECT_LIST = BasicColumn.columnList(id, accountId, amount, currency, direction,
            description, balanceAfter, createdAt);

    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int insertStatement(InsertStatementProvider<AccountTransaction> insertStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    List<AccountTransaction> selectMany(SelectStatementProvider selectStatement);

    default void insert(AccountTransaction transaction) {
        var insertStatement = SqlBuilder.insert(transaction)
                .into(accountTransactions)
                .map(id).toProperty("id")
                .map(accountId).toProperty("accountId")
                .map(amount).toProperty("amount")
                .map(currency).toProperty("currency")
                .map(direction).toProperty("direction")
                .map(description).toProperty("description")
                .map(balanceAfter).toProperty("balanceAfter")
                .map(createdAt).toProperty("createdAt")
                .build()
                .render(RenderingStrategies.MYBATIS3);
        insertStatement(insertStatement);
    }

    default List<AccountTransaction> findByAccountId(UUID accountIdValue) {
        var selectStatement = SqlBuilder.select(SELECT_LIST)
                .from(accountTransactions)
                .where(accountId, isEqualTo(accountIdValue))
                .orderBy(createdAt, id)
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return selectMany(selectStatement);
    }
}
