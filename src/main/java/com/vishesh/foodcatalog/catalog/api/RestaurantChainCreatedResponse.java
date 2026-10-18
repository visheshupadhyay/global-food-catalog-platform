package com.vishesh.foodcatalog.catalog.api;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

/**
 * The small receipt returned after a chain is created.
 */
public record RestaurantChainCreatedResponse(Long id, ChainStatus status) {

}
