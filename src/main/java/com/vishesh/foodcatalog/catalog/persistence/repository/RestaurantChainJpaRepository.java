package com.vishesh.foodcatalog.catalog.persistence.repository;

import com.vishesh.foodcatalog.catalog.persistence.entity.RestaurantChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data creates PostgreSQL queries from these method names. */
public interface RestaurantChainJpaRepository extends JpaRepository<RestaurantChainEntity, Long> {

    Optional<RestaurantChainEntity> findByBusinessCode(String businessCode);

    boolean existsByBusinessCode(String businessCode);
}
