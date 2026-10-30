# Synchronisation Dashdoc : services et trajets

> Document de cadrage et guide de test. L'intégration Dashdoc de bout en bout et la création récursive des trajets ne sont pas encore implémentées. Les exemples ci-dessous simulent une entrée Dashdoc en appelant les API OptiRoute existantes.

## 1. Modèle métier

- Un **service** représente la mission Dashdoc : client, chiffre d'affaires, identifiant/source externes, fenêtres horaires d'origine et durées de chargement/déchargement.
- Un **trajet (`Route`)** représente une portion opérationnelle calculée de cette mission. Un service peut avoir plusieurs trajets, notamment après découpage sur plusieurs journées.
- Le service conserve les **fenêtres sources**. Un trajet conserve ses propres horaires calculés, sa géométrie, ses kilomètres, sa durée, ses coûts et ses waypoints.
- Les fenêtres horaires ne sont pas des heures fixes de trajet : elles expriment les limites autorisées sur les opérations logistiques.

Les champs horaires actuellement nommés sur le service sont :

| Champ du service | Sens Dashdoc |
|---|---|
| `departureStart` / `departureEnd` | Début / fin du créneau du point de chargement (`origin.slots`) |
| `arrivalStart` / `arrivalEnd` | Début / fin du créneau du point de livraison (`destination.slots`) |

`Route` possède lui aussi `departureStart`, `departureEnd`, `arrivalStart`, `arrivalEnd`, mais ces bornes sont **calculées** : l'utilisateur ne les saisit pas.

## 2. Données Dashdoc à importer

Dans l'exemple de réponse disponible, chaque élément de `deliveries[]` peut contenir :

- `origin.address` et `destination.address` avec nom, adresse et coordonnées ;
- `origin.slots[]` et `destination.slots[]` ;
- chaque slot contient `start` et `end` sous forme de date/heure ISO ;
- `uid` et/ou `remote_id` pour identifier la ressource externe.

Exemple de structure (les valeurs d'adresse et les dates de l'exemple ne sont pas des données de test métier) :

```json
{
  "uid": "dashdoc-service-uid",
  "remote_id": "reference-externe",
  "deliveries": [
    {
      "origin": {
        "address": {
          "name": "Entrepôt de chargement",
          "address": "10 rue de l'Entrepôt",
          "city": "Lyon",
          "latitude": 45.764,
          "longitude": 4.8357
        },
        "slots": [
          {
            "start": "2026-11-03T07:00:00+01:00",
            "end": "2026-11-03T09:00:00+01:00"
          }
        ]
      },
      "destination": {
        "address": {
          "name": "Site de livraison",
          "address": "20 avenue du Port",
          "city": "Paris",
          "latitude": 48.8566,
          "longitude": 2.3522
        },
        "slots": [
          {
            "start": "2026-11-03T15:00:00+01:00",
            "end": "2026-11-03T18:00:00+01:00"
          }
        ]
      }
    }
  ]
}
```

### Mapping et conservation des créneaux

1. Importer les dates ISO en conservant leur instant et leur fuseau/offset ; ne pas les réduire à une heure sans date.
2. Stocker sur le service le début et la fin du créneau de chargement et du créneau de livraison.
3. Utiliser le début de fenêtre de livraison (`arrivalStart`) comme heure de référence pour calculer l'itinéraire, en mode **« Arriver à »**.
4. Conserver l'association entre chaque livraison et ses points logistiques. Ne pas joindre des créneaux disjoints en une plage plus large qui rendrait valides des heures non autorisées.
5. Si un service comporte plusieurs points ou plusieurs créneaux possibles, il faut préserver cette structure. Les quatre bornes scalaires du modèle actuel ne suffisent pas à représenter sans perte tous les cas multi-livraisons / multi-créneaux : ne pas choisir arbitrairement un créneau ou écraser les autres sans règle métier définie.

L'identifiant externe doit permettre de retrouver le même objet à chaque synchronisation. Choisir un identifiant Dashdoc stable (`uid` ou identifiant équivalent confirmé sur l'API réellement utilisée) et le combiner avec `externalSource = DASHDOC`. Une référence `remote_id` peut être conservée comme référence métier si elle est distincte.

## 3. Règles d'import du service et des trajets

### 3.1 Création et mise à jour sans doublon

Comportement cible pour chaque message ou import Dashdoc :

