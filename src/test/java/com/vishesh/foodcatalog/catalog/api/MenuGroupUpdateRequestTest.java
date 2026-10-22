package com.vishesh.foodcatalog.catalog.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class MenuGroupUpdateRequestTest {
    String displayName;
    ChainStatus status;
    Long version;
    private MenuGroupUpdateRequest menuGroupUpdateRequest;
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        menuGroupUpdateRequest = new MenuGroupUpdateRequest();
    }

    @Test
    public void nameOnlyRequest_hasExactlyOneChange_returnsTrue() {
        displayName = "Burger-House";
        version = 1L;
        menuGroupUpdateRequest.setDisplayName(displayName);
        menuGroupUpdateRequest.setVersion(version);
        assertTrue(menuGroupUpdateRequest.hasExactlyOneChange());
    }

    @Test
    public void statusOnlyRequest_hasExactlyOneChange_returnsTrue() {
        version = 1L;
        status = ChainStatus.DRAFT;
        menuGroupUpdateRequest.setStatus(status);
        menuGroupUpdateRequest.setVersion(version);
        assertTrue(menuGroupUpdateRequest.hasExactlyOneChange());
    }

    @Test
    public void nameAndStatusTogether_hasExactlyOneChange_returnsFalse() {
        displayName = "Burger-House";
        version = 1L;
        status = ChainStatus.DRAFT;
        menuGroupUpdateRequest.setDisplayName(displayName);
        menuGroupUpdateRequest.setVersion(version);
        menuGroupUpdateRequest.setStatus(status);
        assertFalse(menuGroupUpdateRequest.hasExactlyOneChange());
    }

    @Test
    public void neitherNameNorStatus_hasExactlyOneChange_returnsFalse() {
        version = 1L;
        menuGroupUpdateRequest.setVersion(version);
        assertFalse(menuGroupUpdateRequest.hasExactlyOneChange());
    }

    @Test
    public void blankNameAndNoStatus_hasExactlyOneChange_returnsFalse() {
        displayName = "  ";
        version = 1L;
        menuGroupUpdateRequest.setVersion(version);
        menuGroupUpdateRequest.setDisplayName(displayName);
        assertFalse(menuGroupUpdateRequest.hasExactlyOneChange());
    }

    @Test
    void nullVersion_isRejected() {
        menuGroupUpdateRequest.setDisplayName("Berlin Specials");
        menuGroupUpdateRequest.setVersion(null);

        var violations = validator.validate(menuGroupUpdateRequest);

        assertFalse(violations.isEmpty());

        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("version")));
    }

    @Test
    public void zeroVersion_isAccepted() {
        displayName = "Burger-House";
        version = 0L;
        menuGroupUpdateRequest.setDisplayName(displayName);
        menuGroupUpdateRequest.setVersion(0L);
        var violations = validator.validate(menuGroupUpdateRequest);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void negativeVersion_isRejected() {
        displayName = "Burger-House";
        version = -1L;
        menuGroupUpdateRequest.setDisplayName(displayName);
        var violations = validator.validate(menuGroupUpdateRequest);

        assertFalse(violations.isEmpty());

        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("version")));
    }

    @Test
    void bothChanges_areRejectedByBeanValidation() {
        menuGroupUpdateRequest.setDisplayName("Burgers");
        menuGroupUpdateRequest.setStatus(ChainStatus.ACTIVE);
        menuGroupUpdateRequest.setVersion(0L);

        var violations = validator.validate(menuGroupUpdateRequest);

        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Exactly one")));
    }

    @Test
    void noChanges_areRejectedByBeanValidation() {
        menuGroupUpdateRequest.setVersion(0L);

        var violations = validator.validate(menuGroupUpdateRequest);

        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Exactly one")));
    }

    @Test
    void displayNameLongerThan100Characters_isRejected() {
        menuGroupUpdateRequest.setDisplayName("a".repeat(101));
        menuGroupUpdateRequest.setVersion(0L);

        var violations = validator.validate(menuGroupUpdateRequest);

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("displayName")));
    }
}
