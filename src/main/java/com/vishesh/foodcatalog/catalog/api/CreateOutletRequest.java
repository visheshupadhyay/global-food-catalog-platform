package com.vishesh.foodcatalog.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP body used to create a draft outlet with its first menu-group assignment.
 */
public class CreateOutletRequest {
	@NotBlank
	private String displayName;

	@NotBlank
	private String outletCode;

	@NotBlank
	private String currencyCode;

	@NotBlank
	private String timezone;

	@NotNull
	@Positive
	private Long menuGroupId;

	public String getDisplayName() {
		return displayName;
	}

	public String getOutletCode() {
		return outletCode;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public String getTimezone() {
		return timezone;
	}

	public Long getMenuGroupId() {
		return menuGroupId;
	}
}
