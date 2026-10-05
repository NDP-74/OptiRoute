package com.optiroute.backend.controller.transport;

import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.optiroute.backend.dto.request.transport.TransportFromRouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.transport.TransportDetailResponse;
import com.optiroute.backend.dto.response.transport.PlanningResponse;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.service.transport.TransportDetailService;
import com.optiroute.backend.service.transport.TransportFacadeService;
import com.optiroute.backend.service.transport.TransportPlanningService;

@RestController
@RequestMapping("/api/transports")
public class TransportController {

    private final TransportFacadeService transportFacadeService;
    private final TransportPlanningService transportPlanningService;
    private final TransportDetailService transportDetailService;

    public TransportController(TransportFacadeService transportFacadeService, TransportPlanningService transportPlanningService, TransportDetailService transportDetailService) {
        this.transportFacadeService = transportFacadeService;
        this.transportPlanningService = transportPlanningService;
        this.transportDetailService = transportDetailService;
    }

    @PostMapping
    public ResponseEntity<Service> create(@RequestBody TransportRequest request) {
        Service service = transportFacadeService.createTransport(request);

        return ResponseEntity.ok(service);
    }

    @GetMapping("/planning")
    public ResponseEntity<PlanningResponse> getPlanning(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(transportPlanningService.getPlanning(startDate,endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(transportDetailService.getDetail(id));
    }

    @PostMapping("/from-route")
    public ResponseEntity<Service> createFromRoute(@RequestBody TransportFromRouteRequest request) {
        Service service = transportFacadeService.createFromRoute(request);

        return ResponseEntity.ok(service);
    }

    @PutMapping("/{id}/from-route")
    public ResponseEntity<Service> updateFromRoute(@PathVariable Long id, @RequestBody TransportFromRouteRequest request) {
        Service service = transportFacadeService.updateFromRoute(id,request);

        return ResponseEntity.ok(service);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransport(@PathVariable Long id) {
        transportFacadeService.deleteTransport(id);

        return ResponseEntity.noContent().build();
    }

}