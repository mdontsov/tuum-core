package ee.tuum.banking.outbox.mapper;

import ee.tuum.banking.outbox.dto.OutboxEvent;
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
import org.mybatis.dynamic.sql.util.mybatis3.CommonCountMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.aggregateId;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.aggregateType;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.attempts;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.eventType;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.id;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.occurredAt;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.outboxEvents;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.payload;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.payloadText;
import static ee.tuum.banking.outbox.OutboxDynamicSqlSupport.publishedAt;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isNull;

@Mapper
public interface OutboxMapper extends CommonCountMapper {
    BasicColumn[] SELECT_LIST = BasicColumn.columnList(id, aggregateType, aggregateId, eventType,
            payloadText, occurredAt, publishedAt, attempts);

    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int insertStatement(InsertStatementProvider<OutboxEvent> insertStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    List<OutboxEvent> selectMany(SelectStatementProvider selectStatement);

    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int update(UpdateStatementProvider updateStatement);

    default void insert(OutboxEvent event) {
        var insertStatement = SqlBuilder.insert(event)
                .into(outboxEvents)
                .map(id).toProperty("id")
                .map(aggregateType).toProperty("aggregateType")
                .map(aggregateId).toProperty("aggregateId")
                .map(eventType).toProperty("eventType")
                .map(payload).toConstant("CAST(#{row.payload} AS JSONB)")
                .map(occurredAt).toProperty("occurredAt")
                .map(publishedAt).toProperty("publishedAt")
                .map(attempts).toProperty("attempts")
                .build()
                .render(RenderingStrategies.MYBATIS3);
        insertStatement(insertStatement);
    }

    default List<OutboxEvent> lockUnpublished(int limit) {
        var selectStatement = SqlBuilder.select(SELECT_LIST)
                .from(outboxEvents)
                .where(publishedAt, isNull())
                .orderBy(occurredAt, id)
                .limit(limit)
                .forUpdate()
                .skipLocked()
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return selectMany(selectStatement);
    }

    default void markPublished(UUID eventId, Instant publishedAtValue) {
        var updateStatement = SqlBuilder.update(outboxEvents)
                .set(publishedAt).equalTo(publishedAtValue)
                .set(attempts).equalToConstant("attempts + 1")
                .where(id, isEqualTo(eventId))
                .build()
                .render(RenderingStrategies.MYBATIS3);
        update(updateStatement);
    }

    default long countUnpublished() {
        var countStatement = SqlBuilder.select(SqlBuilder.count())
                .from(outboxEvents)
                .where(publishedAt, isNull())
                .build()
                .render(RenderingStrategies.MYBATIS3);
        return count(countStatement);
    }
}
