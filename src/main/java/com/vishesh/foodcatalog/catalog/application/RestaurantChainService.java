package com.vishesh.foodcatalog.catalog.application;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

/**
 * Performs the business steps for creating a restaurant chain.
 * It cleans the input, checks uniqueness, creates the domain object,
 * and asks the repository to save it.
 */
@Service
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

    public RestaurantChain findChainById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        }
        Optional<RestaurantChain> restaurantChain = repository.findById(id);

        if (restaurantChain.isEmpty()) {
            throw new RestaurantChainNotFoundException("Restaurant chain with ID: " + id + " not found");
        }
        return restaurantChain.get();
    }

    public RestaurantChain findChainByBusinessCode(String businessCode) {
        if (businessCode == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        businessCode = businessCode.trim();
        businessCode = businessCode.toUpperCase(Locale.ROOT);

        if (businessCode.isEmpty()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        Optional<RestaurantChain> restaurantChain = repository.findByBusinessCode(businessCode);

        if (restaurantChain.isEmpty()) {
            throw new RestaurantChainNotFoundException("Restaurant chain with business code: " + businessCode + " not found");
        }
        return restaurantChain.get();
    }
}
