package com.vishesh.foodcatalog.catalog.persistence.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;

/** Spring Data creates the SQL needed to store and find menu groups. */
public interface MenuGroupJpaRepository extends JpaRepository<MenuGroupEntity, Long> {

    Optional<MenuGroupEntity> findByIdAndRestaurantChainId(Long id, Long restaurantChainId);

    boolean existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
            Long restaurantChainId,
            String normalizedDisplayName,
            Collection<ChainStatus> statuses);
}
