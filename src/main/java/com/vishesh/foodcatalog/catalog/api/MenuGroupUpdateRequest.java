package com.vishesh.foodcatalog.catalog.api;


import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Carries one versioned menu-group change from an administrator. */
@ExactlyOneMenuGroupChange
public class MenuGroupUpdateRequest {
    @Size(max = 100)
    private String displayName;
    private ChainStatus status;

    @NotNull
    @PositiveOrZero
    private Long version;

    public MenuGroupUpdateRequest() {

    }

    public String getDisplayName() {
        return displayName;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setStatus(ChainStatus status) {
        this.status = status;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean hasExactlyOneChange() {
        boolean hasName = displayName != null && !displayName.trim().isEmpty();
        boolean hasStatus = status != null;

        return hasName ^ hasStatus;
    }

}
