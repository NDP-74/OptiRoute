package com.optiroute.backend.controller.transport;

import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.optiroute.backend.dto.request.transport.TransportFromRouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.transport.TransportDetailResponse;
import com.optiroute.backend.dto.response.transport.PlanningResponse;
import com.optiroute.backend.entity.transport.Transport;
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
    public ResponseEntity<Transport> create(@RequestBody TransportRequest request) {
        Transport transport = transportFacadeService.createTransport(request);

        return ResponseEntity.ok(transport);
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
    public ResponseEntity<Transport> createFromRoute(@RequestBody TransportFromRouteRequest request) {
        Transport transport = transportFacadeService.createFromRoute(request);

        return ResponseEntity.ok(transport);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransport(@PathVariable Long id) {
        transportFacadeService.deleteTransport(id);

        return ResponseEntity.noContent().build();
    }

}