1. Normaliser le payload Dashdoc vers les données internes de service, points logistiques, fenêtres et affectation.
2. Rechercher le service par la clé externe `(externalSource, externalId)`.
3. Si absent, créer le service et ses trajets.
4. Si présent, mettre à jour ce service et réconcilier ses trajets, sans créer de doublons lors d'une livraison répétée du même message.
5. Effectuer l'écriture du service et de ses trajets comme une unité cohérente. En cas d'échec du calcul ou de sauvegarde d'un trajet, ne pas laisser apparaître une mission partiellement synchronisée comme valide.

L'intégration doit définir explicitement ce qui arrive aux trajets modifiés manuellement, commencés, terminés ou annulés lorsque Dashdoc renvoie une nouvelle version. Une synchronisation ne doit pas effacer silencieusement une modification utilisateur ou un état opérationnel.

**État actuel :** `POST /api/transports` crée un nouveau service ; ce n'est pas encore un endpoint d'upsert Dashdoc. Répéter cet appel avec le même `externalId` peut donc créer un doublon. Il n'existe pas encore d'endpoint dédié du type `/api/dashdoc/sync`.

### 3.2 Durées de manutention

Le service conserve :

- `loadingDurationMinutes` ;
- `unloadingDurationMinutes`.

Valeur initiale prévue pour les imports Dashdoc : **45 minutes pour le chargement et 45 minutes pour le déchargement**.

Ces champs sont renseignables manuellement dans OptiRoute. Lors d'une mise à jour Dashdoc, préserver une durée corrigée manuellement ; appliquer 45 minutes à la création ou lorsque la durée est absente, pas pour écraser une valeur déjà personnalisée. Le modèle actuel ne porte pas encore de marqueur de provenance de ces durées : la stratégie « initialiser si null » est le minimum pour éviter leur écrasement.

Une durée s'applique à l'opération réelle de chargement ou de déchargement. Un point d'arrêt nocturne ne constitue pas un nouveau chargement/déchargement et ne duplique pas la durée. Si le conducteur est déjà chargé, le chargement restant à effectuer vaut zéro.

### 3.3 Calcul des horaires de trajet

Les quatre bornes calculées d'un trajet sont :

- `departureStart` : départ minimal ;
- `departureEnd` : départ maximal ;
- `arrivalStart` : arrivée minimale ;
- `arrivalEnd` : arrivée maximale.

Pour le calcul de l'itinéraire et de ses informations, prendre comme référence l'**heure minimale d'arrivée**, en mode « Arriver à ». En import, les quatre fenêtres du service sont disponibles comme données sources et alimentent les bornes correspondantes du trajet initial. Les bornes de route ne sont pas des champs à saisir dans l'interface.

Pour une création manuelle, seule l'heure de recherche et le calcul de durée déterminent les bornes minimales ; les bornes maximales restent nulles. Pour Dashdoc, la synchronisation doit distinguer les fenêtres sources enregistrées sur le service des horaires effectivement calculés pour chacun des trajets.

Les heures calculées doivent utiliser le fuseau métier `Europe/Paris` pour les règles locales, notamment la coupure de 20 h. Les dates sources Dashdoc restent des instants ISO non ambigus.

### 3.4 Découpage sur plusieurs jours

La synchronisation doit pouvoir produire **plusieurs trajets rattachés au même service**. Appliquer les règles récursivement, quel que soit le nombre de journées.

- Si un déplacement doit continuer après 20 h, couper à la position estimée du camion à **20 h**.
- Pour un grand routier (`LONG_HAUL`), estimer cette position à partir du temps de parcours cumulé sur le tracé ; ne pas utiliser une simple proportion des kilomètres. Il s'agit d'une position théorique, sans recherche d'aire de stationnement.
- Pour un grand régional (`SHORT_DISTANCE`), utiliser le stationnement si le trajet passe à moins de 50 km et si la livraison reste faisable depuis ce stationnement selon les contraintes horaires. Sinon, appliquer la coupure estimée à 20 h. Mesurer la proximité à vol d'oiseau du stationnement au tracé, ou utiliser le détour routier si c'est plus simple avec PTV/HERE ; le choix retenu doit être constant.
- Pour un régional (`REGIONAL`), utiliser le stationnement comme point d'arrêt.
- Pour les trajets de retour au stationnement retenus par ces règles, aucune heure obligatoire d'arrivée n'est imposée : le retour peut dépasser 20 h.
- La coupure à 20 h ne crée pas une livraison ni un déchargement.
- Les temps de pause ne sont pas inclus dans ce calcul ; ils sont gérés séparément.

