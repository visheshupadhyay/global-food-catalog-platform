package com.vishesh.foodcatalog.catalog.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class CreateOutletMenuItemRequest {
    @NotBlank
    private String businessCode;

    @NotBlank
    private String itemName;

    @NotNull 
    @PositiveOrZero
    private BigDecimal itemPrice;

    @Positive
    private Long menuCategoryId;

    public String getBusinessCode() {
        return businessCode;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public Long getMenuCategoryId() {
        return menuCategoryId;
    }

}
