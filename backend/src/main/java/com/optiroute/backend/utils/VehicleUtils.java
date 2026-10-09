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
        return new SemiTrailerResponse(semiTrailer.getId(), semiTrailer.getExternalId(), semiTrailer.getExternalSource(), semiTrailer.getRegistration(), semiTrailer.getBrand(),
            semiTrailer.getModel(), trailerTypeLabel, semiTrailer.getMaxSpeed(), semiTrailer.getEmptyWeightKg(), semiTrailer.getGrossVehicleWeightKg(), semiTrailer.getHeightCm(),
            semiTrailer.getWidthCm(), semiTrailer.getLengthCm(), semiTrailer.getAxleCount(), semiTrailer.getPurchaseCost(), semiTrailer.getDepreciationStartDate(),
            semiTrailer.getDepreciationEndDate(), semiTrailer.getMaintenanceCostPerKm(), semiTrailer.getCreatedAt(), semiTrailer.getUpdatedAt());
    }

    public static SemiTrailerLightResponse toSemiTrailerLightResponse(SemiTrailer semiTrailer) {
        return new SemiTrailerLightResponse(semiTrailer.getId(), semiTrailer.getRegistration(), semiTrailer.getBrand(), semiTrailer.getModel());
    }

    public static TractorResponse toTractorResponse(Tractor tractor) {
        return new TractorResponse(tractor.getId(), tractor.getExternalId(), tractor.getExternalSource(), tractor.getRegistration(), tractor.getBrand(), tractor.getModel(),
            tractor.getMaxSpeed(), tractor.getFuelType(), tractor.getAverageConsumption(), tractor.getAverageConsumptionEmpty(), tractor.getEmptyWeightKg(),
            tractor.getGrossCombinationWeightKg(), tractor.getHeightCm(), tractor.getWidthCm(), tractor.getLengthCm(), tractor.getAxleCount(), tractor.getPurchaseCost(),
            tractor.getDepreciationStartDate(), tractor.getDepreciationEndDate(), tractor.getMaintenanceCostPerKm(), tractor.getCreatedAt(), tractor.getUpdatedAt());
    }

    public static TractorLightResponse toTractorLightResponse(Tractor tractor) {
        return new TractorLightResponse(tractor.getId(), tractor.getRegistration(), tractor.getBrand(), tractor.getModel());
    }
}
