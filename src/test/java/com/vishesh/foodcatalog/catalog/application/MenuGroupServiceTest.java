package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

@ExtendWith(MockitoExtension.class)
public class MenuGroupServiceTest {
    @Mock
    MenuGroupRepository repository;

    @Mock
    RestaurantChainRepository restaurantChainRepository;

    MenuGroupService service;

    @BeforeEach
    void setUp() {
        service = new MenuGroupService(repository, restaurantChainRepository);
    }

    @Test
    void createMenuGroup_savesNewDraftGroup() {
        when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(repository.existsNonArchivedByRestaurantChainIdAndDisplayName(anyLong(), anyString())).thenReturn(false);
        when(repository.save(any(MenuGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MenuGroup newMenuGroup = service.createMenuGroup(101L, "BURGER-KING");
        assertNotNull(newMenuGroup);
        assertEquals(ChainStatus.DRAFT, newMenuGroup.getStatus());
        assertEquals(101L, newMenuGroup.getRestaurantChainId());
        assertEquals("BURGER-KING", newMenuGroup.getDisplayName());
        verify(repository).save(newMenuGroup);
        assertEquals(0L, newMenuGroup.getVersion());
    }

    @Test
    void createMenuGroup_rejectsDuplicateWorkingName() {
        when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(repository.existsNonArchivedByRestaurantChainIdAndDisplayName(anyLong(), anyString())).thenReturn(true);
        assertThrows(DuplicateMenuGroupException.class,
                () -> service.createMenuGroup(101L, "BURGER-KING"));

        verify(repository).existsNonArchivedByRestaurantChainIdAndDisplayName(101L, "BURGER-KING");
        verify(repository, never()).save(any(MenuGroup.class));
    }

    @Test
    void createMenuGroup_allowsNameWhenOnlyArchivedGroupExists() {
        when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(repository.existsNonArchivedByRestaurantChainIdAndDisplayName(anyLong(), anyString())).thenReturn(false);
        when(repository.save(any(MenuGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MenuGroup newMenuGroup = service.createMenuGroup(101L, "BURGER-KING");
        assertEquals(ChainStatus.DRAFT, newMenuGroup.getStatus());
        verify(repository).save(newMenuGroup);
        assertEquals(0L, newMenuGroup.getVersion());
    }

    @Test
    void createMenuGroup_trimsDisplayNameBeforeCheckingAndSaving() {
        when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
        when(repository.existsNonArchivedByRestaurantChainIdAndDisplayName(anyLong(), anyString())).thenReturn(false);
        when(repository.save(any(MenuGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuGroup result = service.createMenuGroup(101L, "  BURGER-KING  ");

        assertEquals("BURGER-KING", result.getDisplayName());
        verify(repository).existsNonArchivedByRestaurantChainIdAndDisplayName(101L, "BURGER-KING");
        verify(repository).save(result);
        assertEquals(0L, result.getVersion());
    }

    @Test
    void createMenuGroup_rejectsMissingRestaurantChain() {
        when(restaurantChainRepository.findById(101L)).thenReturn(Optional.empty());

        assertThrows(RestaurantChainNotFoundException.class,
                () -> service.createMenuGroup(101L, "BURGER-KING"));

        verify(restaurantChainRepository).findById(101L);
        verifyNoInteractions(repository);
    }

    @Test
    void createMenuGroup_rejectsNullRestaurantChainId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createMenuGroup(null, "BURGER-KING"));
        verifyNoRepositoryInteractions();
    }

    @Test
    void createMenuGroup_rejectsNonPositiveRestaurantChainId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createMenuGroup(0L, "BURGER-KING"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createMenuGroup(-1L, "BURGER-KING"));
        verifyNoRepositoryInteractions();
    }

    @Test
    void createMenuGroup_rejectsNullDisplayName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createMenuGroup(101L, null));
        verifyNoRepositoryInteractions();
    }

    @Test
    void createMenuGroup_rejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createMenuGroup(101L, "   "));
        verifyNoRepositoryInteractions();
    }

    @Test
    void findMenuGroupById_whenChainAndGroupMatch_returnsGroup() {
        MenuGroup expected = new MenuGroup(101L, "Burgers",
                ChainStatus.DRAFT,
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                3L,
                42L);
        when(repository.findByIdAndRestaurantChainId(42L, 101L))
                .thenReturn(Optional.of(expected));

        MenuGroup actual = service.findMenuGroupById(101L, 42L);

        assertEquals(expected, actual);
        verify(repository).findByIdAndRestaurantChainId(42L, 101L);
    }

    @Test
    void findMenuGroupById_whenGroupBelongsToAnotherChain_throwsNotFound() {
        when(repository.findByIdAndRestaurantChainId(42L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(MenuGroupNotFoundException.class,
                () -> service.findMenuGroupById(101L, 42L));

        verify(repository).findByIdAndRestaurantChainId(42L, 101L);
    }

    @Test
    void findMenuGroupById_rejectsInvalidChainId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findMenuGroupById(0L, 42L));
        assertThrows(IllegalArgumentException.class,
                () -> service.findMenuGroupById(-1L, 42L));
        verifyNoInteractions(repository);
    }

    @Test
    void findMenuGroupById_rejectsInvalidGroupId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findMenuGroupById(101L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> service.findMenuGroupById(101L, -1L));
        verifyNoInteractions(repository);
    }

    @Test
    void findMenuGroupById_passesIdsToRepositoryInCorrectOrder() {
        when(repository.findByIdAndRestaurantChainId(42L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(MenuGroupNotFoundException.class,
                () -> service.findMenuGroupById(101L, 42L));

        verify(repository).findByIdAndRestaurantChainId(42L, 101L);
    }

    private void verifyNoRepositoryInteractions() {
        verifyNoInteractions(restaurantChainRepository);
        verify(repository, never()).existsNonArchivedByRestaurantChainIdAndDisplayName(anyLong(), anyString());
        verify(repository, never()).save(any(MenuGroup.class));
    }
}
