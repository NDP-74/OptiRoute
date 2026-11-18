package com.optiroute.backend.dto.response.transport;

import com.optiroute.backend.entity.transport.PlanningView.Row;

import java.util.List;

public record PlanningViewResponse(Long id, String name, List<Row> rows) {}
