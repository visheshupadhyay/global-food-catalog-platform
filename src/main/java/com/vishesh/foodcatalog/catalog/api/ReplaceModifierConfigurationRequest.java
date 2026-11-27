package com.vishesh.foodcatalog.catalog.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** HTTP body representing the complete desired modifier configuration. */
public record ReplaceModifierConfigurationRequest(@NotNull @Min(0) Long expectedVersion,
		@NotNull List<@Valid ModifierGroupRequest> groups) {
}
