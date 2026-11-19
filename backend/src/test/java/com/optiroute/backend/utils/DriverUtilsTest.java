package com.optiroute.backend.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.optiroute.backend.repository.driver.DriverRepository;

class DriverUtilsTest {

    @Test
    void shouldKeepLoginWhenNamesStillMatch() {
        assertTrue(DriverUtils.loginMatchesName("JDUPONT", "Jean", "Dupont"));
        assertTrue(DriverUtils.loginMatchesName("JDUPONT2", "jean", "Dupont"));
        assertFalse(DriverUtils.loginMatchesName("JDUPONT", "Jean", "Durand"));
        assertFalse(DriverUtils.loginMatchesName("JDUPONTE", "Jean", "Dupont"));
        assertFalse(DriverUtils.loginMatchesName(null, "Jean", "Dupont"));
    }

    @Test
    void shouldIgnoreTheEditedDriverWhenCheckingLoginUniqueness() {
        DriverRepository repository = mock(DriverRepository.class);
        when(repository.existsByLoginAndIdNot("JDURAND", 4L)).thenReturn(false);

        assertEquals("JDURAND", DriverUtils.generateUniqueLogin(repository, "Jean", "Durand", 4L));
        verify(repository, never()).existsByLogin(anyString());
    }

    @Test
    void shouldAddASuffixWhenAnotherDriverOwnsTheLogin() {
        DriverRepository repository = mock(DriverRepository.class);
        when(repository.existsByLogin("JDUPONT")).thenReturn(true);

        assertEquals("JDUPONT2", DriverUtils.generateUniqueLogin(repository, "Jean", "Dupont"));
        verify(repository, never()).existsByLoginAndIdNot(anyString(), anyLong());
    }

    @Test
    void shouldNotFailWithAFirstNameWithoutLatinCharacters() {
        DriverRepository repository = mock(DriverRepository.class);

        assertEquals("DUPONT", DriverUtils.generateUniqueLogin(repository, "李", "Dupont"));
    }
}
