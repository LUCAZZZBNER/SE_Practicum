package com.delivery.cart.common;

/** Result of a successful logical deletion. */
public record DeleteResult(long id, boolean deleted) {}
