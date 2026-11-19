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
import com.optiroute.backend.utils.MoneyUtils;

@Service
public class VehicleCostService {
	private static final String DEPRECIATION_LABEL = "Amortissement";
	private static final String MAINTENANCE_LABEL = "Entretien";
	private static final String TOLL_LABEL = "Péages";
	private static final String FUEL_LABEL = "Carburant";

	private final CostParameterEngine costParameterEngine;
	private final WorkingDaysService workingDaysService;

	public VehicleCostService(CostParameterEngine costParameterEngine, WorkingDaysService workingDaysService) {
		this.costParameterEngine = costParameterEngine;
		this.workingDaysService = workingDaysService;
	}

	public CostCategoryResponse calculateCosts(Tractor tractor, SemiTrailer semiTrailer, CostCalculationContext context, BigDecimal fuelCost, BigDecimal tollCost) {
		return calculateCosts(tractor,semiTrailer,context,fuelCost,tollCost,List.of(context));
	}

	public CostCategoryResponse calculateCosts(Tractor tractor, SemiTrailer semiTrailer, CostCalculationContext context, BigDecimal fuelCost, BigDecimal tollCost,
		List<CostCalculationContext> dailyContexts) {
		return calculateCosts(tractor, semiTrailer, context, fuelCost, tollCost, dailyContexts, null);
	}

	public CostCategoryResponse calculateCosts(Tractor tractor, SemiTrailer semiTrailer, CostCalculationContext context, BigDecimal fuelCost, BigDecimal tollCost,
		List<CostCalculationContext> dailyContexts, CostParameterSnapshot snapshot) {

		List<AppliedCostResponse> costs = new ArrayList<>();

		// HERE - Carburant
		costs.add(new AppliedCostResponse(FUEL_LABEL, MoneyUtils.orZero(fuelCost)));

		// HERE - Péages
		costs.add(new AppliedCostResponse(TOLL_LABEL, MoneyUtils.orZero(tollCost)));

		// Amortissement
		BigDecimal depreciationCost = calculateDepreciation(tractor,semiTrailer,context.distanceKm(),context.dailyVehicleDistanceKm(),context.date());
		costs.add(new AppliedCostResponse(DEPRECIATION_LABEL, depreciationCost));

		// Entretien
		BigDecimal maintenanceCost = calculateMaintenance(tractor,semiTrailer,context.distanceKm());
		costs.add(new AppliedCostResponse(MAINTENANCE_LABEL, maintenanceCost));

		// CostParameters VEHICLE
		costs.addAll(snapshot == null
			? costParameterEngine.calculateCosts(CostParameterCategoryType.VEHICLE,context,dailyContexts)
			: costParameterEngine.calculateCosts(CostParameterCategoryType.VEHICLE,context,dailyContexts,snapshot));

		return CostCategoryResponse.of(costs);
	}

	private BigDecimal calculateMaintenance(Tractor tractor, SemiTrailer semiTrailer, double distanceKm) {
		BigDecimal costPerKm = BigDecimal.ZERO;
		if (tractor != null && tractor.getMaintenanceCostPerKm() != null) {
			costPerKm = costPerKm.add(tractor.getMaintenanceCostPerKm());
		}
		if (semiTrailer != null && semiTrailer.getMaintenanceCostPerKm() != null) {
			costPerKm = costPerKm.add(semiTrailer.getMaintenanceCostPerKm());
		}
		return MoneyUtils.multiply(costPerKm, distanceKm);
	}

	private BigDecimal calculateDepreciation(Tractor tractor, SemiTrailer semiTrailer, double distanceKm, double dailyVehicleDistanceKm, LocalDate date) {
		if (dailyVehicleDistanceKm <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal depreciationCost = BigDecimal.ZERO;
		if (tractor != null && isInDepreciationPeriod(tractor.getDepreciationStartDate(),tractor.getDepreciationEndDate(),date)) {
			depreciationCost = depreciationCost.add(calculateVehicleDepreciation(tractor.getPurchaseCost(),tractor.getDepreciationStartDate(),
				tractor.getDepreciationEndDate(),distanceKm,dailyVehicleDistanceKm));
		}

		if (semiTrailer != null && isInDepreciationPeriod(semiTrailer.getDepreciationStartDate(),semiTrailer.getDepreciationEndDate(),date)) {
			depreciationCost = depreciationCost.add(calculateVehicleDepreciation(semiTrailer.getPurchaseCost(),semiTrailer.getDepreciationStartDate(),
				semiTrailer.getDepreciationEndDate(),distanceKm,dailyVehicleDistanceKm));
		}

		return depreciationCost;
	}

	private BigDecimal calculateVehicleDepreciation(BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate, double distanceKm,
		double dailyVehicleDistanceKm) {
		if (purchaseCost == null || depreciationStartDate == null || depreciationEndDate == null) {
			return BigDecimal.ZERO;
		}

		int workingDaysInPeriod = workingDaysService.getWorkingDaysBetween(depreciationStartDate,depreciationEndDate);
		if (workingDaysInPeriod <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal dailyDepreciation = MoneyUtils.divide(purchaseCost, workingDaysInPeriod);
		return MoneyUtils.multiply(dailyDepreciation, distanceKm / dailyVehicleDistanceKm);
	}

	private boolean isInDepreciationPeriod(LocalDate startDate, LocalDate endDate, LocalDate date) {
		if (startDate == null || endDate == null) {
			return false;
		}

		return !date.isBefore(startDate) && !date.isAfter(endDate);
	}

	// Amortissement cumulé sur la période sélectionnée pour un véhicule non affecté
	// à un conducteur, arrondi au centime
	public BigDecimal calculateDepreciationForPeriod(BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate, LocalDate periodStart,
		LocalDate periodEndExclusive) {
		if (purchaseCost == null || depreciationStartDate == null || depreciationEndDate == null) {
			return MoneyUtils.ZERO;
		}

		int workingDaysInDepreciationPeriod = workingDaysService.getWorkingDaysBetween(depreciationStartDate,depreciationEndDate);
		if (workingDaysInDepreciationPeriod <= 0) {
			return MoneyUtils.ZERO;
		}

		LocalDate periodEndInclusive = periodEndExclusive.minusDays(1);
		LocalDate overlapStart = depreciationStartDate.isAfter(periodStart) ? depreciationStartDate : periodStart;
		LocalDate overlapEnd = depreciationEndDate.isBefore(periodEndInclusive) ? depreciationEndDate : periodEndInclusive;

		if (overlapEnd.isBefore(overlapStart)) {
			return MoneyUtils.ZERO;
		}

		int workingDaysInOverlap = workingDaysService.getWorkingDaysBetween(overlapStart,overlapEnd);
		BigDecimal dailyDepreciation = MoneyUtils.divide(purchaseCost, workingDaysInDepreciationPeriod);

		return MoneyUtils.round(MoneyUtils.multiply(dailyDepreciation, workingDaysInOverlap));
	}
}
