package com.delivery.backend.address.service;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.common.PatchRequest;

public interface UserAddressService {
	AddressView create(long userId, CreateRequest request);
	List<AddressView> list(long userId);
	AddressView update(long userId, long addressId, UpdateRequest request);
	DeleteResult remove(long userId, long addressId);
	AddressView requireOwned(long userId, long addressId);

	record CreateRequest(@NotBlank String recipient, @NotBlank @Pattern(regexp = "1[0-9]{10}") String phone,
			@NotBlank String region, @NotBlank String detail, Boolean isDefault) {}

	final class UpdateRequest extends PatchRequest {
		private String recipient;
		@Pattern(regexp = "1[0-9]{10}") private String phone;
		private String region;
		private String detail;
		private Boolean isDefault;
		private boolean recipientSpecified;
		private boolean phoneSpecified;
		private boolean regionSpecified;
		private boolean detailSpecified;
		private boolean defaultSpecified;
		public String recipient() { return recipient; }
		public void setRecipient(String value) { recipient = value; recipientSpecified = true; markUpdateSpecified(); }
		public String phone() { return phone; }
		public void setPhone(String value) { phone = value; phoneSpecified = true; markUpdateSpecified(); }
		public String region() { return region; }
		public void setRegion(String value) { region = value; regionSpecified = true; markUpdateSpecified(); }
		public String detail() { return detail; }
		public void setDetail(String value) { detail = value; detailSpecified = true; markUpdateSpecified(); }
		public Boolean isDefault() { return isDefault; }
		@com.fasterxml.jackson.annotation.JsonProperty("isDefault")
		public void setDefault(Boolean value) { isDefault = value; defaultSpecified = true; markUpdateSpecified(); }
		@Override @com.fasterxml.jackson.annotation.JsonIgnore @jakarta.validation.constraints.AssertTrue
		public boolean isUpdateSpecified() { return super.isUpdateSpecified(); }
		public boolean recipientSpecified() { return recipientSpecified; }
		public boolean phoneSpecified() { return phoneSpecified; }
		public boolean regionSpecified() { return regionSpecified; }
		public boolean detailSpecified() { return detailSpecified; }
		public boolean defaultSpecified() { return defaultSpecified; }
	}

	record AddressView(long id, long userId, String recipient, String phone, String region, String detail,
			boolean isDefault, Instant createdAt, Instant updatedAt) {}
}
