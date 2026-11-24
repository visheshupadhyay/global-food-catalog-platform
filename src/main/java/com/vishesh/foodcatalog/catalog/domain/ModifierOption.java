package com.vishesh.foodcatalog.catalog.domain;

import java.math.BigDecimal;

/** One atomic add-on choice, such as Extra cheese for an additional 20. */
public class ModifierOption {

    private final Long id;
    private final Long modifierGroupId;
    private final String optionName;
    private final BigDecimal priceDelta;
    private final int displayOrder;

    /** Creates an option that PostgreSQL has not stored yet. */
    public ModifierOption(Long modifierGroupId, String optionName, BigDecimal priceDelta, int displayOrder) {
        this(null, modifierGroupId, optionName, priceDelta, displayOrder);
    }

    /** Recreates an option that already has a database identity. */
    public ModifierOption(Long id, Long modifierGroupId, String optionName,
            BigDecimal priceDelta, int displayOrder) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Modifier option ID must be greater than zero");
        }
        if (modifierGroupId == null || modifierGroupId <= 0) {
            throw new IllegalArgumentException("Modifier group ID must be greater than zero");
        }
        if (optionName == null || optionName.isBlank()) {
            throw new IllegalArgumentException("Modifier option name cannot be blank");
        }
        if (priceDelta != null && priceDelta.signum() < 0) {
            throw new IllegalArgumentException("Modifier option price delta cannot be negative");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Modifier option display order cannot be negative");
        }
        this.id = id;
        this.modifierGroupId = modifierGroupId;
        this.optionName = optionName.trim();
        this.priceDelta = priceDelta == null ? BigDecimal.ZERO : priceDelta;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getModifierGroupId() {
        return modifierGroupId;
    }

    public String getOptionName() {
        return optionName;
    }

    public BigDecimal getPriceDelta() {
        return priceDelta;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
