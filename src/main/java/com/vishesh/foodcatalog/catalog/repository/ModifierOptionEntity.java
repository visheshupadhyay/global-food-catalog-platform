package com.vishesh.foodcatalog.catalog.repository;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA row for one atomic add-on option in a modifier group. */
@Entity
@Table(name = "modifier_option")
public class ModifierOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modifier_group_id", nullable = false)
    private Long modifierGroupId;
    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;
    @Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected ModifierOptionEntity() {
    }

    public ModifierOptionEntity(Long modifierGroupId, String optionName,
            BigDecimal priceDelta, int displayOrder) {
        this.modifierGroupId = modifierGroupId;
        this.optionName = optionName;
        this.priceDelta = priceDelta;
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

    public void update(String optionName, BigDecimal priceDelta, int displayOrder) {
        this.optionName = optionName;
        this.priceDelta = priceDelta;
        this.displayOrder = displayOrder;
    }
}
