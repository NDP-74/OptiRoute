package com.optiroute.backend.utils;

import com.optiroute.backend.dto.response.vehicle.SemiTrailerLightResponse;
import com.optiroute.backend.dto.response.vehicle.SemiTrailerResponse;
import com.optiroute.backend.entity.vehicle.SemiTrailer;

import com.optiroute.backend.dto.response.vehicle.TractorResponse;
import com.optiroute.backend.dto.response.vehicle.TractorLightResponse;
import com.optiroute.backend.entity.vehicle.Tractor;

public class VehicleUtils {
    public static SemiTrailerResponse toSemiTrailerResponse(SemiTrailer semiTrailer) {
        String trailerTypeLabel = semiTrailer.getTrailerType() != null ? semiTrailer.getTrailerType().getLabel() : null;
        return new SemiTrailerResponse(semiTrailer.getId(), semiTrailer.getRegistration(), trailerTypeLabel, semiTrailer.getPurchaseCost(), semiTrailer.getDepreciationStartDate(),
            semiTrailer.getDepreciationEndDate(), semiTrailer.getMaintenanceCostPerKm());
    }

    public static SemiTrailerLightResponse toSemiTrailerLightResponse(SemiTrailer semiTrailer) {
        return new SemiTrailerLightResponse(semiTrailer.getId(), semiTrailer.getRegistration());
    }

    public static TractorResponse toTractorResponse(Tractor tractor) {
        return new TractorResponse(tractor.getId(), tractor.getRegistration(), tractor.getPurchaseCost(), tractor.getDepreciationStartDate(), tractor.getDepreciationEndDate(),
            tractor.getMaintenanceCostPerKm());
    }

    public static TractorLightResponse toTractorLightResponse(Tractor tractor) {
        return new TractorLightResponse(tractor.getId(), tractor.getRegistration());
    }
}
