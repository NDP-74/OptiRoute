package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.request.transport.PlanningViewRequest;
import com.optiroute.backend.entity.User;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.transport.PlanningView.RowType;
import com.optiroute.backend.repository.UserRepository;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.PlanningViewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlanningViewServiceTest {
    private final PlanningViewRepository views = mock(PlanningViewRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DriverRepository drivers = mock(DriverRepository.class);
    private final PlanningViewService service = new PlanningViewService(views, users, drivers);

    @Test
    void createsOwnedViewWithSelectedOrder() {
        User user = new User();
        when(users.findByEmail("owner")).thenReturn(Optional.of(user));
        when(drivers.findAllById(Set.of(3L, 7L))).thenReturn(List.of(new Driver(), new Driver()));
        when(views.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var saved = service.create("owner", new PlanningViewRequest(" Matin ", List.of(
            new PlanningViewRequest.Row(RowType.DRIVER, 7L),
            new PlanningViewRequest.Row(RowType.UNUSED_VEHICLES, null),
            new PlanningViewRequest.Row(RowType.DRIVER, 3L))));

        assertEquals("Matin", saved.name());
        assertEquals(7L, saved.rows().get(0).driverId());
        assertEquals(RowType.UNUSED_VEHICLES, saved.rows().get(1).type());
        assertEquals(3L, saved.rows().get(2).driverId());
        verify(views).save(argThat(view -> view.getOwner() == user));
    }

    @Test
    void foreignViewCannotBeReadOrDeleted() {
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(ResponseStatusException.class, () -> service.get("other", 42L)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(ResponseStatusException.class, () -> service.delete("other", 42L)).getStatusCode());
        verify(views, times(2)).findByIdAndOwnerEmail(42L, "other");
        verify(views, never()).delete(any());
    }

    @Test
    void rejectsDuplicateUnusedVehiclesRows() {
        when(users.findByEmail("owner")).thenReturn(Optional.of(new User()));
        var row = new PlanningViewRequest.Row(RowType.UNUSED_VEHICLES, null);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.create("owner", new PlanningViewRequest("Matin", List.of(row, row)))).getStatusCode());
        verifyNoInteractions(views, drivers);
    }
}