Le départ est calculé ainsi :

```text
départ = heure maximale de livraison
         - durée de chargement restant à effectuer
         - durée de l'itinéraire
```

Cette formule remplace la règle de départ à 05 h. Si le véhicule est déjà chargé, ne pas déduire une nouvelle fois la durée de chargement.

Les origines/destinations successives doivent se rejoindre exactement. Les détours vers le stationnement sont de vrais tronçons routiers et contribuent aux distances et coûts.

### 3.5 Coûts et chiffre d'affaires

- Calculer normalement les coûts de chaque trajet à partir de sa distance, sa durée, ses péages/carburant et des règles de coûts existantes.
- Ne pas ajouter de coût spécial uniquement parce que la mission est découpée.
- Répartir le chiffre d'affaires du service au prorata des kilomètres de chaque trajet :

```text
CA trajet i = CA du service × distance du trajet i
                             / somme des distances de tous les trajets
```

- Après arrondi, garantir que la somme des CA des trajets égale exactement le CA du service.
- Ne jamais répéter la totalité du CA sur chaque trajet.
- La politique d'inclusion des trajets d'approche à vide dans la répartition doit être explicitée avant sa mise en œuvre.

## 4. API OptiRoute disponible pour tester

Les API métiers nécessitent un jeton Bearer. Authentification via `POST /api/auth/login`, avec un compte existant. Les identifiants conducteur et tracteur du payload doivent correspondre à des enregistrements existants (`GET /api/drivers` et `GET /api/tractors`).

### 4.1 Obtenir un jeton (PowerShell)

Remplacer l'adresse et le mot de passe par ceux d'un compte de développement autorisé :

```powershell
$baseUrl = "http://localhost:8080"
$loginBody = @{
    email = "COMPTE_DE_TEST"
    password = "MOT_DE_PASSE_DE_TEST"
} | ConvertTo-Json

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$headers = @{ Authorization = [string]::Concat('Bearer', ' ', $login.token) }
```

### 4.2 (Optionnel) Vérifier le calcul seul

`POST /api/routes/calculate` calcule et retourne des propositions d'itinéraire sans créer de service. Utiliser l'heure minimale de livraison comme `routeTime` et `timeMode: ARRIVAL`.

```powershell
$routeBody = @'
{
  "origin": {
    "name": "Entrepôt de chargement",
    "address": "10 rue de l'Entrepôt, Lyon",
    "lat": 45.7640,
    "lng": 4.8357
  },
  "destination": {
    "name": "Site de livraison",
    "address": "20 avenue du Port, Paris",
    "lat": 48.8566,
    "lng": 2.3522
  },
  "waypoints": [],
  "mode": "FASTEST",
  "routeTime": "2026-11-03T15:00:00+01:00",
  "timeMode": "ARRIVAL",
  "driverId": 1,
  "tractorId": 1,
  "emptyTrip": false
}
'@

$routes = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/routes/calculate" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $routeBody

$routes.routes
```

Remplacer les coordonnées et les IDs par des données de test cohérentes. `waypoints` est une liste ordonnée de `{ "name", "address", "lat", "lng" }`. La réponse contient notamment `duration`, `distanceMeters`, `polyline`, `type` et les coûts.

### 4.3 Créer un service et un trajet Dashdoc simulés

`POST /api/transports` appelle le calcul d'itinéraire puis crée un service et **un** trajet. Le code de création sélectionne la route la plus rapide. Il ne fait pas encore l'upsert externe, les valeurs par défaut de 45 minutes, ni le découpage multi-jours.

Le corps ci-dessous illustre le contrat OptiRoute actuel. Il reprend les champs de fenêtres qui seraient normalisés depuis Dashdoc et fournit explicitement les deux durées, car leur défaut d'import n'est pas encore câblé :

```powershell
$transportBody = @'
{
  "name": "Test Dashdoc - Lyon vers Paris",
  "externalId": "dashdoc-test-uid-20261103-001",
  "externalSource": "DASHDOC",
  "driverId": 1,
  "tractorId": 1,
  "semiTrailerId": null,
  "customerId": null,
  "emptyTrip": false,
  "routeTime": "2026-11-03T15:00:00+01:00",
  "timeMode": "ARRIVAL",
  "originName": "Entrepôt de chargement",
  "originAddress": "10 rue de l'Entrepôt, Lyon",
  "originLat": 45.7640,
  "originLng": 4.8357,
  "destinationName": "Site de livraison",
  "destinationAddress": "20 avenue du Port, Paris",
  "destinationLat": 48.8566,
  "destinationLng": 2.3522,
  "revenue": 850.00,
  "serviceTiming": {
    "loadingDurationMinutes": 45,
    "unloadingDurationMinutes": 45,
    "departureStart": "2026-11-03T07:00:00+01:00",
    "departureEnd": "2026-11-03T09:00:00+01:00",
    "arrivalStart": "2026-11-03T15:00:00+01:00",
    "arrivalEnd": "2026-11-03T18:00:00+01:00"
  },
  "waypoints": []
}
'@

$created = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/transports" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $transportBody

$created
```

