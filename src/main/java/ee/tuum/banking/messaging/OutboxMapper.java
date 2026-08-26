package ee.tuum.banking.messaging;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OutboxMapper {
    void insert(OutboxEvent event);
    List<OutboxEvent> lockUnpublished(@Param("limit") int limit);
    void markPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);
    long countUnpublished();
}
