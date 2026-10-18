package com.optiroute.backend.mapper;

import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.utils.CommonUtils;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class TruckConfigurationFactory {

    private static final int DEFAULT_LOADED_WEIGHT_KG = 40000;
    private static final int DEFAULT_EMPTY_WEIGHT_KG = 14000;
    private static final BigDecimal DEFAULT_LOADED_CONSUMPTION = new BigDecimal("32");
    private static final BigDecimal DEFAULT_EMPTY_CONSUMPTION = new BigDecimal("26");

    // Ensemble générique tracteur + semi-remorque EU, calé sur le profil PTV
    // EUR_TRAILER_TRUCK
    public TruckConfiguration createDefault() {
        return createDefault(false);
    }

    // Charge : 40 t (PTV total weight), consommation moyenne d'un 40 t EU.
    // A vide : poids a vide typique d'un tracteur (~7 t) + semi-remorque (~7 t) et consommation reduite (estimations).
    // Les dimensions, essieux et vitesse sont ceux du profil EUR_TRAILER_TRUCK, identiques a vide ou en charge.
    public TruckConfiguration createDefault(boolean emptyTrip) {
        return TruckConfiguration.builder().weightKg(emptyTrip ? DEFAULT_EMPTY_WEIGHT_KG : DEFAULT_LOADED_WEIGHT_KG).heightCm(400).widthCm(255)
            .lengthCm(1650).axleCount(5).maxSpeed(CommonUtils.kmhToMs(90))
            .averageConsumption(emptyTrip ? DEFAULT_EMPTY_CONSUMPTION : DEFAULT_LOADED_CONSUMPTION).build();
    }

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