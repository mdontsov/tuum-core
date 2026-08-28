package ee.tuum.banking.transaction;

import ee.tuum.banking.domain.Currency;
import ee.tuum.banking.domain.TransactionDirection;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.time.Instant;
import java.util.UUID;

public final class TransactionDynamicSqlSupport {
    public static final AccountTransactions accountTransactions = new AccountTransactions();
    public static final SqlColumn<UUID> id = accountTransactions.id;
    public static final SqlColumn<UUID> accountId = accountTransactions.accountId;
    public static final SqlColumn<BigDecimal> amount = accountTransactions.amount;
    public static final SqlColumn<Currency> currency = accountTransactions.currency;
    public static final SqlColumn<TransactionDirection> direction = accountTransactions.direction;
    public static final SqlColumn<String> description = accountTransactions.description;
    public static final SqlColumn<BigDecimal> balanceAfter = accountTransactions.balanceAfter;
    public static final SqlColumn<Instant> createdAt = accountTransactions.createdAt;

    private TransactionDynamicSqlSupport() {
    }

    public static final class AccountTransactions extends AliasableSqlTable<AccountTransactions> {
        public final SqlColumn<UUID> id = column("id", JDBCType.OTHER);
        public final SqlColumn<UUID> accountId = column("account_id", JDBCType.OTHER);
        public final SqlColumn<BigDecimal> amount = column("amount", JDBCType.NUMERIC);
        public final SqlColumn<Currency> currency = column("currency", JDBCType.VARCHAR);
        public final SqlColumn<TransactionDirection> direction = column("direction", JDBCType.VARCHAR);
        public final SqlColumn<String> description = column("description", JDBCType.VARCHAR);
        public final SqlColumn<BigDecimal> balanceAfter = column("balance_after", JDBCType.NUMERIC);
        public final SqlColumn<Instant> createdAt = column("created_at", JDBCType.TIMESTAMP_WITH_TIMEZONE);

        public AccountTransactions() {
            super("account_transactions", AccountTransactions::new);
        }
    }
}
