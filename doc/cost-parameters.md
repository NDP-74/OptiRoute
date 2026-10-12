# Référentiel des coûts (CostParameter)

Les `CostParameter` sont les frais définis dans les `Données référentielles`. Ils sont consommés par
`CostParameterEngine` pour les catégories `DRIVER`, `VEHICLE` et `STRUCTURE` (voir `cost-calculation.md`).

## Champs principaux

| Champ | Rôle |
|-------|------|
| `category` | Axe de coût : `DRIVER`, `VEHICLE`, `STRUCTURE` |
| `label` | Libellé affiché sur le trajet |
| `value` + `unit` | Montant et unité de calcul (voir ci-dessous) |
| `active` | Un paramètre inactif n'est jamais compté |
| `assignmentType` | Mode d'assignation : `AUTOMATIC` (défaut) ou `MANUAL` |
| `rule` | Conditions d'applicabilité (optionnelles) |

## Unités

| Unité | Calcul pour un trajet |
|-------|-----------------------|
| `EUR_PER_KM` | distance du trajet × valeur |
| `EUR_PER_TRIP` | valeur (forfait par trajet) |
| `EUR_PER_HOUR` | durée du trajet (h) × valeur |
| `EUR_PER_DAY` | valeur / nombre de trajets du jour |
| `EUR_PER_MONTH` | (valeur / jours ouvrés du mois) × (km trajet / km journaliers du véhicule) |
| `EUR_PER_YEAR` | (valeur / jours ouvrés de l'année) / nombre de trajets du jour |

## Type d'assignation (`CostParameterAssignmentType`)

### `AUTOMATIC`
- Le paramètre est compté sur **tous** les trajets de sa catégorie.
- S'il n'a pas de condition (`rule`), il est toujours compté.
- S'il a des conditions, elles sont vérifiées pour chaque trajet ; le coût n'est compté que si elles sont satisfaites.
- Reste soumis à `active = true`.

### `MANUAL`
- Le paramètre n'est **jamais** compté automatiquement.
- Il n'est compté que si un **événement** (`Event`) l'utilise, et uniquement sur **le trajet de cet événement**.
- Un événement exige obligatoirement : un paramètre `MANUAL`, un trajet (via le transport), et un seul véhicule
  (tracteur **ou** semi-remorque) appartenant à ce trajet. Un point d'intérêt est optionnel.
- Sans événement rattaché à un trajet, un paramètre `MANUAL` ne doit avoir aucun impact sur ce trajet.
- Un événement peut être supprimé depuis le détail du transport, après confirmation.
  `DELETE /api/events/{id}` supprime l'événement et recalcule l'itinéraire avec les points
  d'intérêt des événements restants, y compris lorsque la dernière étape est supprimée.
  Ces opérations sont transactionnelles : un échec du recalcul annule la suppression.
  Le détail du transport (carte, événements et coûts) est ensuite rechargé.
- Lors de la modification d'un transport, les événements géolocalisés sont préremplis
  comme points de passage verrouillés. Ils restent inclus dans les calculs et ne peuvent
  être remplacés, déplacés ou supprimés depuis le formulaire d'itinéraire.
  Les étapes ajoutées manuellement restent modifiables et supprimables.

### Résumé

| | `AUTOMATIC` | `MANUAL` |
|---|---|---|
| Compté sans événement | Oui (si conditions OK) | **Non** |
| Lien avec un trajet | Tous les trajets de la catégorie | Uniquement via un événement |
| Conditions (`rule`) | Vérifiées | N/A |

## Implémentation

- CostParameterEngine.calculateCosts ignore les paramètres MANUAL.
- TransportCostService.calculate ajoute, pour chaque événement du trajet, le coût du paramètre MANUAL associé (CostParameterEngine.calculateManualCost) dans la catégorie du paramètre. Un même paramètre assigné à deux événements est compté deux fois.
