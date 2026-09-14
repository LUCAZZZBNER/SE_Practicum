package com.delivery.order.events;
import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper public interface StockReleaseOutboxDao {
  int insert(StockReleaseOutbox row);
  int claimReady(@Param("worker") String worker, @Param("limit") int limit);
  List<StockReleaseOutbox> findClaimed(@Param("worker") String worker);
  int markAttempt(@Param("id") long id, @Param("worker") String worker);
  int markCompleted(@Param("id") long id, @Param("worker") String worker);
}
