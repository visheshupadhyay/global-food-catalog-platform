package com.vishesh.foodcatalog.catalog.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

/** One modifier group supplied as part of a complete replacement request. */
public record ModifierGroupRequest(
        @Positive Long groupId,
        @NotBlank String groupName,
        @Min(0) int minimumSelections,
        @Min(0) int maximumSelections,
        @Min(0) int displayOrder,
        @NotEmpty List<@Valid ModifierOptionRequest> options) {
}
