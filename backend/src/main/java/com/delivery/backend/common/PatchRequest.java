package com.delivery.backend.common;

/** Tracks whether a PATCH body supplied at least one recognized field, including an explicit null. */
public abstract class PatchRequest {

	private boolean updateSpecified;

	protected final void markUpdateSpecified() {
		updateSpecified = true;
	}

	public boolean isUpdateSpecified() {
		return updateSpecified;
	}
}
