package com.optiroute.backend.mapper;

import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.utils.CommonUtils;

import org.springframework.stereotype.Component;

@Component
public class TruckConfigurationFactory {

    public TruckConfiguration create(Tractor tractor, SemiTrailer semiTrailer) {
        return create(tractor,semiTrailer,false);
    }

    public TruckConfiguration create(Tractor tractor, SemiTrailer semiTrailer, boolean emptyTrip) {
        return TruckConfiguration.builder().weightKg(emptyTrip ? add(tractor.getEmptyWeightKg(),semiTrailer.getEmptyWeightKg()) : tractor.getGrossCombinationWeightKg())
            .heightCm(max(tractor.getHeightCm(),semiTrailer.getHeightCm())).widthCm(max(tractor.getWidthCm(),semiTrailer.getWidthCm()))
            .lengthCm(add(tractor.getLengthCm(),semiTrailer.getLengthCm())).axleCount(add(tractor.getAxleCount(),semiTrailer.getAxleCount()))
            .maxSpeed(CommonUtils.kmhToMs(min(tractor.getMaxSpeed(),semiTrailer.getMaxSpeed())))
            .averageConsumption(emptyTrip && tractor.getAverageConsumptionEmpty() != null ? tractor.getAverageConsumptionEmpty() : tractor.getAverageConsumption()).build();
    }

    private Integer add(Integer first, Integer second) {
        if (first == null && second == null) {
            return null;
        }

        return valueOrZero(first) + valueOrZero(second);
    }

    private Integer max(Integer first, Integer second) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return Math.max(first,second);
    }

    private Integer min(Integer first, Integer second) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return Math.min(first,second);
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }
}