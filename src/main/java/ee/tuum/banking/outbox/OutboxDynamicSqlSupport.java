package ee.tuum.banking.outbox;

import ee.tuum.banking.domain.EventType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.DerivedColumn;
import org.mybatis.dynamic.sql.SqlColumn;

import java.sql.JDBCType;
import java.time.Instant;
import java.util.UUID;

public final class OutboxDynamicSqlSupport {
    public static final OutboxEvents outboxEvents = new OutboxEvents();
    public static final SqlColumn<UUID> id = outboxEvents.id;
    public static final SqlColumn<String> aggregateType = outboxEvents.aggregateType;
    public static final SqlColumn<UUID> aggregateId = outboxEvents.aggregateId;
    public static final SqlColumn<EventType> eventType = outboxEvents.eventType;
    public static final SqlColumn<String> payload = outboxEvents.payload;
    public static final BasicColumn payloadText = DerivedColumn.<String>of("payload::text").as("payload");
    public static final SqlColumn<Instant> occurredAt = outboxEvents.occurredAt;
    public static final SqlColumn<Instant> publishedAt = outboxEvents.publishedAt;
    public static final SqlColumn<Integer> attempts = outboxEvents.attempts;

    private OutboxDynamicSqlSupport() {
    }

    public static final class OutboxEvents extends AliasableSqlTable<OutboxEvents> {
        public final SqlColumn<UUID> id = column("id", JDBCType.OTHER);
        public final SqlColumn<String> aggregateType = column("aggregate_type", JDBCType.VARCHAR);
        public final SqlColumn<UUID> aggregateId = column("aggregate_id", JDBCType.OTHER);
        public final SqlColumn<EventType> eventType = column("event_type", JDBCType.VARCHAR);
        public final SqlColumn<String> payload = column("payload", JDBCType.OTHER);
        public final SqlColumn<Instant> occurredAt = column("occurred_at", JDBCType.TIMESTAMP_WITH_TIMEZONE);
        public final SqlColumn<Instant> publishedAt = column("published_at", JDBCType.TIMESTAMP_WITH_TIMEZONE);
        public final SqlColumn<Integer> attempts = column("attempts", JDBCType.INTEGER);

        public OutboxEvents() {
            super("outbox_events", OutboxEvents::new);
        }
    }
}
