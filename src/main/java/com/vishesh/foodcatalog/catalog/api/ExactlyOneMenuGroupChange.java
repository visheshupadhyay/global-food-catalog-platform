package com.vishesh.foodcatalog.catalog.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Validation rule saying an update must change exactly one field. */
@Documented
@Constraint(validatedBy = ExactlyOneMenuGroupChangeValidator.class)
@Retention(RUNTIME)
@Target({ TYPE, ANNOTATION_TYPE })
public @interface ExactlyOneMenuGroupChange {
    String message() default "Exactly one of displayName or status must be supplied";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
