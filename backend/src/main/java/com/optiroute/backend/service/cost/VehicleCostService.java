package com.optiroute.backend.service.cost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.dto.response.cost.AppliedCostResponse;
import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.type.cost.CostParameterCategoryType;

@Service
public class VehicleCostService {
	private static final String DEPRECIATION_LABEL = "Amortissement du véhicule";
	private static final String TOLL_LABEL = "Péages";
	private static final String FUEL_LABEL = "Carburant";

	private final CostParameterEngine costParameterEngine;
	private final WorkingDaysService workingDaysService;

	public VehicleCostService(CostParameterEngine costParameterEngine, WorkingDaysService workingDaysService) {
		this.costParameterEngine = costParameterEngine;
		this.workingDaysService = workingDaysService;
	}

	public CostCategoryResponse calculateCosts(Tractor tractor, SemiTrailer semiTrailer, CostCalculationContext context, double fuelCost, double tollCost) {
		return calculateCosts(tractor,semiTrailer,context,fuelCost,tollCost,List.of(context));
	}

	public CostCategoryResponse calculateCosts(Tractor tractor, SemiTrailer semiTrailer, CostCalculationContext context, double fuelCost, double tollCost,
		List<CostCalculationContext> dailyContexts) {

		List<AppliedCostResponse> costs = new ArrayList<>();

		// HERE - Carburant
		costs.add(new AppliedCostResponse(FUEL_LABEL, fuelCost));

		// HERE - Péages
		costs.add(new AppliedCostResponse(TOLL_LABEL, tollCost));

		// Amortissement
		double depreciationCost = calculateDepreciation(tractor,semiTrailer,context.distanceKm(),context.dailyVehicleDistanceKm(),context.date());
		costs.add(new AppliedCostResponse(DEPRECIATION_LABEL, depreciationCost));

		// CostParameters VEHICLE
		costs.addAll(costParameterEngine.calculateCosts(CostParameterCategoryType.VEHICLE,context,dailyContexts));

		double totalCost = costs.stream().mapToDouble(AppliedCostResponse::amount).sum();

		return new CostCategoryResponse(costs, totalCost);
	}

	private double calculateDepreciation(Tractor tractor, SemiTrailer semiTrailer, double distanceKm, double dailyVehicleDistanceKm, LocalDate date) {
		if (dailyVehicleDistanceKm <= 0) {
			return 0;
		}

		double depreciationCost = 0;
		if (isInDepreciationPeriod(tractor.getDepreciationStartDate(),tractor.getDepreciationEndDate(),date)) {
			depreciationCost += calculateVehicleDepreciation(tractor.getPurchaseCost(),tractor.getDepreciationStartDate(),tractor.getDepreciationEndDate(),distanceKm,
				dailyVehicleDistanceKm);
		}

		if (isInDepreciationPeriod(semiTrailer.getDepreciationStartDate(),semiTrailer.getDepreciationEndDate(),date)) {
			depreciationCost += calculateVehicleDepreciation(semiTrailer.getPurchaseCost(),semiTrailer.getDepreciationStartDate(),semiTrailer.getDepreciationEndDate(),distanceKm,
				dailyVehicleDistanceKm);
		}

		return depreciationCost;
	}

	private double calculateVehicleDepreciation(BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate, double distanceKm,
		double dailyVehicleDistanceKm) {
		if (purchaseCost == null || depreciationStartDate == null || depreciationEndDate == null) {
			return 0;
		}

		int workingDaysInPeriod = workingDaysService.getWorkingDaysBetween(depreciationStartDate,depreciationEndDate);
		if (workingDaysInPeriod <= 0) {
			return 0;
		}

		double dailyDepreciation = purchaseCost.doubleValue() / workingDaysInPeriod;
		return dailyDepreciation * (distanceKm / dailyVehicleDistanceKm);
	}

	private boolean isInDepreciationPeriod(LocalDate startDate, LocalDate endDate, LocalDate date) {
		if (startDate == null || endDate == null) {
			return false;
		}

		return !date.isBefore(startDate) && !date.isAfter(endDate);
	}

	// Amortissement cumulé sur la période sélectionnée pour un véhicule non affecté
	// à un conducteur
	public double calculateDepreciationForPeriod(BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate, LocalDate periodStart,
		LocalDate periodEndExclusive) {
		if (purchaseCost == null || depreciationStartDate == null || depreciationEndDate == null) {
			return 0;
		}

		int workingDaysInDepreciationPeriod = workingDaysService.getWorkingDaysBetween(depreciationStartDate,depreciationEndDate);
		if (workingDaysInDepreciationPeriod <= 0) {
			return 0;
		}

		LocalDate periodEndInclusive = periodEndExclusive.minusDays(1);
		LocalDate overlapStart = depreciationStartDate.isAfter(periodStart) ? depreciationStartDate : periodStart;
		LocalDate overlapEnd = depreciationEndDate.isBefore(periodEndInclusive) ? depreciationEndDate : periodEndInclusive;

		if (overlapEnd.isBefore(overlapStart)) {
			return 0;
		}

		int workingDaysInOverlap = workingDaysService.getWorkingDaysBetween(overlapStart,overlapEnd);
		double dailyDepreciation = purchaseCost.doubleValue() / workingDaysInDepreciationPeriod;

		return dailyDepreciation * workingDaysInOverlap;
	}
}