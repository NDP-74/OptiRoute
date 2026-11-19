package com.optiroute.backend.controller.transport;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.optiroute.backend.dto.response.transport.DashdocSyncResponse;
import com.optiroute.backend.service.dashdoc.DashdocSyncService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashdoc")
@RequiredArgsConstructor
public class DashdocSyncController {

    private final DashdocSyncService dashdocSyncService;

    @PostMapping("/sync")
    public ResponseEntity<DashdocSyncResponse> synchronize(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(dashdocSyncService.synchronize(startDate, endDate));
    }
}
