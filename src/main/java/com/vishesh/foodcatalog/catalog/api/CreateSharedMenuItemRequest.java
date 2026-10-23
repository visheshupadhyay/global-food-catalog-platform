package com.vishesh.foodcatalog.catalog.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class CreateSharedMenuItemRequest {
    @NotBlank
    private String businessCode;

    @NotBlank
    private String itemName;

    @NotNull 
    @PositiveOrZero
    private BigDecimal itemPrice;

    @NotNull
    @Positive
    private Long menuGroupId;

    @NotNull
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

    public Long getMenuGroupId() {
        return menuGroupId;
    }

    public Long getMenuCategoryId() {
        return menuCategoryId;
    }

}
