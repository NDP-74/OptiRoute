package com.optiroute.backend.dto.request.transport;

import com.optiroute.backend.entity.transport.PlanningView.RowType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlanningViewRequest(
    @NotBlank @Size(max = 100) String name,
    @NotEmpty List<@NotNull @Valid Row> rows
) {
    public record Row(@NotNull RowType type, @Positive Long driverId) {}
}
