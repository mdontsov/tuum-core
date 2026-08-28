package ee.tuum.banking.account;

import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

import java.sql.JDBCType;
import java.time.Instant;
import java.util.UUID;

public final class AccountDynamicSqlSupport {
    public static final Accounts accounts = new Accounts();
    public static final SqlColumn<UUID> id = accounts.id;
    public static final SqlColumn<String> customerId = accounts.customerId;
    public static final SqlColumn<String> country = accounts.country;
    public static final SqlColumn<Instant> createdAt = accounts.createdAt;
    public static final SqlColumn<Instant> updatedAt = accounts.updatedAt;

    private AccountDynamicSqlSupport() {
    }

    public static final class Accounts extends AliasableSqlTable<Accounts> {
        public final SqlColumn<UUID> id = column("id", JDBCType.OTHER);
        public final SqlColumn<String> customerId = column("customer_id", JDBCType.VARCHAR);
        public final SqlColumn<String> country = column("country", JDBCType.VARCHAR);
        public final SqlColumn<Instant> createdAt = column("created_at", JDBCType.TIMESTAMP_WITH_TIMEZONE);
        public final SqlColumn<Instant> updatedAt = column("updated_at", JDBCType.TIMESTAMP_WITH_TIMEZONE);

        public Accounts() {
            super("accounts", Accounts::new);
        }
    }
}
