package com.delivery.catalog.events;

import java.time.Instant;

public class CatalogProjectionEvent {
  private long id;
  private String eventId;
  private long aggregateId;
  private String payload;
  private int attempts;
  private Instant availableAt;
  private Instant claimedUntil;
  private String claimedBy;
  public long getId(){return id;} public void setId(long v){id=v;}
  public String getEventId(){return eventId;} public void setEventId(String v){eventId=v;}
  public long getAggregateId(){return aggregateId;} public void setAggregateId(long v){aggregateId=v;}
  public String getPayload(){return payload;} public void setPayload(String v){payload=v;}
  public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;}
  public Instant getAvailableAt(){return availableAt;} public void setAvailableAt(Instant v){availableAt=v;}
  public Instant getClaimedUntil(){return claimedUntil;} public void setClaimedUntil(Instant v){claimedUntil=v;}
  public String getClaimedBy(){return claimedBy;} public void setClaimedBy(String v){claimedBy=v;}
}
