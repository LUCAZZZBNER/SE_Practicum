package com.delivery.catalog.events;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CatalogProjectionEventDao {
  int insert(CatalogProjectionEvent event);
  int claimReady(@Param("worker") String worker, @Param("limit") int limit);
  List<CatalogProjectionEvent> findClaimed(@Param("worker") String worker);
  int markAttempt(@Param("id") long id, @Param("worker") String worker);
  int markPublished(@Param("id") long id, @Param("worker") String worker);
}
