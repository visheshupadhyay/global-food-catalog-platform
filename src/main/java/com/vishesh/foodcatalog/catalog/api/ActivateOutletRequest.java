package com.vishesh.foodcatalog.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The outlet version an administrator saw before asking to activate it. */
public record ActivateOutletRequest(@NotNull @Min(0) Long expectedVersion) {
}
