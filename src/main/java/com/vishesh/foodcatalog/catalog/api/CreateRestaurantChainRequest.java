package com.vishesh.foodcatalog.catalog.api;

import jakarta.validation.constraints.NotBlank;

/**
 * The two values a client must send to create a restaurant chain.
 * The server, not the client, chooses the ID, status, and timestamps.
 */
public class CreateRestaurantChainRequest {
    // A unique short code such as DH-DE.
    @NotBlank
    private String businessCode;

    @NotBlank
    // The friendly name shown to people.
    private String displayName;

    public String getBusinessCode() {
        return businessCode;
    }

    public String getDisplayName() {
        return displayName;
    }

}
