package com.vishesh.foodcatalog.catalog.application;

import org.springframework.stereotype.Service;
import com.vishesh.foodcatalog.catalog.api.MenuGroupUpdateRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

/** Runs menu-group rules before data is stored or returned to the API. */
@Service
public class MenuGroupService {
    private final MenuGroupRepository repository;
    private final RestaurantChainRepository restaurantChainRepository;

    public MenuGroupService(MenuGroupRepository repository, RestaurantChainRepository restaurantChainRepository) {
        this.repository = repository;
        this.restaurantChainRepository = restaurantChainRepository;
    }

    public MenuGroup createMenuGroup(Long chainId, String displayName) {
        if (chainId == null || chainId <= 0) throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        if (displayName == null) throw new IllegalArgumentException("Value cannot be null");
        displayName = displayName.trim();
        if (displayName.isBlank()) throw new IllegalArgumentException("Value cannot be blank");
        if (restaurantChainRepository.findById(chainId).isEmpty()) throw new RestaurantChainNotFoundException("Restaurant chain with ID: " + chainId + " not found");
        if (repository.existsNonArchivedByRestaurantChainIdAndDisplayName(chainId, displayName)) throw new DuplicateMenuGroupException("Menu group with name: " + displayName + " already exists");
        return repository.save(new MenuGroup(chainId, displayName));
    }

    public MenuGroup findMenuGroupById(Long chainId, Long groupId) {
        validateIds(chainId, groupId);
        return repository.findByIdAndRestaurantChainId(groupId, chainId)
                .orElseThrow(() -> new MenuGroupNotFoundException("Menu group with ID: " + groupId + " not found in restaurant chain " + chainId));
    }

    public MenuGroup updateMenuGroup(Long chainId, Long groupId, MenuGroupUpdateRequest request) {
        validateIds(chainId, groupId);
        validateExactlyOneChange(request);
        MenuGroup group = repository.findByIdAndRestaurantChainId(groupId, chainId)
                .orElseThrow(() -> new MenuGroupNotFoundException("Menu group with ID: " + groupId + " not found in restaurant chain " + chainId));
        if (!group.getVersion().equals(request.getVersion())) throw new MenuGroupVersionConflictException("Menu group version is stale");
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) group.changeDisplayName(request.getDisplayName());
        else group.changeStatusTo(request.getStatus());

        return repository.save(group);
    }

    private void validateIds(Long chainId, Long groupId) {
        if (chainId == null || chainId <= 0) throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        if (groupId == null || groupId <= 0) throw new IllegalArgumentException("Menu group ID must be greater than zero");
    }

    private void validateExactlyOneChange(MenuGroupUpdateRequest request) {
        if (request == null || !request.hasExactlyOneChange()) throw new IllegalArgumentException("Exactly one of displayName or status must be supplied");
    }
}