Ici, `serviceTiming.departure*` représente le créneau du chargement et `serviceTiming.arrival*` celui de la livraison. Le backend utilise `serviceTiming.arrivalStart` comme heure de calcul en mode « Arriver à ».

> **Important :** ce test crée réellement des données. Utiliser un environnement de développement et un `externalId` de test inédit. Répéter le POST ne vérifie pas l'idempotence : l'API actuelle créera un autre service.

### 4.4 Vérifier les données enregistrées

Lire le détail avec l'ID renvoyé par la création :

```powershell
$serviceId = $created.id

Invoke-RestMethod `
    -Method Get `
    -Uri "$baseUrl/api/transports/$serviceId" `
    -Headers $headers
```

Vérifier dans la réponse :

- `serviceTiming.loadingDurationMinutes` et `unloadingDurationMinutes` ;
- les quatre bornes sources de `serviceTiming` ;
- `departureStart`, `departureEnd`, `arrivalStart`, `arrivalEnd` du trajet ;
- `waypoints`, la distance, la durée et les coûts.

Le planning se vérifie également avec :

```powershell
Invoke-RestMethod `
    -Method Get `
    -Uri "$baseUrl/api/transports/planning?startDate=2026-11-03&endDate=2026-11-05" `
    -Headers $headers
```

`endDate` est une borne exclusive dans le service de planning. Les dates minimales des trajets déterminent le jour affiché.

## 5. Tests d'acceptation de la future synchronisation

1. Un service Dashdoc nouveau est créé avec `externalSource = DASHDOC` et une clé externe stable.
2. Le service conserve les fenêtres chargement/livraison sources et les durées initialisées à 45 minutes lorsqu'elles sont absentes.
3. La route initiale est calculée en mode « Arriver à » sur la borne basse d'arrivée.
4. Les quatre bornes de source alimentent les bornes de la route initiale ; les routes manuelles n'ont pas de bornes maximales inventées.
5. Un service sur plusieurs journées produit plusieurs `Route` ordonnées, rattachées au même service, avec des waypoints persistés et réutilisés au recalcul.
6. La coupure sur route est à la position estimée à 20 h ; un retour au stationnement peut dépasser cette heure.
7. La règle du conducteur, la formule de départ sans seuil de 05 h, et l'exclusion des pauses sont appliquées.
8. Les coûts sont calculés par trajet et les revenus répartis au prorata des kilomètres ; leur somme est égale au CA source.
9. Réimporter deux fois le même événement Dashdoc met à jour le même service et ne duplique ni service ni trajets.
10. Une mise à jour des fenêtres ou points logistiques recalcule les trajets concernés sans écraser une durée de manutention personnalisée ni les trajets protégés par la politique de synchronisation.
11. Une erreur de routage/sauvegarde est remontée et n'enregistre pas une synchronisation partielle.

Les scénarios 5 à 11 ne sont pas couverts par le simple POST de test ci-dessus ; ils nécessitent l'implémentation du service d'import et de la logique multi-trajets.

## 6. Éléments à finaliser pour l'intégration réelle

- Contrat exact de l'API Dashdoc utilisée et choix de la clé externe stable.
- Politique de sélection ou représentation de plusieurs `deliveries` et `slots`.
- Correspondance entre livraison Dashdoc, client, conducteur, véhicule, stationnement et état déjà chargé.
- Valeur par défaut 45 minutes appliquée à l'import et distinction entre valeur importée et correction manuelle.
- Upsert idempotent et politique d'écrasement des trajets modifiés, commencés, terminés ou annulés.
- Calcul de la position à 20 h à partir des données de segments disponibles auprès de PTV/HERE.
- Découpage récursif, création de plusieurs routes, calcul des plages propres à chaque portion, coûts et répartition du CA.
- Représentation des horaires ou opérations multiples si le contrat Dashdoc comporte plusieurs points logistiques.
