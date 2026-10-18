package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ChainStatusTest {
    @Test
    void hasExactlyThreeStatuses() {
        assertEquals(3, ChainStatus.values().length);
    }

    @Test
    void eachExpectedStatusExists() {
        assertNotNull(ChainStatus.valueOf("DRAFT"));
        assertNotNull(ChainStatus.valueOf("ACTIVE"));
        assertNotNull(ChainStatus.valueOf("ARCHIVED"));
    }

    @Test
    void enumNamesAreExactlyDraftActiveAndArchived() {
        List<String> names = Arrays.stream(ChainStatus.values())
                .map(Enum::name)
                .toList();

        assertEquals(List.of("DRAFT", "ACTIVE", "ARCHIVED"), names);
    }
}
