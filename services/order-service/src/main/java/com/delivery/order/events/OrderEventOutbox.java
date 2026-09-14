package com.delivery.order.events;

import java.time.Instant;

/** Durable event awaiting publication to Kafka. */
public class OrderEventOutbox {
  private long id;
  private String eventId;
  private long orderId;
  private long userId;
  private String reservationId;
  private String payload;
  private int attempts;
  private Instant availableAt;
  private String claimedBy;
  private Instant claimUntil;

  public long getId() { return id; }
  public void setId(long id) { this.id = id; }
  public String getEventId() { return eventId; }
  public void setEventId(String eventId) { this.eventId = eventId; }
  public long getOrderId() { return orderId; }
  public void setOrderId(long orderId) { this.orderId = orderId; }
  public long getUserId() { return userId; }
  public void setUserId(long userId) { this.userId = userId; }
  public String getReservationId() { return reservationId; }
  public void setReservationId(String reservationId) { this.reservationId = reservationId; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public int getAttempts() { return attempts; }
  public void setAttempts(int attempts) { this.attempts = attempts; }
  public Instant getAvailableAt() { return availableAt; }
  public void setAvailableAt(Instant availableAt) { this.availableAt = availableAt; }
  public String getClaimedBy() { return claimedBy; }
  public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }
  public Instant getClaimUntil() { return claimUntil; }
  public void setClaimUntil(Instant claimUntil) { this.claimUntil = claimUntil; }
}
