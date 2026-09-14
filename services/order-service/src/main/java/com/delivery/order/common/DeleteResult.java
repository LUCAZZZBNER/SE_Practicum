package com.delivery.order.common;

/** Result of a successful logical deletion. */
public record DeleteResult(long id, boolean deleted) {}
