package com.vishesh.foodcatalog.catalog.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.ModifierGroup;
import com.vishesh.foodcatalog.catalog.domain.ModifierOption;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierGroup;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierOption;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.ModifierGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.ModifierOptionRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

@Service
public class ModifierConfigurationService {
    private final RestaurantChainRepository restaurantChainRepository;
    private final MenuItemRepository menuItemRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final MenuItemChangedEventFactory menuItemChangedEventFactory;
    private final OutboxEventFactory outboxEventFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ModifierConfigurationSnapshotAssembler snapshotAssembler;

    public ModifierConfigurationService(RestaurantChainRepository restaurantChainRepository,
            MenuItemRepository menuItemRepository, ModifierGroupRepository modifierGroupRepository,
            ModifierOptionRepository modifierOptionRepository, MenuItemChangedEventFactory menuItemChangedEventFactory,
            OutboxEventFactory outboxEventFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            ModifierConfigurationSnapshotAssembler snapshotAssembler) {

        this.restaurantChainRepository = restaurantChainRepository;
        this.menuItemRepository = menuItemRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.menuItemChangedEventFactory = menuItemChangedEventFactory;
        this.outboxEventFactory = outboxEventFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.snapshotAssembler = snapshotAssembler;
    }

    @Transactional
    public MenuItem replaceConfiguration(ReplaceModifierConfigurationCommand command) {
        // The request describes the desired final state, not one small edit.
        // Validate its shape before loading or changing any persisted data.
        validateCommand(command);
        if (restaurantChainRepository.findById(command.restaurantChainId()).isEmpty()) {
            throw new RestaurantChainNotFoundException(
                    "Restaurant chain with ID " + command.restaurantChainId() + " was not found");
        }

        // Modifier groups belong to the parent menu-item aggregate. Loading the
        // owner through the chain also prevents cross-chain modifications.
        MenuItem owner = menuItemRepository
                .findByIdAndRestaurantChainId(command.ownerMenuItemId(), command.restaurantChainId())
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Menu item with ID " + command.ownerMenuItemId()
                                + " was not found for restaurant chain " + command.restaurantChainId()));
        if (!owner.getVersion().equals(command.expectedVersion())) {
            throw new MenuItemVersionConflictException(
                    "Menu item version is stale; expected " + owner.getVersion()
                            + " but received " + command.expectedVersion());
        }

        // Build an in-memory view of the current configuration. We need it to
        // decide which supplied IDs are updates and which old rows were omitted.
        List<ModifierGroup> existingGroups = modifierGroupRepository.findByOwnerMenuItemId(owner.getId());
        Map<Long, ModifierGroup> existingGroupsById = new HashMap<>();
        Map<Long, Map<Long, ModifierOption>> existingOptionsByGroupId = new HashMap<>();
        for (ModifierGroup existingGroup : existingGroups) {
            existingGroupsById.put(existingGroup.getId(), existingGroup);
            Map<Long, ModifierOption> optionsById = new HashMap<>();
            for (ModifierOption existingOption : modifierOptionRepository
                    .findByModifierGroupId(existingGroup.getId())) {
                optionsById.put(existingOption.getId(), existingOption);
            }
            existingOptionsByGroupId.put(existingGroup.getId(), optionsById);
        }

        // Never trust client-supplied IDs: a group/option may only be changed by
        // the menu item that already owns it.
        validateOwnership(command.groups(), existingGroupsById, existingOptionsByGroupId);
        if (!configurationChanged(command.groups(), existingGroupsById, existingOptionsByGroupId)) {
            // Identical replacement requests are safe no-ops: no new aggregate
            // version and no unnecessary Kafka/outbox work.
            return owner;
        }

        // Find all outlet read-models before writes. A shared item must notify
        // every outlet that inherits it, not only the catalog owner.
        List<Long> affectedOutletIds = menuItemRepository.findAffectedOutletIds(owner.getId());
        if (affectedOutletIds == null || affectedOutletIds.isEmpty()) {
            throw new IllegalArgumentException("No affected outlets were found for the menu item");
        }

        // Reconciliation preserves known IDs, creates rows with no ID, and
        // removes rows omitted from the submitted final state.
        reconcileConfiguration(owner.getId(), command.groups(), existingGroupsById, existingOptionsByGroupId);

        // One logical modifier change advances the parent aggregate once.
        // The new version orders this event against other Burger changes.
        owner.markStructureChanged();
        MenuItem savedOwner = menuItemRepository.save(owner);
        publishModifierConfigurationChanged(savedOwner, affectedOutletIds, snapshotAssembler.assemble(owner.getId()));
        return savedOwner;
    }

    private void reconcileConfiguration(Long ownerMenuItemId, List<ModifierGroupConfiguration> requestedGroups,
            Map<Long, ModifierGroup> existingGroupsById,
            Map<Long, Map<Long, ModifierOption>> existingOptionsByGroupId) {
        Set<Long> retainedGroupIds = new HashSet<>();

        for (ModifierGroupConfiguration requestedGroup : requestedGroups) {
            ModifierGroup savedGroup;
            if (requestedGroup.groupId() == null) {
                savedGroup = modifierGroupRepository.save(new ModifierGroup(
                        ownerMenuItemId,
                        requestedGroup.groupName(), requestedGroup.minimumSelections(),
                        requestedGroup.maximumSelections(), requestedGroup.displayOrder()));
            } else {
                retainedGroupIds.add(requestedGroup.groupId());
                ModifierGroup existingGroup = existingGroupsById.get(requestedGroup.groupId());
                ModifierGroup desiredGroup = new ModifierGroup(existingGroup.getId(),
                        existingGroup.getOwnerMenuItemId(),
                        requestedGroup.groupName(), requestedGroup.minimumSelections(),
                        requestedGroup.maximumSelections(), requestedGroup.displayOrder());
                savedGroup = sameGroup(existingGroup, desiredGroup)
                        ? existingGroup
                        : modifierGroupRepository.save(desiredGroup);
            }

            Map<Long, ModifierOption> existingOptions = requestedGroup.groupId() == null
                    ? Map.of()
                    : existingOptionsByGroupId.get(requestedGroup.groupId());
            Set<Long> retainedOptionIds = new HashSet<>();
            for (ModifierOptionConfiguration requestedOption : requestedGroup.options()) {
                if (requestedOption.optionId() == null) {
                    modifierOptionRepository.save(new ModifierOption(savedGroup.getId(), requestedOption.optionName(),
                            requestedOption.priceDelta(), requestedOption.displayOrder()));
                    continue;
                }

                retainedOptionIds.add(requestedOption.optionId());
                ModifierOption existingOption = existingOptions.get(requestedOption.optionId());
                ModifierOption desiredOption = new ModifierOption(existingOption.getId(), savedGroup.getId(),
                        requestedOption.optionName(), requestedOption.priceDelta(), requestedOption.displayOrder());
                if (!sameOption(existingOption, desiredOption)) {
                    modifierOptionRepository.save(desiredOption);
                }
            }

            // An existing option absent from this group request was deliberately
            // removed, so delete only that option.
            for (ModifierOption existingOption : existingOptions.values()) {
                if (!retainedOptionIds.contains(existingOption.getId())) {
                    modifierOptionRepository.deleteById(existingOption.getId());
                }
            }
        }

        // An existing group absent from the top-level request was removed.
        // Delete its children first so PostgreSQL foreign keys remain valid.
        for (ModifierGroup existingGroup : existingGroupsById.values()) {
            if (retainedGroupIds.contains(existingGroup.getId())) {
                continue;
            }
            for (ModifierOption existingOption : existingOptionsByGroupId.get(existingGroup.getId()).values()) {
                modifierOptionRepository.deleteById(existingOption.getId());
            }
            modifierGroupRepository.deleteById(existingGroup.getId());
        }
    }

    private void validateOwnership(List<ModifierGroupConfiguration> requestedGroups,
            Map<Long, ModifierGroup> existingGroupsById,
            Map<Long, Map<Long, ModifierOption>> existingOptionsByGroupId) {
        for (ModifierGroupConfiguration requestedGroup : requestedGroups) {
            if (requestedGroup.groupId() == null) {
                for (ModifierOptionConfiguration requestedOption : requestedGroup.options()) {
                    if (requestedOption.optionId() != null) {
                        throw new IllegalArgumentException(
                                "An existing modifier option cannot be moved into a new modifier group");
                    }
                }
                continue;
            }

            if (!existingGroupsById.containsKey(requestedGroup.groupId())) {
                throw new IllegalArgumentException(
                        "Modifier group " + requestedGroup.groupId() + " does not belong to this menu item");
            }

            Map<Long, ModifierOption> optionsInGroup = existingOptionsByGroupId.get(requestedGroup.groupId());
            for (ModifierOptionConfiguration requestedOption : requestedGroup.options()) {
                if (requestedOption.optionId() != null && !optionsInGroup.containsKey(requestedOption.optionId())) {
                    throw new IllegalArgumentException(
                            "Modifier option " + requestedOption.optionId() + " does not belong to modifier group "
                                    + requestedGroup.groupId());
                }
            }
        }
    }

    private boolean configurationChanged(List<ModifierGroupConfiguration> requestedGroups,
            Map<Long, ModifierGroup> existingGroupsById,
            Map<Long, Map<Long, ModifierOption>> existingOptionsByGroupId) {
        // Different counts mean at least one create or delete is required.
        if (requestedGroups.size() != existingGroupsById.size()) {
            return true;
        }

        for (ModifierGroupConfiguration requestedGroup : requestedGroups) {
            if (requestedGroup.groupId() == null) {
                return true;
            }
            ModifierGroup existingGroup = existingGroupsById.get(requestedGroup.groupId());
            ModifierGroup desiredGroup = new ModifierGroup(existingGroup.getId(), existingGroup.getOwnerMenuItemId(),
                    requestedGroup.groupName(), requestedGroup.minimumSelections(),
                    requestedGroup.maximumSelections(), requestedGroup.displayOrder());
            if (!sameGroup(existingGroup, desiredGroup)) {
                return true;
            }

            Map<Long, ModifierOption> existingOptions = existingOptionsByGroupId.get(requestedGroup.groupId());
            if (requestedGroup.options().size() != existingOptions.size()) {
                return true;
            }
            for (ModifierOptionConfiguration requestedOption : requestedGroup.options()) {
                if (requestedOption.optionId() == null) {
                    return true;
                }
                ModifierOption existingOption = existingOptions.get(requestedOption.optionId());
                ModifierOption desiredOption = new ModifierOption(existingOption.getId(),
                        existingOption.getModifierGroupId(),
                        requestedOption.optionName(), requestedOption.priceDelta(), requestedOption.displayOrder());
                if (!sameOption(existingOption, desiredOption)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sameGroup(ModifierGroup first, ModifierGroup second) {
        return first.getGroupName().equals(second.getGroupName())
                && first.getMinimumSelections() == second.getMinimumSelections()
                && first.getMaximumSelections() == second.getMaximumSelections()
                && first.getDisplayOrder() == second.getDisplayOrder();
    }

    private boolean sameOption(ModifierOption first, ModifierOption second) {
        return first.getOptionName().equals(second.getOptionName())
                && first.getPriceDelta().compareTo(second.getPriceDelta()) == 0
                && first.getDisplayOrder() == second.getDisplayOrder();
    }

    private void publishModifierConfigurationChanged(MenuItem owner, List<Long> affectedOutletIds,
            List<EventModifierGroup> modifierSnapshot) {
        // The event intentionally carries a full snapshot. The Effective Menu
        // service can replace its configuration safely even after a retry.
        try {
            for (Long outletId : affectedOutletIds) {
                MenuItemChangedEvent event = menuItemChangedEventFactory.createModifierConfigurationChanged(
                        owner.getId(), owner.getRestaurantChainId(), owner.getVersion(), outletId,
                        UUID.randomUUID().toString(), owner.getStatus(), owner.getItemName(), modifierSnapshot);
                String payload = objectMapper.writeValueAsString(event);
                outboxEventRepository.save(outboxEventFactory.create(event, payload));
            }
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize modifier configuration event", exception);
        }
    }

    @Transactional
    public ModifierGroup createGroup(CreateModifierGroupCommand command) {
        validateCommand(command);

        if (restaurantChainRepository.findById(command.restaurantChainId()).isEmpty()) {
            throw new RestaurantChainNotFoundException(
                    "Restaurant chain with ID " + command.restaurantChainId() + " was not found");
        }

        MenuItem owner = menuItemRepository
                .findByIdAndRestaurantChainId(command.ownerMenuItemId(), command.restaurantChainId())
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Menu item with ID " + command.ownerMenuItemId()
                                + " was not found for restaurant chain " + command.restaurantChainId()));

        if (!owner.getVersion().equals(command.expectedVersion())) {
            throw new MenuItemVersionConflictException(
                    "Menu item version is stale; expected " + owner.getVersion()
                            + " but received " + command.expectedVersion());
        }

        // Work out delivery targets before changing data. This lets us reject a
        // modifier configuration that could not be delivered to any outlet.
        List<Long> affectedOutletIds = menuItemRepository.findAffectedOutletIds(owner.getId());
        if (affectedOutletIds == null || affectedOutletIds.isEmpty()) {
            throw new IllegalArgumentException("No affected outlets were found for the menu item");
        }

        ModifierGroup savedGroup = modifierGroupRepository.save(new ModifierGroup(
                command.ownerMenuItemId(),
                command.groupName(),
                command.minimumSelections(),
                command.maximumSelections(),
                command.displayOrder()));

        for (CreateModifierOptionCommand option : command.options()) {
            modifierOptionRepository.save(new ModifierOption(
                    savedGroup.getId(),
                    option.optionName(),
                    option.priceDelta(),
                    option.displayOrder()));
        }

        // A modifier change belongs to the parent menu-item aggregate, so its
        // version must advance before a later outbox event can be ordered safely.
        owner.markStructureChanged();
        MenuItem savedOwner = menuItemRepository.save(owner);
        List<EventModifierGroup> modifierSnapshot = snapshotAssembler.assemble(owner.getId());

        try {
            for (Long outletId : affectedOutletIds) {
                MenuItemChangedEvent event = menuItemChangedEventFactory.createModifierConfigurationChanged(
                        savedOwner.getId(),
                        savedOwner.getRestaurantChainId(),
                        savedOwner.getVersion(),
                        outletId,
                        UUID.randomUUID().toString(),
                        savedOwner.getStatus(),
                        savedOwner.getItemName(),
                        modifierSnapshot);
                String payload = objectMapper.writeValueAsString(event);
                outboxEventRepository.save(outboxEventFactory.create(event, payload));
            }
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize modifier configuration event", exception);
        }

        return savedGroup;
    }

    private void validateCommand(CreateModifierGroupCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Modifier group command is required");
        }
        if (command.ownerMenuItemId() == null || command.ownerMenuItemId() <= 0) {
            throw new IllegalArgumentException("Modifier group owner menu item ID must be greater than zero");
        }
        if (command.restaurantChainId() == null || command.restaurantChainId() <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        }

        if (command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw new IllegalArgumentException("Version must be greater than or equal to zero");
        }

        List<CreateModifierOptionCommand> options = command.options();
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("A modifier group must contain at least one option");
        }
        if (command.minimumSelections() > options.size()) {
            throw new IllegalArgumentException("Minimum selections cannot exceed the number of options");
        }
        if (command.maximumSelections() > options.size()) {
            throw new IllegalArgumentException("Maximum selections cannot exceed the number of options");
        }

        Set<Integer> displayOrders = new HashSet<>();
        for (CreateModifierOptionCommand option : options) {
            if (option == null) {
                throw new IllegalArgumentException("Modifier group options cannot contain null values");
            }
            if (!displayOrders.add(option.displayOrder())) {
                throw new IllegalArgumentException("Each modifier option must have a unique display order");
            }
        }
    }

    private void validateCommand(ReplaceModifierConfigurationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Modifier configuration command is required");
        }
        if (command.ownerMenuItemId() == null || command.ownerMenuItemId() <= 0) {
            throw new IllegalArgumentException("Modifier group owner menu item ID must be greater than zero");
        }
        if (command.restaurantChainId() == null || command.restaurantChainId() <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        }

        if (command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw new IllegalArgumentException("Version must be greater than or equal to zero");
        }
        if (command.groups() == null) {
            throw new IllegalArgumentException("Modifier groups are required; use an empty list to remove all groups");
        }

        Set<Long> groupIds = new HashSet<>();
        Set<Integer> groupDisplayOrders = new HashSet<>();
        for (ModifierGroupConfiguration group : command.groups()) {
            if (group == null) {
                throw new IllegalArgumentException("Modifier groups cannot contain null values");
            }
            if (group.groupId() != null && group.groupId() <= 0) {
                throw new IllegalArgumentException("Modifier group ID must be greater than zero when supplied");
            }
            if (group.groupId() != null && !groupIds.add(group.groupId())) {
                throw new IllegalArgumentException("A modifier group ID can appear only once");
            }
            if (group.groupName() == null || group.groupName().isBlank()) {
                throw new IllegalArgumentException("Modifier group name cannot be blank");
            }
            if (group.minimumSelections() < 0 || group.maximumSelections() < group.minimumSelections()) {
                throw new IllegalArgumentException("Modifier group selection limits are invalid");
            }
            if (group.displayOrder() < 0 || !groupDisplayOrders.add(group.displayOrder())) {
                throw new IllegalArgumentException("Each modifier group must have a unique non-negative display order");
            }
            validateGroupOptions(group);
        }
    }

    private void validateGroupOptions(ModifierGroupConfiguration group) {
        if (group.options() == null || group.options().isEmpty()) {
            throw new IllegalArgumentException("A modifier group must contain at least one option");
        }
        if (group.minimumSelections() > group.options().size()
                || group.maximumSelections() > group.options().size()) {
            throw new IllegalArgumentException("Modifier group selection limits cannot exceed the number of options");
        }

        Set<Long> optionIds = new HashSet<>();
        Set<Integer> optionDisplayOrders = new HashSet<>();
        for (ModifierOptionConfiguration option : group.options()) {
            if (option == null) {
                throw new IllegalArgumentException("Modifier group options cannot contain null values");
            }
            if (option.optionId() != null && option.optionId() <= 0) {
                throw new IllegalArgumentException("Modifier option ID must be greater than zero when supplied");
            }
            if (option.optionId() != null && !optionIds.add(option.optionId())) {
                throw new IllegalArgumentException("A modifier option ID can appear only once in its group");
            }
            if (option.optionName() == null || option.optionName().isBlank()) {
                throw new IllegalArgumentException("Modifier option name cannot be blank");
            }
            if (option.priceDelta() != null && option.priceDelta().signum() < 0) {
                throw new IllegalArgumentException("Modifier option price delta cannot be negative");
            }
            if (option.displayOrder() < 0 || !optionDisplayOrders.add(option.displayOrder())) {
                throw new IllegalArgumentException(
                        "Each modifier option must have a unique non-negative display order");
            }
        }
    }
}
