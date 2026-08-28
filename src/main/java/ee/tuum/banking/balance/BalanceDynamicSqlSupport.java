package ee.tuum.banking.balance;

import ee.tuum.banking.domain.Currency;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.time.Instant;
import java.util.UUID;

public final class BalanceDynamicSqlSupport {
    public static final Balances balances = new Balances();
    public static final SqlColumn<UUID> accountId = balances.accountId;
    public static final SqlColumn<Currency> currency = balances.currency;
    public static final SqlColumn<BigDecimal> availableAmount = balances.availableAmount;
    public static final SqlColumn<Long> version = balances.version;
    public static final SqlColumn<Instant> createdAt = balances.createdAt;
    public static final SqlColumn<Instant> updatedAt = balances.updatedAt;

    private BalanceDynamicSqlSupport() {
    }

    public static final class Balances extends AliasableSqlTable<Balances> {
        public final SqlColumn<UUID> accountId = column("account_id", JDBCType.OTHER);
        public final SqlColumn<Currency> currency = column("currency", JDBCType.VARCHAR);
        public final SqlColumn<BigDecimal> availableAmount = column("available_amount", JDBCType.NUMERIC);
        public final SqlColumn<Long> version = column("version", JDBCType.BIGINT);
        public final SqlColumn<Instant> createdAt = column("created_at", JDBCType.TIMESTAMP_WITH_TIMEZONE);
        public final SqlColumn<Instant> updatedAt = column("updated_at", JDBCType.TIMESTAMP_WITH_TIMEZONE);

        public Balances() {
            super("balances", Balances::new);
        }
    }
}
