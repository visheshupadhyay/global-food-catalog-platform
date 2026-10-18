package com.vishesh.foodcatalog.catalog.domain;

/** The three stages in the life of a catalog object. */
public enum ChainStatus {
    DRAFT, // Being prepared; not ready for customers yet.
    ACTIVE, // Ready for the application to use.
    ARCHIVED // Kept for history but no longer active.
}
