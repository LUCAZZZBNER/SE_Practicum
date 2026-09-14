package com.delivery.order.events;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderEventOutboxDao {
  int insert(OrderEventOutbox event);
  int claimReady(@Param("worker") String worker, @Param("limit") int limit);
  List<OrderEventOutbox> findClaimed(@Param("worker") String worker);
  int markAttempt(@Param("id") long id, @Param("worker") String worker);
  int markPublished(@Param("id") long id, @Param("worker") String worker);
}
