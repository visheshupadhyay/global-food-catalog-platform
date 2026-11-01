package com.vishesh.foodcatalog.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.vishesh.foodcatalog.catalog.reconciliation.*;

class ReconciliationServiceTest {
    private final ReconciliationService service = new ReconciliationService();

    @Test
    void matchingStates_returnMatch() {
        assertEquals(ReconciliationResultType.MATCH,
                service.compare(state(5, "12.00", true, true, 5), snapshot(5, "12.00", true, true, 5)).getResultType());
    }

    @Test
    void missingSnapshot_isReported() {
        assertEquals(ReconciliationResultType.MISSING_SNAPSHOT,
                service.compare(state(5, "12.00", true, true, 5), null).getResultType());
    }

    @Test
    void olderSnapshot_isReportedAsStale() {
        assertEquals(ReconciliationResultType.STALE_SNAPSHOT,
                service.compare(state(5, "12.00", true, true, 5), snapshot(4, "12.00", true, true, 5)).getResultType());
    }

    @Test
    void differentPrice_isReportedAsMismatch() {
        assertEquals(ReconciliationResultType.FIELD_MISMATCH,
                service.compare(state(5, "12.00", true, true, 5), snapshot(5, "10.00", true, true, 5)).getResultType());
    }

    @Test
    void differentAvailability_isReportedAsMismatch() {
        assertEquals(ReconciliationResultType.FIELD_MISMATCH, service
                .compare(state(5, "12.00", true, true, 5), snapshot(5, "12.00", false, true, 5)).getResultType());
    }

    @Test
    void differentVisibility_isReportedAsMismatch() {
        assertEquals(ReconciliationResultType.FIELD_MISMATCH, service
                .compare(state(5, "12.00", true, true, 5), snapshot(5, "12.00", true, false, 5)).getResultType());
    }

    private CatalogMenuItemState state(long version, String price, boolean available, boolean visible, long itemId) {
        return new CatalogMenuItemState(itemId, 501L, new BigDecimal(price), available, visible, version);
    }

    private EffectiveMenuSnapshotState snapshot(long version, String price, boolean available, boolean visible,
            long itemId) {
        return new EffectiveMenuSnapshotState(itemId, 501L, new BigDecimal(price), available, visible, version);
    }
}
