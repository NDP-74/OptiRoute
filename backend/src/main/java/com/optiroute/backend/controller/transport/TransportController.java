package com.optiroute.backend.controller.transport;

import java.time.LocalDate;
import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.optiroute.backend.dto.request.transport.RouteSegmentRequest;
import com.optiroute.backend.dto.request.transport.ServiceInfoRequest;
import com.optiroute.backend.dto.request.transport.TransportFromRouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.transport.TransportDetailResponse;
import com.optiroute.backend.dto.response.transport.PlanningResponse;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.service.transport.TransportDetailService;
import com.optiroute.backend.service.transport.TransportFacadeService;
import com.optiroute.backend.service.transport.TransportPlanningService;
import com.optiroute.backend.service.transport.PlanningViewService;
import com.optiroute.backend.entity.transport.PlanningView.RowType;

@RestController
@RequestMapping("/api/transports")
public class TransportController {

    private final TransportFacadeService transportFacadeService;
    private final TransportPlanningService transportPlanningService;
    private final TransportDetailService transportDetailService;
    private final PlanningViewService planningViewService;

    public TransportController(TransportFacadeService transportFacadeService, TransportPlanningService transportPlanningService, TransportDetailService transportDetailService, PlanningViewService planningViewService) {
        this.transportFacadeService = transportFacadeService;
        this.transportPlanningService = transportPlanningService;
        this.transportDetailService = transportDetailService;
        this.planningViewService = planningViewService;
    }

    @PostMapping
    public ResponseEntity<Service> create(@Valid @RequestBody TransportRequest request) {
        Service service = transportFacadeService.createTransport(request);

        return ResponseEntity.ok(service);
    }

    @GetMapping("/planning")
    public ResponseEntity<PlanningResponse> getPlanning(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long viewId, Principal principal) {
        if (viewId == null) {
            return ResponseEntity.ok(transportPlanningService.getPlanning(startDate, endDate));
        }
        var rows = planningViewService.get(principal.getName(), viewId).rows();
        List<Long> driverIds = rows.stream().filter(row -> row.type() == RowType.DRIVER).map(row -> row.driverId()).toList();
        boolean includeUnusedVehicles = rows.stream().anyMatch(row -> row.type() == RowType.UNUSED_VEHICLES);
        return ResponseEntity.ok(transportPlanningService.getPlanning(startDate, endDate, driverIds, includeUnusedVehicles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(transportDetailService.getDetail(id));
    }

    @GetMapping("/{serviceId}/routes/{routeId}")
    public ResponseEntity<TransportDetailResponse> getRouteDetail(@PathVariable Long serviceId, @PathVariable Long routeId) {
        return ResponseEntity.ok(transportDetailService.getDetail(serviceId, routeId));
    }

    @PostMapping("/from-route")
    public ResponseEntity<Service> createFromRoute(@Valid @RequestBody TransportFromRouteRequest request) {
        Service service = transportFacadeService.createFromRoute(request);

        return ResponseEntity.ok(service);
    }

    @PutMapping("/{id}/from-route")
    public ResponseEntity<Service> updateFromRoute(@PathVariable Long id, @Valid @RequestBody TransportFromRouteRequest request) {
        Service service = transportFacadeService.updateFromRoute(id,request);

        return ResponseEntity.ok(service);
    }

    @PutMapping("/{serviceId}/routes/{routeId}")
    public ResponseEntity<Void> updateRoute(@PathVariable Long serviceId, @PathVariable Long routeId,
            @Valid @RequestBody RouteSegmentRequest request) {
        transportFacadeService.updateRoute(serviceId, routeId, request);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateServiceInfo(@PathVariable Long id, @Valid @RequestBody ServiceInfoRequest request) {
        transportFacadeService.updateServiceInfo(id, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransport(@PathVariable Long id) {
        transportFacadeService.deleteTransport(id);

        return ResponseEntity.noContent().build();
    }

}