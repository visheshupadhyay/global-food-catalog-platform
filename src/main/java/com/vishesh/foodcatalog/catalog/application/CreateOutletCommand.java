package com.vishesh.foodcatalog.catalog.application;

/**
 * Values needed to create a draft outlet and assign its initial shared menu.
 */
public record CreateOutletCommand(String displayName, String outletCode, String currencyCode, String timezone,
		Long menuGroupId) {
}
