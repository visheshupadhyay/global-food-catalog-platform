package com.vishesh.foodcatalog.catalog.reconciliation;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Component
public class HttpEffectiveMenuStateClient implements EffectiveMenuStateClient {
    private final RestClient client;

    public HttpEffectiveMenuStateClient(RestClient.Builder builder,
            @Value("${effective-menu.service.base-url}") String baseUrl) {
        if (builder == null || baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Effective Menu client configuration is invalid");
        }
        String normalizedBaseUrl = baseUrl.trim();
        if (normalizedBaseUrl.startsWith("\"") && normalizedBaseUrl.endsWith("\"")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(1, normalizedBaseUrl.length() - 1).trim();
        }
        if (!normalizedBaseUrl.startsWith("http://") && !normalizedBaseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Effective Menu client URL must start with http:// or https://");
        }
        this.client = builder.baseUrl(normalizedBaseUrl).build();
    }

    @Override
    public Optional<EffectiveMenuSnapshotState> findSnapshotState(Long menuItemId, Long outletId) {
        if (menuItemId == null || menuItemId <= 0 || outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Menu item and outlet IDs must be positive");
        }
        try {
            EffectiveMenuSnapshotResponse response = client.get()
                    .uri("/internal/effective-menu/items/{itemId}/outlets/{outletId}", menuItemId, outletId)
                    .retrieve().body(EffectiveMenuSnapshotResponse.class);

            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(new EffectiveMenuSnapshotState(response.menuItemId(),
                    response.outletId(),
                    response.price(),
                    response.availability(),
                    response.visibility(),
                    response.version()));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (HttpServerErrorException.ServiceUnavailable exception) {
            // Effective Menu uses 503 when the outlet has no snapshot yet.
            // Reconciliation should report that as MISSING_SNAPSHOT.
            return Optional.empty();
        }
    }
}
