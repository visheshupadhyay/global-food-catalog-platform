package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

@ExtendWith(MockitoExtension.class)
public class RestaurantChainServiceTest {
    @Mock
    RestaurantChainRepository repository;

    RestaurantChainService service;

    @BeforeEach
    void setUp() {
        service = new RestaurantChainService(repository);
    }

    @Test
    void createChain_savesNewChain() {
        when(repository.existsByBusinessCode("BURGER-KING-100")).thenReturn(false);
        when(repository.save(any(RestaurantChain.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantChain newRestaurant = service.createChain("BURGER-KING-100", "BURGER-KING");
        assertNotNull(newRestaurant.getCreatedAt());
        verify(repository, times(1)).save(any(RestaurantChain.class));
    }

    @Test
    void createChain_rejectsDuplicateBusinessCode() {
        when(repository.existsByBusinessCode("BURGER-KING-100")).thenReturn(true);
        assertThrows(DuplicateBusinessCodeException.class,
                () -> service.createChain("BURGER-KING-100",
                        "BURGER-KING"));
        verify(repository).existsByBusinessCode("BURGER-KING-100");
        verify(repository, never()).save(any(RestaurantChain.class));
    }

    @Test
    void createChain_passesCorrectValuesToDomain() {
        String businessCode = "BERLIN-CHAIN-100";
        String displayName = "Berlin Burgers";

        when(repository.existsByBusinessCode(businessCode)).thenReturn(false);
        when(repository.save(any(RestaurantChain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createChain(businessCode, displayName);

        ArgumentCaptor<RestaurantChain> captor = ArgumentCaptor.forClass(RestaurantChain.class);

        verify(repository).save(captor.capture());

        RestaurantChain savedChain = captor.getValue();

        assertEquals(businessCode, savedChain.getBusinessCode());
        assertEquals(displayName, savedChain.getDisplayName());
        assertEquals(ChainStatus.DRAFT, savedChain.getStatus());
    }

    @Test
    void createChain_rejectsBlankBusinessCode() {
        String businessCode = "  ";
        String displayName = "Berlin Burgers";

        assertThrows(IllegalArgumentException.class,
                () -> service.createChain(businessCode, displayName));
        verifyNoInteractions(repository);
    }

    @Test
    void createChain_rejectsBlankDisplayName() {
        String businessCode = "BERLIN-CHAIN-100";
        String displayName = "  ";

        assertThrows(IllegalArgumentException.class,
                () -> service.createChain(businessCode, displayName));
        verifyNoInteractions(repository);
    }

    @Test
    void createChain_rejectsNullBusinessCode() {
        String businessCode = null;
        String displayName = "Berlin Burgers";

        assertThrows(IllegalArgumentException.class,
                () -> service.createChain(businessCode, displayName));
        verifyNoInteractions(repository);

    }

    @Test
    void createChain_rejectsNullDisplayName() {
        String businessCode = "BERLIN-CHAIN-100";
        String displayName = null;

        assertThrows(IllegalArgumentException.class,
                () -> service.createChain(businessCode, displayName));
        verifyNoInteractions(repository);
    }

    @Test
    void createChain_normalizesMixedCaseBusinessCode() {
        String businessCode = " berLin-Chain-100 ";
        String displayName = "Berlin Burgers";

        when(repository.existsByBusinessCode(businessCode.trim().toUpperCase(Locale.ROOT))).thenReturn(false);
        when(repository.save(any(RestaurantChain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createChain(businessCode, displayName);

        ArgumentCaptor<RestaurantChain> captor = ArgumentCaptor.forClass(RestaurantChain.class);

        verify(repository).save(captor.capture());

        RestaurantChain savedChain = captor.getValue();

        assertEquals(businessCode.trim().toUpperCase(Locale.ROOT), savedChain.getBusinessCode());
        assertEquals(displayName, savedChain.getDisplayName());
        assertEquals(ChainStatus.DRAFT, savedChain.getStatus());
    }

    @Test
    void createChain_normalizesLowercaseBusinessCode() {
        String businessCode = " berlin-chain-100 ";
        String displayName = "Berlin Burgers";

        when(repository.existsByBusinessCode(businessCode.trim().toUpperCase(Locale.ROOT))).thenReturn(false);
        when(repository.save(any(RestaurantChain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createChain(businessCode, displayName);

        ArgumentCaptor<RestaurantChain> captor = ArgumentCaptor.forClass(RestaurantChain.class);

        verify(repository).save(captor.capture());

        RestaurantChain savedChain = captor.getValue();

        assertEquals(businessCode.trim().toUpperCase(Locale.ROOT), savedChain.getBusinessCode());
        assertEquals(displayName, savedChain.getDisplayName());
        assertEquals(ChainStatus.DRAFT, savedChain.getStatus());
    }

    @Test
    void createChain_rejectsDuplicateAfterBusinessCodeNormalization() {
        String businessCode = " berlin-chain-100 ";
        String displayName = "Berlin Burgers";

        when(repository.existsByBusinessCode(businessCode.trim().toUpperCase(Locale.ROOT))).thenReturn(false);
        when(repository.save(any(RestaurantChain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createChain(businessCode, displayName);
        when(repository.existsByBusinessCode(businessCode.trim().toUpperCase(Locale.ROOT))).thenReturn(true);
        assertThrows(DuplicateBusinessCodeException.class, () -> service.createChain(businessCode, displayName));
        ArgumentCaptor<RestaurantChain> captor = ArgumentCaptor.forClass(RestaurantChain.class);

        verify(repository).save(captor.capture());

        RestaurantChain savedChain = captor.getValue();

        assertEquals(businessCode.trim().toUpperCase(Locale.ROOT), savedChain.getBusinessCode());
        assertEquals(displayName, savedChain.getDisplayName());
        assertEquals(ChainStatus.DRAFT, savedChain.getStatus());
        verify(repository).save(any(RestaurantChain.class));
    }

    @Test
    void findById_returnsChainWhenRepositoryFindsIt() {
        RestaurantChain persistedChain = persistedChain(42L, "DH-DE", "Delivery Hero Germany");
        when(repository.findById(42L)).thenReturn(Optional.of(persistedChain));

        RestaurantChain result = service.findChainById(42L);

        assertEquals(persistedChain, result);
        verify(repository).findById(42L);
    }

    @Test
    void findById_throwsNotFoundWhenRepositoryIsEmpty() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        RestaurantChainNotFoundException exception = assertThrows(RestaurantChainNotFoundException.class,
                () -> service.findChainById(42L));
        assertEquals("Restaurant chain with ID: 42 not found", exception.getMessage());
        verify(repository).findById(42L);
    }

    @Test
    void findByBusinessCode_normalizesLowercaseInput() {
        RestaurantChain persistedChain = persistedChain(42L, "DH-DE", "Delivery Hero Germany");
        when(repository.findByBusinessCode("DH-DE")).thenReturn(Optional.of(persistedChain));

        RestaurantChain result = service.findChainByBusinessCode(" dh-de ");

        assertEquals(persistedChain, result);
        verify(repository).findByBusinessCode("DH-DE");
    }

    @Test
    void findByBusinessCode_returnsChainWhenRepositoryFindsIt() {
        RestaurantChain persistedChain = persistedChain(42L, "DH-DE", "Delivery Hero Germany");
        when(repository.findByBusinessCode("DH-DE")).thenReturn(Optional.of(persistedChain));

        RestaurantChain result = service.findChainByBusinessCode("DH-DE");

        assertEquals(persistedChain, result);
        verify(repository).findByBusinessCode("DH-DE");
    }

    @Test
    void findByBusinessCode_throwsNotFoundWhenRepositoryIsEmpty() {
        when(repository.findByBusinessCode("DH-DE")).thenReturn(Optional.empty());

        assertThrows(RestaurantChainNotFoundException.class,
                () -> service.findChainByBusinessCode("DH-DE"));
        verify(repository).findByBusinessCode("DH-DE");
    }

    @Test
    void findById_rejectsInvalidId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findChainById(0L));
        verifyNoInteractions(repository);
    }

    @Test
    void findByBusinessCode_rejectsBlankBusinessCode() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findChainByBusinessCode("  "));
        verifyNoInteractions(repository);
    }

    private RestaurantChain persistedChain(Long id, String businessCode, String displayName) {
        return new RestaurantChain(
                businessCode,
                displayName,
                ChainStatus.DRAFT,
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                id);
    }


}
