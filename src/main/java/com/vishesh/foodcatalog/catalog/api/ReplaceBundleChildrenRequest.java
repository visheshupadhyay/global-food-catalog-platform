package com.vishesh.foodcatalog.catalog.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * HTTP body representing the complete direct-child replacement for a bundle.
 */
public record ReplaceBundleChildrenRequest(@NotNull @Min(0) Long expectedVersion,
		@NotEmpty List<@Valid BundleChildRequest> children) {
}
