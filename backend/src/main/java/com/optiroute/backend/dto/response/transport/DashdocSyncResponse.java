package com.optiroute.backend.dto.response.transport;

import java.util.List;
import java.util.Map;

public record DashdocSyncResponse(
        int created,
        int updated,
        int skipped,
        Map<String, Integer> skippedReasons,
        List<String> failures) {
}
