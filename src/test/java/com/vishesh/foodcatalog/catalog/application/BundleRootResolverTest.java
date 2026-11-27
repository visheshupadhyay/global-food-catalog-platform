package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;

@ExtendWith(MockitoExtension.class)
class BundleRootResolverTest {
	@Mock
	private BundleChildRepository bundleChildRepository;

	private BundleRootResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new BundleRootResolver(bundleChildRepository);
	}

	@Test
	void findAffectedBundleIds_withoutParents_returnsChangedBundleOnly() {
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(10L)).thenReturn(List.of());

		assertEquals(List.of(10L), resolver.findAffectedBundleIds(10L));
	}

	@Test
	void findAffectedBundleIds_returnsChangedBundleAndAllAncestors() {
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(10L)).thenReturn(List.of(20L));
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(20L)).thenReturn(List.of(30L));
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(30L)).thenReturn(List.of());

		assertEquals(List.of(10L, 20L, 30L), resolver.findAffectedBundleIds(10L));
		assertEquals(List.of(20L, 30L), resolver.findAncestorBundleIds(10L));
	}

	@Test
	void findAffectedBundleIds_refreshesEveryParentThatReusesTheChangedBundle() {
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(10L)).thenReturn(List.of(30L, 20L, 20L));
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(20L)).thenReturn(List.of());
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(30L)).thenReturn(List.of());

		assertEquals(List.of(10L, 20L, 30L), resolver.findAffectedBundleIds(10L));
	}

	@Test
	void findAffectedBundleIds_stopsWhenPersistedRelationshipsAreCircular() {
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(10L)).thenReturn(List.of(20L));
		when(bundleChildRepository.findParentBundleIdsByChildMenuItemId(20L)).thenReturn(List.of(10L));

		assertEquals(List.of(10L, 20L), resolver.findAffectedBundleIds(10L));
	}

	@Test
	void findAffectedBundleIds_rejectsInvalidChangedBundleId() {
		assertThrows(IllegalArgumentException.class, () -> resolver.findAffectedBundleIds(null));
		assertThrows(IllegalArgumentException.class, () -> resolver.findAffectedBundleIds(0L));
	}
}
