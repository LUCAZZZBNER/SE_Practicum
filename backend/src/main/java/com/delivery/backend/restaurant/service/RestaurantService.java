package com.delivery.backend.restaurant.service;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.delivery.backend.common.PatchRequest;
import com.delivery.backend.common.PageResult;

/** Shop business contract. Implementation and persistence are intentionally pending. */
public interface RestaurantService {

	ShopView create(long merchantId, CreateRequest request);

	PageResult<ShopView> list(ListQuery query);

	ShopView get(long shopId);

	ShopView update(long merchantId, long shopId, UpdateRequest request);

	ShopSnapshot requireOrderable(long shopId);

	ShopSnapshot requireOwned(long merchantId, long shopId);

	ShopSnapshot requireOwnedForRead(long merchantId, long shopId);

	record CreateRequest(@NotBlank String name, String description) {
	}

	final class UpdateRequest extends PatchRequest {
		@Pattern(regexp = "(?s).*\\S.*")
		private String name;
		private String description;
		@Pattern(regexp = "OPEN|CLOSED|TEMPORARILY_CLOSED")
		private String status;
		private boolean nameSpecified;
		private boolean descriptionSpecified;
		private boolean statusSpecified;

		public UpdateRequest() {
		}

		@Override
		@JsonIgnore
		@AssertTrue
		public boolean isUpdateSpecified() {
			return super.isUpdateSpecified();
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			this.nameSpecified = true;
			markUpdateSpecified();
		}

		public String description() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
			this.descriptionSpecified = true;
			markUpdateSpecified();
		}

		public String status() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
			this.statusSpecified = true;
			markUpdateSpecified();
		}

		@JsonIgnore
		public boolean isNameSpecified() {
			return nameSpecified;
		}

		@JsonIgnore
		public boolean isDescriptionSpecified() {
			return descriptionSpecified;
		}

		@JsonIgnore
		public boolean isStatusSpecified() {
			return statusSpecified;
		}
	}

	record ListQuery(Integer page, Integer pageSize, String keyword, String status, Boolean mine, String sortBy,
		String sortOrder, Long merchantId) {
	}

	record ShopView(long id, long merchantId, String name, String description, String status,
			Instant createdAt, Instant updatedAt) {
	}

	record ShopSnapshot(long id, long merchantId, String name, String status) {
	}
}
