package com.vishesh.foodcatalog.catalog.api;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

/**
 * MenuGroupCreatedResponse
 */
public record MenuGroupCreatedResponse(Long id, 
    Long restaurantChainId, 
    String displayName,
    ChainStatus status,
    Long version) {

}
