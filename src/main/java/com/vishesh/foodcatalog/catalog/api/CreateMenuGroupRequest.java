package com.vishesh.foodcatalog.catalog.api;

import jakarta.validation.constraints.NotBlank;

/** The menu-group name sent by an administrator. */
public class CreateMenuGroupRequest {
    // The category name shown to customers, such as Burgers or Drinks.
    @NotBlank
    private String displayName;


    public String getDisplayName() {
        return displayName;
    }


}
