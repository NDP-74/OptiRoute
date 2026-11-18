package com.optiroute.backend.controller.transport;

import com.optiroute.backend.dto.request.transport.PlanningViewRequest;
import com.optiroute.backend.dto.response.transport.PlanningViewResponse;
import com.optiroute.backend.service.transport.PlanningViewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/planning-views")
@RequiredArgsConstructor
public class PlanningViewController {
    private final PlanningViewService views;

    @GetMapping
    public List<PlanningViewResponse> list(Principal principal) {
        return views.list(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanningViewResponse create(Principal principal, @Valid @RequestBody PlanningViewRequest request) {
        return views.create(principal.getName(), request);
    }

    @PutMapping("/{id}")
    public PlanningViewResponse update(Principal principal, @PathVariable Long id, @Valid @RequestBody PlanningViewRequest request) {
        return views.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable Long id) {
        views.delete(principal.getName(), id);
    }
}
