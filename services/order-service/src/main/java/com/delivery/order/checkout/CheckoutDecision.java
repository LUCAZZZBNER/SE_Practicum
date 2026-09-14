package com.delivery.order.checkout;

/** Local commit decision shared with checkout participants. */
public class CheckoutDecision {
  private String id;
  private String fingerprint;
  private String state;
  private String error;
  public String getId() { return id; }
  public void setId(String value) { id = value; }
  public String getFingerprint() { return fingerprint; }
  public void setFingerprint(String value) { fingerprint = value; }
  public String getState() { return state; }
  public void setState(String value) { state = value; }
  public String getError() { return error; }
  public void setError(String value) { error = value; }
}
