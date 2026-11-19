package com.optiroute.backend.controller.cost;

import com.optiroute.backend.dto.request.cost.CreateCostParameterRequest;
import com.optiroute.backend.dto.request.cost.UpdateCostParameterRequest;
import com.optiroute.backend.dto.response.cost.CostParameterResponse;
import com.optiroute.backend.dto.response.cost.CostParameterLightResponse;
import com.optiroute.backend.service.cost.CostParameterService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost-parameters")
// TO DO: Use ResponseEntity<CostParameterResponse>
public class CostParameterController {

    private final CostParameterService costParameterService;

    public CostParameterController(CostParameterService costParameterService) {
        this.costParameterService = costParameterService;
    }

    @GetMapping
    public List<CostParameterLightResponse> getAll() {
        return costParameterService.getAll();
    }

    @GetMapping("/{id}")
    public CostParameterResponse getById(@PathVariable Long id) {
        return costParameterService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CostParameterResponse create(@Valid @RequestBody CreateCostParameterRequest request) {
        return costParameterService.create(request);
    }

    @PutMapping("/{id}")
    public CostParameterResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCostParameterRequest request) {
        return costParameterService.update(id,request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        costParameterService.delete(id);
    }

    @PatchMapping("/{id}/active")
    public CostParameterResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return costParameterService.setActive(id,active);
    }
}
