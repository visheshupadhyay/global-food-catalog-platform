package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.ModifierGroup;
import com.vishesh.foodcatalog.catalog.domain.ModifierOption;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.ModifierGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.ModifierOptionRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

@ExtendWith(MockitoExtension.class)
class ModifierConfigurationServiceTest {

    @Mock
    private RestaurantChainRepository restaurantChainRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private ModifierGroupRepository modifierGroupRepository;

    @Mock
    private ModifierOptionRepository modifierOptionRepository;

    @Mock
    private OutboxEventFactory outboxEventFactory;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private tools.jackson.databind.ObjectMapper objectMapper;

    private ModifierConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new ModifierConfigurationService(restaurantChainRepository, menuItemRepository,
                modifierGroupRepository, modifierOptionRepository, new MenuItemChangedEventFactory(),
                outboxEventFactory, outboxEventRepository, objectMapper,
                new ModifierConfigurationSnapshotAssembler(modifierGroupRepository, modifierOptionRepository));
    }

    @Test
    void createGroup_withValidOptions_savesGroupAndEveryOption() {
        CreateModifierGroupCommand command = validCommand();
        ModifierGroup savedGroup = new ModifierGroup(50L, 10L, "Choose extras", 0, 2, 0);

        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(modifierGroupRepository.save(any(ModifierGroup.class))).thenReturn(savedGroup);
        when(menuItemRepository.findAffectedOutletIds(10L)).thenReturn(List.of(30L));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modifierGroupRepository.findByOwnerMenuItemId(10L)).thenReturn(List.of(savedGroup));
        when(modifierOptionRepository.findByModifierGroupId(50L)).thenReturn(List.of(
                new ModifierOption(70L, 50L, "Extra cheese", BigDecimal.valueOf(20), 0),
                new ModifierOption(71L, 50L, "Bacon", BigDecimal.valueOf(50), 1)));
        when(objectMapper.writeValueAsString(any(MenuItemChangedEvent.class))).thenReturn("{}");
        when(outboxEventFactory.create(any(MenuItemChangedEvent.class), any(String.class)))
                .thenReturn(mock(OutboxEvent.class));

        ModifierGroup result = service.createGroup(command);

        assertEquals(savedGroup, result);
        verify(modifierGroupRepository).save(any(ModifierGroup.class));

        ArgumentCaptor<ModifierOption> optionCaptor = ArgumentCaptor.forClass(ModifierOption.class);
        verify(modifierOptionRepository, org.mockito.Mockito.times(2)).save(optionCaptor.capture());
        assertEquals(50L, optionCaptor.getAllValues().get(0).getModifierGroupId());
        assertEquals("Extra cheese", optionCaptor.getAllValues().get(0).getOptionName());
        assertEquals(50L, optionCaptor.getAllValues().get(1).getModifierGroupId());
        assertEquals("Bacon", optionCaptor.getAllValues().get(1).getOptionName());
        verify(menuItemRepository).save(any(MenuItem.class));
        ArgumentCaptor<MenuItemChangedEvent> eventCaptor = ArgumentCaptor.forClass(MenuItemChangedEvent.class);
        verify(outboxEventFactory).create(eventCaptor.capture(), any(String.class));
        assertEquals(OutboxEventType.MODIFIER_CONFIGURATION_CHANGED, eventCaptor.getValue().getEventType());
        assertEquals(30L, eventCaptor.getValue().getOutletId());
        assertEquals(1, eventCaptor.getValue().getModifierGroups().size());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void createGroup_withNoOptions_isRejectedWithoutSavingAnything() {
        CreateModifierGroupCommand command = new CreateModifierGroupCommand(
                10L, 1L, "Choose extras", 0, 0, 0, 0L, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.createGroup(command));

        verify(modifierGroupRepository, never()).save(any());
        verify(modifierOptionRepository, never()).save(any());
    }

    @Test
    void createGroup_withMaximumAboveOptionCount_isRejectedWithoutSavingAnything() {
        CreateModifierGroupCommand command = new CreateModifierGroupCommand(
                10L, 1L, "Choose extras", 0, 3, 0, 0L,
                List.of(new CreateModifierOptionCommand("Extra cheese", BigDecimal.valueOf(20), 0),
                        new CreateModifierOptionCommand("Bacon", BigDecimal.valueOf(50), 1)));

        assertThrows(IllegalArgumentException.class, () -> service.createGroup(command));

        verify(modifierGroupRepository, never()).save(any());
        verify(modifierOptionRepository, never()).save(any());
    }

    @Test
    void createGroup_withOwnerFromAnotherChain_isRejectedWithoutSavingAnything() {
        CreateModifierGroupCommand command = validCommand();
        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(MenuItemNotFoundException.class, () -> service.createGroup(command));

        verify(modifierGroupRepository, never()).save(any());
        verify(modifierOptionRepository, never()).save(any());
    }

    @Test
    void createGroup_withStaleOwnerVersion_isRejectedBeforeAnyWrite() {
        CreateModifierGroupCommand command = validCommand();
        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger(1L)));

        assertThrows(MenuItemVersionConflictException.class, () -> service.createGroup(command));

        verify(modifierGroupRepository, never()).save(any());
        verify(modifierOptionRepository, never()).save(any());
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void createGroup_withoutAffectedOutlets_isRejectedBeforeAnyWrite() {
        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(menuItemRepository.findAffectedOutletIds(10L)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.createGroup(validCommand()));

        verify(modifierGroupRepository, never()).save(any());
        verify(modifierOptionRepository, never()).save(any());
        verify(menuItemRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void createGroup_forSharedBurger_createsOneOutboxEventPerAffectedOutlet() {
        ModifierGroup savedGroup = new ModifierGroup(50L, 10L, "Choose extras", 0, 2, 0);
        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(menuItemRepository.findAffectedOutletIds(10L)).thenReturn(List.of(30L, 31L, 32L));
        when(modifierGroupRepository.save(any(ModifierGroup.class))).thenReturn(savedGroup);
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modifierGroupRepository.findByOwnerMenuItemId(10L)).thenReturn(List.of(savedGroup));
        when(modifierOptionRepository.findByModifierGroupId(50L)).thenReturn(List.of(
                new ModifierOption(70L, 50L, "Extra cheese", BigDecimal.valueOf(20), 0),
                new ModifierOption(71L, 50L, "Bacon", BigDecimal.valueOf(50), 1)));
        when(objectMapper.writeValueAsString(any(MenuItemChangedEvent.class))).thenReturn("{}");
        when(outboxEventFactory.create(any(MenuItemChangedEvent.class), any(String.class)))
                .thenReturn(mock(OutboxEvent.class));

        service.createGroup(validCommand());

        ArgumentCaptor<MenuItemChangedEvent> eventCaptor = ArgumentCaptor.forClass(MenuItemChangedEvent.class);
        verify(outboxEventFactory, org.mockito.Mockito.times(3)).create(eventCaptor.capture(), any(String.class));
        assertEquals(List.of(30L, 31L, 32L), eventCaptor.getAllValues().stream()
                .map(MenuItemChangedEvent::getOutletId)
                .toList());
        verify(outboxEventRepository, org.mockito.Mockito.times(3)).save(any(OutboxEvent.class));
    }

    @Test
    void replaceConfiguration_withUnchangedConfiguration_doesNotWriteOrPublish() {
        ModifierGroup existingGroup = new ModifierGroup(50L, 10L, "Choose extras", 0, 2, 0);
        ModifierOption cheese = new ModifierOption(70L, 50L, "Extra cheese", BigDecimal.valueOf(20), 0);
        ModifierOption bacon = new ModifierOption(71L, 50L, "Bacon", BigDecimal.valueOf(50), 1);
        ReplaceModifierConfigurationCommand command = new ReplaceModifierConfigurationCommand(10L, 1L, 0L,
                List.of(new ModifierGroupConfiguration(50L, "Choose extras", 0, 2, 0,
                        List.of(new ModifierOptionConfiguration(70L, "Extra cheese", BigDecimal.valueOf(20), 0),
                                new ModifierOptionConfiguration(71L, "Bacon", BigDecimal.valueOf(50), 1)))));

        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(modifierGroupRepository.findByOwnerMenuItemId(10L)).thenReturn(List.of(existingGroup));
        when(modifierOptionRepository.findByModifierGroupId(50L)).thenReturn(List.of(cheese, bacon));

        service.replaceConfiguration(command);

        verify(modifierGroupRepository, never()).save(any());
        verify(modifierOptionRepository, never()).save(any());
        verify(modifierGroupRepository, never()).deleteById(any());
        verify(modifierOptionRepository, never()).deleteById(any());
        verify(menuItemRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void replaceConfiguration_withGroupOwnedByAnotherBurger_isRejectedBeforeAnyWrite() {
        ReplaceModifierConfigurationCommand command = new ReplaceModifierConfigurationCommand(10L, 1L, 0L,
                List.of(new ModifierGroupConfiguration(999L, "Choose extras", 0, 1, 0,
                        List.of(new ModifierOptionConfiguration(null, "Extra cheese", BigDecimal.valueOf(20), 0)))));

        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(modifierGroupRepository.findByOwnerMenuItemId(10L)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.replaceConfiguration(command));

        verify(modifierGroupRepository, never()).save(any());
        verify(modifierOptionRepository, never()).save(any());
        verify(menuItemRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void replaceConfiguration_withNoGroups_deletesOptionsBeforeTheirGroupAndPublishesEmptySnapshot() {
        ModifierGroup existingGroup = new ModifierGroup(50L, 10L, "Choose extras", 0, 1, 0);
        ModifierOption cheese = new ModifierOption(70L, 50L, "Extra cheese", BigDecimal.valueOf(20), 0);
        ReplaceModifierConfigurationCommand command = new ReplaceModifierConfigurationCommand(10L, 1L, 0L, List.of());

        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(modifierGroupRepository.findByOwnerMenuItemId(10L))
                .thenReturn(List.of(existingGroup), List.of());
        when(modifierOptionRepository.findByModifierGroupId(50L)).thenReturn(List.of(cheese));
        when(menuItemRepository.findAffectedOutletIds(10L)).thenReturn(List.of(30L));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any(MenuItemChangedEvent.class))).thenReturn("{}");
        when(outboxEventFactory.create(any(MenuItemChangedEvent.class), any(String.class)))
                .thenReturn(mock(OutboxEvent.class));

        service.replaceConfiguration(command);

        org.mockito.InOrder removalOrder = org.mockito.Mockito.inOrder(modifierOptionRepository,
                modifierGroupRepository);
        removalOrder.verify(modifierOptionRepository).deleteById(70L);
        removalOrder.verify(modifierGroupRepository).deleteById(50L);
        ArgumentCaptor<MenuItemChangedEvent> eventCaptor = ArgumentCaptor.forClass(MenuItemChangedEvent.class);
        verify(outboxEventFactory).create(eventCaptor.capture(), any(String.class));
        assertEquals(List.of(), eventCaptor.getValue().getModifierGroups());
    }

    @Test
    void replaceConfiguration_updatesKnownOption_createsNewOptionAndDeletesOmittedOption() {
        ModifierGroup existingGroup = new ModifierGroup(50L, 10L, "Choose extras", 0, 2, 0);
        ModifierOption cheese = new ModifierOption(70L, 50L, "Extra cheese", BigDecimal.valueOf(20), 0);
        ModifierOption bacon = new ModifierOption(71L, 50L, "Bacon", BigDecimal.valueOf(50), 1);
        ModifierOption updatedCheese = new ModifierOption(70L, 50L, "Extra cheese", BigDecimal.valueOf(30), 0);
        ModifierOption sauce = new ModifierOption(72L, 50L, "BBQ sauce", BigDecimal.valueOf(10), 1);
        ReplaceModifierConfigurationCommand command = new ReplaceModifierConfigurationCommand(10L, 1L, 0L,
                List.of(new ModifierGroupConfiguration(50L, "Choose extras", 0, 2, 0,
                        List.of(new ModifierOptionConfiguration(70L, "Extra cheese", BigDecimal.valueOf(30), 0),
                                new ModifierOptionConfiguration(null, "BBQ sauce", BigDecimal.valueOf(10), 1)))));

        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(modifierGroupRepository.findByOwnerMenuItemId(10L)).thenReturn(List.of(existingGroup));
        when(modifierOptionRepository.findByModifierGroupId(50L))
                .thenReturn(List.of(cheese, bacon), List.of(updatedCheese, sauce));
        when(menuItemRepository.findAffectedOutletIds(10L)).thenReturn(List.of(30L));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any(MenuItemChangedEvent.class))).thenReturn("{}");
        when(outboxEventFactory.create(any(MenuItemChangedEvent.class), any(String.class)))
                .thenReturn(mock(OutboxEvent.class));

        service.replaceConfiguration(command);

        ArgumentCaptor<ModifierOption> optionCaptor = ArgumentCaptor.forClass(ModifierOption.class);
        verify(modifierOptionRepository, org.mockito.Mockito.times(2)).save(optionCaptor.capture());
        assertEquals(70L, optionCaptor.getAllValues().get(0).getId());
        assertEquals(BigDecimal.valueOf(30), optionCaptor.getAllValues().get(0).getPriceDelta());
        assertEquals(null, optionCaptor.getAllValues().get(1).getId());
        assertEquals("BBQ sauce", optionCaptor.getAllValues().get(1).getOptionName());
        verify(modifierOptionRepository).deleteById(71L);
        verify(modifierGroupRepository, never()).save(any());
    }

    @Test
    void replaceConfiguration_withNewGroupAndOption_createsBothAndPublishesTheCompleteSnapshot() {
        ModifierGroup savedGroup = new ModifierGroup(50L, 10L, "Choose sauce", 0, 1, 0);
        ModifierOption savedOption = new ModifierOption(70L, 50L, "BBQ sauce", BigDecimal.valueOf(10), 0);
        ReplaceModifierConfigurationCommand command = new ReplaceModifierConfigurationCommand(10L, 1L, 0L,
                List.of(new ModifierGroupConfiguration(null, "Choose sauce", 0, 1, 0,
                        List.of(new ModifierOptionConfiguration(null, "BBQ sauce", BigDecimal.valueOf(10), 0)))));

        when(restaurantChainRepository.findById(1L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(menuItemRepository.findByIdAndRestaurantChainId(10L, 1L)).thenReturn(Optional.of(burger()));
        when(modifierGroupRepository.findByOwnerMenuItemId(10L)).thenReturn(List.of(), List.of(savedGroup));
        when(modifierGroupRepository.save(any(ModifierGroup.class))).thenReturn(savedGroup);
        when(modifierOptionRepository.findByModifierGroupId(50L)).thenReturn(List.of(savedOption));
        when(menuItemRepository.findAffectedOutletIds(10L)).thenReturn(List.of(30L));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any(MenuItemChangedEvent.class))).thenReturn("{}");
        when(outboxEventFactory.create(any(MenuItemChangedEvent.class), any(String.class)))
                .thenReturn(mock(OutboxEvent.class));

        service.replaceConfiguration(command);

        verify(modifierGroupRepository).save(any(ModifierGroup.class));
        ArgumentCaptor<ModifierOption> optionCaptor = ArgumentCaptor.forClass(ModifierOption.class);
        verify(modifierOptionRepository).save(optionCaptor.capture());
        assertEquals(50L, optionCaptor.getValue().getModifierGroupId());
        assertEquals("BBQ sauce", optionCaptor.getValue().getOptionName());
        ArgumentCaptor<MenuItemChangedEvent> eventCaptor = ArgumentCaptor.forClass(MenuItemChangedEvent.class);
        verify(outboxEventFactory).create(eventCaptor.capture(), any(String.class));
        assertEquals(1, eventCaptor.getValue().getModifierGroups().size());
    }

    private CreateModifierGroupCommand validCommand() {
        return new CreateModifierGroupCommand(10L, 1L, "Choose extras", 0, 2, 0,
                0L,
                List.of(new CreateModifierOptionCommand("Extra cheese", BigDecimal.valueOf(20), 0),
                        new CreateModifierOptionCommand("Bacon", BigDecimal.valueOf(50), 1)));
    }

    private MenuItem burger() {
        return burger(0L);
    }

    private MenuItem burger(Long version) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new MenuItem(10L, "Burger", BigDecimal.TEN, now, 20L, 30L, null,
                "BURGER-10", MenuItemStatus.ACTIVE, true, true, version, 1L, now);
    }
}
