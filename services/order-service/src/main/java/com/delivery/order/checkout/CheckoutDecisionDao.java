package com.delivery.order.checkout;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CheckoutDecisionDao {
  @Insert("INSERT IGNORE INTO checkout_decisions(id,user_id,idempotency_key,fingerprint) VALUES(#{id},#{userId},#{key},#{fingerprint})")
  int begin(@Param("id") String id, @Param("userId") long userId, @Param("key") String key, @Param("fingerprint") String fingerprint);
  @Select("SELECT * FROM checkout_decisions WHERE id=#{id} FOR UPDATE")
  CheckoutDecision lock(String id);
  @Select("SELECT * FROM checkout_decisions WHERE id=#{id}")
  CheckoutDecision find(String id);
  @Update("UPDATE checkout_decisions SET state='COMMITTED' WHERE id=#{id} AND state='OPEN'")
  int commit(String id);
  @Update("UPDATE checkout_decisions SET state='ABORTED',error=#{error} WHERE id=#{id} AND state='OPEN'")
  int abort(@Param("id") String id, @Param("error") String error);
  @Select("SELECT * FROM checkout_decisions WHERE state='OPEN' AND created_at < CURRENT_TIMESTAMP(3) - INTERVAL 2 MINUTE LIMIT 100 FOR UPDATE SKIP LOCKED")
  List<CheckoutDecision> abandoned();
}
