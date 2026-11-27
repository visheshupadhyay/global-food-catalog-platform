package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;
import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;

@ExtendWith(MockitoExtension.class)
class BundleGraphValidatorTest {
	@Mock
	private BundleChildRepository bundleChildRepository;

	private BundleGraphValidator validator;

	@BeforeEach
	void setUp() {
		validator = new BundleGraphValidator(bundleChildRepository);
	}

	@Test
	void validateReplacement_directProductChild_isValid() {
		when(bundleChildRepository.findByBundleMenuItemId(2L)).thenReturn(List.of());

		assertDoesNotThrow(() -> validator.validateReplacement(1L, List.of(new BundleChild(1L, 2L, 1, 0))));
	}

	@Test
	void validateReplacement_threeNestingHops_isValid() {
		when(bundleChildRepository.findByBundleMenuItemId(2L)).thenReturn(List.of(new BundleChild(2L, 3L, 1, 0)));
		when(bundleChildRepository.findByBundleMenuItemId(3L)).thenReturn(List.of(new BundleChild(3L, 4L, 1, 0)));
		when(bundleChildRepository.findByBundleMenuItemId(4L)).thenReturn(List.of());

		assertDoesNotThrow(() -> validator.validateReplacement(1L, List.of(new BundleChild(1L, 2L, 1, 0))));
	}

	@Test
	void validateReplacement_fourNestingHops_isRejected() {
		when(bundleChildRepository.findByBundleMenuItemId(2L)).thenReturn(List.of(new BundleChild(2L, 3L, 1, 0)));
		when(bundleChildRepository.findByBundleMenuItemId(3L)).thenReturn(List.of(new BundleChild(3L, 4L, 1, 0)));
		when(bundleChildRepository.findByBundleMenuItemId(4L)).thenReturn(List.of(new BundleChild(4L, 5L, 1, 0)));

		assertThrows(IllegalArgumentException.class,
				() -> validator.validateReplacement(1L, List.of(new BundleChild(1L, 2L, 1, 0))));
	}

	@Test
	void validateReplacement_indirectCycle_isRejected() {
		when(bundleChildRepository.findByBundleMenuItemId(2L)).thenReturn(List.of(new BundleChild(2L, 1L, 1, 0)));

		assertThrows(IllegalArgumentException.class,
				() -> validator.validateReplacement(1L, List.of(new BundleChild(1L, 2L, 1, 0))));
	}

	@Test
	void validateReplacement_exactly250ResolvedComponents_isValid() {
		List<BundleChild> children = IntStream.rangeClosed(2, 251)
				.mapToObj(id -> new BundleChild(1L, (long) id, 1, id - 2)).toList();
		children.forEach(child -> when(bundleChildRepository.findByBundleMenuItemId(child.getChildMenuItemId()))
				.thenReturn(List.of()));

		assertDoesNotThrow(() -> validator.validateReplacement(1L, children));
	}

	@Test
	void validateReplacement_moreThan250ResolvedComponents_isRejected() {
		List<BundleChild> children = IntStream.rangeClosed(2, 252)
				.mapToObj(id -> new BundleChild(1L, (long) id, 1, id - 2)).toList();
		children.stream().limit(250)
				.forEach(child -> when(bundleChildRepository.findByBundleMenuItemId(child.getChildMenuItemId()))
						.thenReturn(List.of()));

		assertThrows(IllegalArgumentException.class, () -> validator.validateReplacement(1L, children));
	}
}
