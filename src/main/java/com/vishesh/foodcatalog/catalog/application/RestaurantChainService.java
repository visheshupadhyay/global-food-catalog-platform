package com.vishesh.foodcatalog.catalog.application;

import java.util.Locale;

import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

/**
 * Performs the business steps for creating a restaurant chain.
 * It cleans the input, checks uniqueness, creates the domain object,
 * and asks the repository to save it.
 */
public class RestaurantChainService {
    private final RestaurantChainRepository repository;

    public RestaurantChainService(RestaurantChainRepository repository) {
        this.repository = repository;
    }

    public RestaurantChain createChain(String businessCode, String displayName) {
        if (businessCode == null || displayName == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        businessCode = businessCode.trim();
        businessCode = businessCode.toUpperCase(Locale.ROOT);
        displayName = displayName.trim();

        if (businessCode.isEmpty() || displayName.isEmpty()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        // Two chains must not share the same business identity.
        if (repository.existsByBusinessCode(businessCode)) {
            throw new DuplicateBusinessCodeException("Restaurant Chain already exists!");
        }

        // A new chain starts as DRAFT; the domain object also records its timestamps.
        RestaurantChain newRestaurantChain = new RestaurantChain(businessCode, displayName);

        return repository.save(newRestaurantChain);
    }

}
