# Logique de calcul des coûts

Trois axes : **driver**, **véhicule**, **structure**. Chacun combine des valeurs prédéfinies
(entités : conducteur, tracteur, semi-remorque) et des frais optionnels issus des
`Données référentielles` (`CostParameterEngine`, catégories DRIVER / VEHICLE / STRUCTURE).

## DriverCostService
- Coût conducteur par trajet = coût conducteur journalier × (durée trajet / durée journée totale) + frais conducteur optionnels
- Coût conducteur journalier = coût conducteur annuel / nb jours ouvrés annuels

## VehicleCostService
- Coût véhicule par trajet = amortissement par trajet + coût d'entretien par trajet + frais véhicule optionnels
- Amortissement par trajet = amortissement journalier × (km trajet / km total journalier)
- Amortissement journalier = coût d'achat / nb jours ouvrés sur la période d'amortissement
- Coût d'entretien par trajet = coût d'entretien (€/km, champ `maintenanceCostPerKm`) × km du trajet
  - appliqué au tracteur et à la semi-remorque (somme des deux)
- Carburant et péages (HERE) sont aussi ajoutés à la catégorie véhicule (hors périmètre de la formule ci-dessus).

## StructureCostService
- Coût de structure par trajet = frais de structure optionnels
