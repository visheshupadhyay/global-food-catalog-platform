package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.Outlet;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuInitializedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupOutletRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;
import com.vishesh.foodcatalog.catalog.event.application.OutletMenuInitializedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutletServiceTest {
    @Mock OutletRepository outlets;
    @Mock RestaurantChainRepository restaurantChains;
    @Mock MenuGroupRepository menuGroups;
    @Mock MenuGroupOutletRepository menuGroupOutlets;
    @Mock OutletMenuInitializationAssembler menuInitializationAssembler;
    @Mock OutletMenuInitializedEventFactory initializedEventFactory;
    @Mock OutboxEventFactory outboxEventFactory;
    @Mock OutboxEventRepository outboxEvents;
    @Mock tools.jackson.databind.ObjectMapper objectMapper;

    private OutletService service;

    @BeforeEach
    void setUp() {
        service = new OutletService(outlets, restaurantChains, menuGroups, menuGroupOutlets,
                menuInitializationAssembler, initializedEventFactory, outboxEventFactory, outboxEvents, objectMapper);
    }

    @Test
    void createDraftOutlet_savesDraftAndMenuGroupAssignmentTogether() {
        when(restaurantChains.findById(10L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuGroups.findByIdAndRestaurantChainId(20L, 10L)).thenReturn(Optional.of(mock()));
        when(outlets.existsByRestaurantChainIdAndOutletCode(10L, "BLR-IND-01")).thenReturn(false);
        when(outlets.save(any(Outlet.class))).thenAnswer(invocation -> {
            Outlet draft = invocation.getArgument(0);
            return new Outlet(30L, draft.getRestaurantChainId(), draft.getDisplayName(), draft.getOutletCode(),
                    draft.getCurrency(), draft.getStatus(), draft.getCreatedAt(), draft.getUpdatedAt(),
                    draft.getTimezone(), 0L);
        });

        Outlet outlet = service.createDraftOutlet(10L,
                new CreateOutletCommand("Indiranagar", "BLR-IND-01", "INR", "Asia/Kolkata", 20L));

        assertEquals(30L, outlet.getId());
        assertEquals(ChainStatus.DRAFT, outlet.getStatus());
        assertEquals(0L, outlet.getVersion());
        verify(menuGroupOutlets).assignMenuGroupToOutlet(20L, 30L);
    }

    @Test
    void createDraftOutlet_rejectsMenuGroupFromAnotherChain() {
        when(restaurantChains.findById(10L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuGroups.findByIdAndRestaurantChainId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(MenuGroupNotFoundException.class, () -> service.createDraftOutlet(10L,
                new CreateOutletCommand("Indiranagar", "BLR-IND-01", "INR", "Asia/Kolkata", 99L)));

        verifyNoInteractions(outlets, menuGroupOutlets);
    }

    @Test
    void activateOutlet_savesActiveOutletAndOneInitializationOutboxEvent() throws Exception {
        Outlet draft = storedOutlet(ChainStatus.DRAFT, 0L);
        MenuGroup group = activeMenuGroup();
        Outlet active = storedOutlet(ChainStatus.ACTIVE, 1L);
        OutletMenuInitializedEvent event = new OutletMenuInitializedEvent(
                java.util.UUID.randomUUID(), 30L, 10L, 20L, 1L,
                "Asia/Kolkata", Instant.parse("2026-11-23T10:00:00Z"), "corr", List.of());

        when(outlets.findById(30L)).thenReturn(Optional.of(draft));
        when(menuGroupOutlets.findMenuGroupIdByOutletId(30L)).thenReturn(Optional.of(20L));
        when(menuGroups.findByIdAndRestaurantChainId(20L, 10L)).thenReturn(Optional.of(group));
        when(menuInitializationAssembler.assemble(draft, group)).thenReturn(List.of());
        when(outlets.save(draft)).thenReturn(active);
        when(initializedEventFactory.create(org.mockito.ArgumentMatchers.eq(30L), org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("Asia/Kolkata"), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(event);
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(outboxEventFactory.create(event, "{}")).thenReturn(mock(OutboxEvent.class));

        Outlet result = service.activateOutlet(30L, 0L);

        assertEquals(ChainStatus.ACTIVE, result.getStatus());
        assertEquals(1L, result.getVersion());
        verify(outboxEvents).save(org.mockito.ArgumentMatchers.any(OutboxEvent.class));
    }

    @Test
    void activateOutlet_whenAlreadyActive_returnsItWithoutCreatingAnotherEvent() {
        Outlet active = storedOutlet(ChainStatus.ACTIVE, 4L);
        when(outlets.findById(30L)).thenReturn(Optional.of(active));

        Outlet result = service.activateOutlet(30L, 0L);

        assertEquals(active, result);
        verifyNoInteractions(menuGroups, menuGroupOutlets, menuInitializationAssembler,
                initializedEventFactory, outboxEventFactory, outboxEvents, objectMapper);
    }

    @Test
    void activateOutlet_rejectsDraftOutletWhenAssignedGroupIsNotActive() {
        Outlet draft = storedOutlet(ChainStatus.DRAFT, 0L);
        MenuGroup draftGroup = new MenuGroup(10L, "Bangalore Menu", ChainStatus.DRAFT,
                Instant.parse("2026-11-23T10:00:00Z"), Instant.parse("2026-11-23T10:00:00Z"), 0L, 20L);
        when(outlets.findById(30L)).thenReturn(Optional.of(draft));
        when(menuGroupOutlets.findMenuGroupIdByOutletId(30L)).thenReturn(Optional.of(20L));
        when(menuGroups.findByIdAndRestaurantChainId(20L, 10L)).thenReturn(Optional.of(draftGroup));

        assertThrows(IllegalStateException.class, () -> service.activateOutlet(30L, 0L));

        verifyNoInteractions(menuInitializationAssembler, initializedEventFactory, outboxEventFactory, outboxEvents);
    }

    @Test
    void activateOutlet_rejectsStaleExpectedVersionBeforeBuildingTheMenu() {
        Outlet draft = storedOutlet(ChainStatus.DRAFT, 3L);
        when(outlets.findById(30L)).thenReturn(Optional.of(draft));

        assertThrows(OutletVersionConflictException.class, () -> service.activateOutlet(30L, 2L));

        verifyNoInteractions(menuGroups, menuGroupOutlets, menuInitializationAssembler,
                initializedEventFactory, outboxEventFactory, outboxEvents);
    }

    private static Outlet storedOutlet(ChainStatus status, Long version) {
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        return new Outlet(30L, 10L, "Indiranagar", "BLR-IND-01", Currency.getInstance("INR"),
                status, now, now, "Asia/Kolkata", version);
    }

    private static MenuGroup activeMenuGroup() {
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        return new MenuGroup(10L, "Bangalore Menu", ChainStatus.ACTIVE, now, now, 0L, 20L);
    }
}
