package com.vishesh.foodcatalog.catalog.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks that a request contains a name change or a status change, but not both. */
public class ExactlyOneMenuGroupChangeValidator
        implements ConstraintValidator<ExactlyOneMenuGroupChange, MenuGroupUpdateRequest> {

    @Override
    public boolean isValid(MenuGroupUpdateRequest request, ConstraintValidatorContext context) {
        return request == null || request.hasExactlyOneChange();
    }
}
