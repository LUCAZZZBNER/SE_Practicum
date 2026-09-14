package com.delivery.order.events;
import java.time.Instant;
public class StockReleaseOutbox {
  private long id; private String reservationId; private String payload; private int attempts; private Instant availableAt; private String claimedBy; private Instant claimUntil;
  public long getId(){return id;} public void setId(long v){id=v;}
  public String getReservationId(){return reservationId;} public void setReservationId(String v){reservationId=v;}
  public String getPayload(){return payload;} public void setPayload(String v){payload=v;}
  public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;}
  public Instant getAvailableAt(){return availableAt;} public void setAvailableAt(Instant v){availableAt=v;}
  public String getClaimedBy(){return claimedBy;} public void setClaimedBy(String v){claimedBy=v;}
  public Instant getClaimUntil(){return claimUntil;} public void setClaimUntil(Instant v){claimUntil=v;}
}
