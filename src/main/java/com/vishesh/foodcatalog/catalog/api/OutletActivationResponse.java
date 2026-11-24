package com.vishesh.foodcatalog.catalog.api;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

/** Small lifecycle response returned after an activation request. */
public record OutletActivationResponse(Long id, ChainStatus status, Long version) {
}
