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
| `assignmentType` | Logique d'application : `ALWAYS` (défaut), `ALL_CONDITIONS`, `ANY_CONDITION` ou `MANUAL` |
| `conditions` | Conditions d'applicabilité, directement rattachées au paramètre |

## Unités

| Unité | Calcul pour un trajet |
|-------|-----------------------|
| `EUR_PER_KM` | distance du trajet × valeur |
| `EUR_PER_TRIP` | valeur (forfait par trajet) |
| `EUR_PER_HOUR` | durée du trajet (h) × valeur |
| `EUR_PER_DAY` | valeur / nombre de trajets du jour |
| `EUR_PER_MONTH` | (valeur / jours ouvrés du mois) × (km trajet / km journaliers du véhicule) |
| `EUR_PER_YEAR` | (valeur / jours ouvrés de l'année) / nombre de trajets du jour |

## Logique d'application (`CostParameterAssignmentType`)

### `ALWAYS` — Toujours applicable
- Sélectionné par défaut à la création.
- Le coût actif est appliqué automatiquement aux trajets de sa catégorie, sans condition ni événement.
- Le formulaire masque les conditions, comme pour le mode manuel.
- Passer à ce mode supprime les anciennes conditions du paramètre à l'enregistrement.

### `ALL_CONDITIONS`
- Le paramètre est évalué automatiquement pour les trajets de sa catégorie.
- S'il n'a pas de condition, il est toujours compté.
- S'il a des conditions, elles doivent toutes être satisfaites.
- Reste soumis à `active = true`.

### `ANY_CONDITION`
- Le paramètre est évalué automatiquement pour les trajets de sa catégorie.
- S'il n'a pas de condition, il est toujours compté.
- S'il a des conditions, au moins une doit être satisfaite.

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
- Les tiroirs de détail du tracteur et de la semi-remorque affichent en lecture seule les
  événements qui leur sont assignés (`GET /api/events/tractors/{id}` et
  `GET /api/events/semi-trailers/{id}`), du plus récent au plus ancien.

### Résumé

| Logique | Application automatique | Conditions |
|---|---|---|
| `ALWAYS` | Oui | Aucune |
| `ALL_CONDITIONS` | Oui | Toutes doivent être satisfaites |
| `ANY_CONDITION` | Oui | Au moins une doit être satisfaite |
| `MANUAL` | Non, événement obligatoire | Non utilisées |

## Implémentation

- `CostParameterEngine.calculateCosts` ignore les paramètres `MANUAL` et vérifie directement les conditions associées au paramètre.
- `TransportCostService.calculate` ajoute, pour chaque événement du trajet, le coût du paramètre `MANUAL` associé (`CostParameterEngine.calculateManualCost`) dans la catégorie du paramètre. Un même paramètre assigné à deux événements est compté deux fois.

## Mise à niveau de la base existante

La mise à jour automatique d'Hibernate ne supprime pas les anciennes tables/colonnes et ne peut pas transférer les conditions. Avant de lancer la nouvelle version sur une base existante, exécuter une fois [`2026-11-04-simplify-cost-rules.sql`](../backend/src/main/resources/db/manual/2026-11-04-simplify-cost-rules.sql). La migration conserve les conditions et convertit l'opérateur des paramètres automatiques en `ALL_CONDITIONS` ou `ANY_CONDITION`. Elle s'arrête si un paramètre possède plusieurs anciennes règles, car leur combinaison ne peut pas toujours être représentée par une seule logique.

Exécuter le script entier : il est transactionnel et refuse les conditions orphelines avant toute modification. Ces références doivent être réparées ou leur suppression explicitement décidée avant la migration. Si une ancienne version du script a déjà été exécutée partiellement sans transaction, ne pas relancer le script entier sans vérifier les colonnes et données déjà migrées.

Sur une base déjà migrée vers les trois modes, exécuter uniquement
[`2026-11-04-add-always-cost-assignment.sql`](../backend/src/main/resources/db/manual/2026-11-04-add-always-cost-assignment.sql)
pour autoriser `ALWAYS` et en faire la valeur par défaut SQL. Les paramètres existants conservent leur logique.

### Réparation d'une migration partielle (uniquement si ces contraintes sont absentes)

```sql
BEGIN;

ALTER TABLE cost_parameter
    DROP CONSTRAINT ck_cost_parameter_assignment_type;

ALTER TABLE cost_parameter
    ALTER COLUMN assignment_type SET DEFAULT 'ALWAYS';

ALTER TABLE cost_parameter
    ADD CONSTRAINT ck_cost_parameter_assignment_type
    CHECK (assignment_type IN (
        'ALWAYS', 'ALL_CONDITIONS', 'ANY_CONDITION', 'MANUAL'
    ));

ALTER TABLE cost_condition
    ADD CONSTRAINT fk_cost_condition_parameter
    FOREIGN KEY (cost_parameter_id) REFERENCES cost_parameter(id);

COMMIT;
```
