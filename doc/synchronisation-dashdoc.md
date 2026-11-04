# Synchronisation Dashdoc : services et trajets

> Document de cadrage et guide de test. L'intégration Dashdoc de bout en bout reste à implémenter. Le découpage récurrent à 20 h avec HERE est disponible, même sans fenêtres sources. Pas de conduite jusqu'à 5 h par défaut ; un départ plus tôt est autorisé si nécessaire pour respecter la livraison. Les régionaux passent par leur stationnement et peuvent y arriver après 20 h. Les exemples ci-dessous simulent une entrée Dashdoc avec les API OptiRoute existantes.

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

Pour le calcul initial en simulation/import, prendre comme référence le **départ minimal**, en mode « Partir à ». Sans départ minimal, utiliser l'arrivée minimale, sinon l'heure de recherche. Le chargement renseigné s'ajoute au départ minimal pour déterminer le début de conduite. La planification nocturne recalcule ensuite chaque portion à son horaire effectif. Les fenêtres sources restent sur le service ; les bornes de route ne sont pas des champs à saisir dans l'interface.

Pour une création manuelle, seule l'heure de recherche et le calcul de durée déterminent les bornes minimales ; les bornes maximales restent nulles. Pour Dashdoc, la synchronisation doit distinguer les fenêtres sources enregistrées sur le service des horaires effectivement calculés pour chacun des trajets.

Les heures calculées doivent utiliser le fuseau métier `Europe/Paris` pour les règles locales, notamment la coupure de 20 h. Les dates sources Dashdoc restent des instants ISO non ambigus.

### 3.4 Découpage sur plusieurs jours

La synchronisation doit pouvoir produire **plusieurs trajets rattachés au même service**. Appliquer les règles récursivement, quel que soit le nombre de journées.

- Si un déplacement doit continuer après 20 h, couper à la position estimée du camion à **20 h**.
- Pour un grand routier (`LONG_HAUL`), estimer cette position à partir du temps de parcours cumulé sur le tracé ; ne pas utiliser une simple proportion des kilomètres. Il s'agit d'une position théorique, sans recherche d'aire de stationnement.
- Pour un grand régional (`SHORT_DISTANCE`), utiliser le stationnement si sa distance minimale à vol d'oiseau aux segments du tracé est strictement inférieure à 50 km et si la livraison reste faisable le lendemain en repartant à partir de 5 h, déchargement compris. La proximité est calculée localement sur la géométrie HERE, puis deux calculs routiers vérifient le détour réel et les horaires. Sinon, appliquer la coupure sur route à 20 h ; l'exception de reprise avant 5 h reste possible uniquement dans ce mode Grand routier.
- Pour un régional (`REGIONAL`), si la livraison est prévue le lendemain du départ, créer obligatoirement origine → stationnement puis stationnement → livraison, même si le trajet direct pourrait être terminé le jour du départ.
- Pour les trajets de retour au stationnement retenus par ces règles, aucune heure obligatoire d'arrivée n'est imposée : le retour peut dépasser 20 h.
- La coupure à 20 h ne crée pas une livraison ni un déchargement.
- Les temps de pause ne sont pas inclus dans ce calcul ; ils sont gérés séparément.

Pour la reprise, viser par défaut l'arrivée minimale, en tenant compte du déchargement : cible routière = `arrivalStart - unloadingDurationMinutes`. Si cela impose un départ avant 05 h, essayer un départ à 05 h et une arrivée plus tardive dans la fenêtre. Si la borne maximale ne le permet pas, reprendre exceptionnellement avant 05 h en visant la borne maximale, au plus tôt à minuit le lendemain de la coupure. Le grand régional n'utilise son stationnement que si la reprise reste possible à partir de 05 h ; sinon, ses règles de coupure sur route restent applicables.

Sans arrivée minimale, conserver la borne maximale comme cible. Sans échéance, reprise à 05 h. Le chargement n'est jamais déduit une seconde fois après une nuit. Le mode Manuel, sans fenêtres sources, conserve l'heure et le mode choisis dans le formulaire.

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

Dans le modal de création/modification, deux modes sont proposés et affichés séparément :

- **Manuel** : départ, arrivée, points de passage et choix d'une heure de départ ou d'arrivée.
- **Simulation Dashdoc** : ses propres lieux et points de passage, plus les quatre fenêtres horaires du service. Le calcul utilise le départ minimal s'il est renseigné, sinon l'arrivée minimale, sinon un départ maintenant. Les fenêtres restent facultatives.

Le changement de mode conserve les saisies et résultats de chacun pendant l'ouverture du modal. L'affectation, les durées de chargement/déchargement et le revenu restent communs. Seul le mode actif est enregistré ; en mode Manuel, les fenêtres de simulation ne sont pas envoyées. Après une modification des lieux ou créneaux, recalculer avant de valider. La simulation utilise les API de création existantes : elle ne lance pas une synchronisation réelle avec Dashdoc.

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

`POST /api/transports` calcule l'itinéraire puis planifie la mission. Pour `REGIONAL`, une mission nécessitant une nuit passe par le stationnement. Pour `SHORT_DISTANCE`, ce choix dépend des deux conditions ci-dessus. Les autres cas suivent la coupure à 20 h estimée par HERE, autant de nuits que nécessaire, même sans `serviceTiming`. L'upsert externe et les valeurs par défaut de 45 minutes restent à implémenter.

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

### 4.5 Tester un Grand routier avec coupure à 20 h (HERE)

Configurer `routing.provider=here` et une clé HERE valide. Réutiliser `$baseUrl` et `$headers` de la section 4.1. Remplacer `driverId` par un conducteur existant de type `LONG_HAUL`, ainsi que les identifiants de véhicule. La date de livraison doit être le lendemain de celle du chargement en `Europe/Paris`.

```powershell
$longHaulBody = @'
{
  "name": "Test Grand routier - Lille vers Nice",
  "externalId": "dashdoc-test-long-haul-20261103-001",
  "externalSource": "DASHDOC",
  "driverId": 1,
  "tractorId": 1,
  "emptyTrip": false,
  "routeTime": "2026-11-03T16:00:00+01:00",
  "timeMode": "DEPARTURE",
  "originName": "Lille",
  "originLat": 50.6292,
  "originLng": 3.0573,
  "destinationName": "Nice",
  "destinationLat": 43.7102,
  "destinationLng": 7.2620,
  "revenue": 1500.00,
  "serviceTiming": {
    "loadingDurationMinutes": 45,
    "unloadingDurationMinutes": 45,
    "departureStart": "2026-11-03T16:00:00+01:00",
    "departureEnd": "2026-11-03T18:00:00+01:00",
    "arrivalStart": "2026-11-04T13:00:00+01:00",
    "arrivalEnd": "2026-11-04T16:00:00+01:00"
  },
  "waypoints": []
}
'@

$created = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/transports" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $longHaulBody

$detail = Invoke-RestMethod `
    -Method Get `
    -Uri "$baseUrl/api/transports/$($created.id)" `
    -Headers $headers

$detail.routes
```

Le calcul de progression part ici à 16 h 45, après chargement. Les horaires réels dépendent de la réponse HERE ; ce payload n'est pas un résultat de routage garanti.

Vérifier :

1. Un service, deux trajets : le premier arrive le 3 novembre à `20:00:00+01:00`, le second part le 4 novembre.
2. Destination du premier et origine du second ont les mêmes coordonnées.
3. Le second arrive à 15 h 15, avant les 45 minutes de déchargement. Son départ vaut 15 h 15 moins sa durée routière, sans nouveau chargement.
4. Les waypoints ajoutés au test apparaissent sur la portion correspondante, dans l'ordre initial.
5. Chaque portion possède sa géométrie, sa durée, sa distance et ses coûts. Le CA est réparti par distance, sans duplication.
6. La création depuis un itinéraire (`POST /api/transports/from-route`) et sa modification (`PUT /api/transports/{id}/from-route`) appliquent la même règle.

Cas limites :

- Trajet achevé avant 20 h ou exactement à 20 h : pas de portion fictive. Départ demandé à partir de 20 h : report au lendemain à 5 h.
- Livraison à J+2 et au-delà : répéter la coupure chaque soir à 20 h si nécessaire. Sans `serviceTiming`, un trajet de 10 h partant à 15 h est découpé en 15 h–20 h puis 5 h–10 h.
- Avec une livraison à 12 h le lendemain, ce même trajet reprend à 7 h ; avec une livraison à 9 h, il reprend exceptionnellement à 4 h. Le chargement est supposé terminé à 15 h dans ces exemples.
- Départ calculé du deuxième trajet sur la veille : HTTP `422`, code `TRANSPORT_PLANNING_ERROR`, et aucune sauvegarde partielle.
- Le recalcul du premier trajet exige un départ avant la fin du chargement : même erreur de planning, pas de décalage silencieux.
- `routing.provider=ptv` pour un cas nécessitant la chronologie : HTTP `422` indiquant que HERE est requis.
- Durée de chargement/déchargement absente : zéro pour le calcul, sans appliquer automatiquement les 45 minutes prévues pour le futur import.

Le point à 20 h reste une estimation théorique sur la route, sans validation d'un stationnement réel. Le découpage répète les nuits à l'arrêt ; il n'ajoute pas les pauses réglementaires.

Tests automatisés ciblés, sans requête réelle au fournisseur ni base de données :

```powershell
Set-Location backend
.\mvnw.cmd "-Dtest=HereApiClientTest,RouteHereParserTest,RoutingServiceTest,FlexiblePolylineDecoderTest,RouteGeometryTest,RouteTimelineTest,TransportRoutePlannerTest,TransportFacadeServiceTest,ServiceRouteServiceTest,GlobalExceptionHandlerTest" test
```

### 4.6 Tester le Grand régional et le choix du stationnement

Réutiliser le payload `$longHaulBody` de la section 4.5 avec un conducteur existant de type `SHORT_DISTANCE`, possédant un stationnement à proximité du tracé Lille–Nice. Choisir les coordonnées à partir du tracé retourné par `POST /api/routes/calculate`, pas de la ligne droite Lille–Nice.

```powershell
$shortDistance = $longHaulBody | ConvertFrom-Json
$shortDistance.driverId = 2 # Remplacer par l'ID du Grand regional de test.
$shortDistance.name = "Test Grand regional - stationnement"
$shortDistance.externalId = "dashdoc-test-short-distance-20261103-001"

$created = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/transports" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body ($shortDistance | ConvertTo-Json -Depth 10)

$detail = Invoke-RestMethod `
    -Method Get `
    -Uri "$baseUrl/api/transports/$($created.id)" `
    -Headers $headers

$detail.routes
```

Le conducteur et son stationnement doivent être configurés via les écrans/API existants avant ce test. Chaque POST crée un nouveau service ; utiliser des identifiants externes de test distincts.

Vérifications :

| Cas | Résultat attendu |
|---|---|
| Stationnement à moins de 50 km et livraison faisable le lendemain à partir de 5 h | Destination du premier trajet et origine du second = stationnement ; départ du second à partir de 5 h |
| Reprise calculée à 5 h exactement | Stationnement accepté |
| Stationnement à 50 km exactement ou au-delà | Coupure sur route à 20 h |
| Proche à vol d'oiseau, mais détour routier imposant une reprise avant 5 h | Stationnement refusé, règle Grand routier conservée |
| Retour au stationnement trop long pour permettre la reprise calculée | Stationnement refusé, pas de chevauchement entre portions |
| Coordonnées de stationnement absentes | Règle Grand routier |
| Livraison sur plusieurs journées | Décision réévaluée à chaque nuit ; stationnement retenu seulement si la livraison devient réalisable le lendemain |

Le retour au stationnement retenu peut dépasser 20 h. Les waypoints sont répartis selon la projection du stationnement sur le tracé et restent ordonnés ; les coûts et le CA portent uniquement sur les trajets finalement enregistrés. Pour les seuils précis, les tests automatisés contrôlent 49 999 m, 50 000 m et 50 001 m sans dépendre d'une réponse HERE réelle.

## 5. Tests d'acceptation de la future synchronisation

1. Un service Dashdoc nouveau est créé avec `externalSource = DASHDOC` et une clé externe stable.
2. Le service conserve les fenêtres chargement/livraison sources et les durées initialisées à 45 minutes lorsqu'elles sont absentes.
3. La route initiale est calculée en mode « Arriver à » sur la borne basse d'arrivée.
4. Les bornes sources sont conservées sur le service ; les horaires de chaque portion sont calculés, sans bornes maximales inventées.
5. Un service sur plusieurs journées produit plusieurs `Route` ordonnées, rattachées au même service, avec des waypoints persistés et réutilisés au recalcul.
6. La coupure sur route est à la position estimée à 20 h ; un retour au stationnement peut dépasser cette heure.
7. La règle du conducteur, la coupure 20 h–05 h avec départ anticipé seulement si nécessaire, et l'exclusion des pauses sont appliquées.
8. Les coûts sont calculés par trajet et les revenus répartis au prorata des kilomètres ; leur somme est égale au CA source.
9. Réimporter deux fois le même événement Dashdoc met à jour le même service et ne duplique ni service ni trajets.
10. Une mise à jour des fenêtres ou points logistiques recalcule les trajets concernés sans écraser une durée de manutention personnalisée ni les trajets protégés par la politique de synchronisation.
11. Une erreur de routage/sauvegarde est remontée et n'enregistre pas une synchronisation partielle.

Le POST permet de vérifier les découpages nocturnes, le stationnement des grands régionaux, les coûts et le CA, ainsi que l'absence de sauvegarde partielle en cas de conflit. L'idempotence et les politiques de synchronisation restent à implémenter.

## 6. Éléments à finaliser pour l'intégration réelle

- Contrat exact de l'API Dashdoc utilisée et choix de la clé externe stable.
- Politique de sélection ou représentation de plusieurs `deliveries` et `slots`.
- Correspondance entre livraison Dashdoc, client, conducteur, véhicule, stationnement et état déjà chargé.
- Valeur par défaut 45 minutes appliquée à l'import et distinction entre valeur importée et correction manuelle.
- Upsert idempotent et politique d'écrasement des trajets modifiés, commencés, terminés ou annulés.
- Adaptateur PTV vers la chronologie du tracé (le calcul à 20 h est disponible avec HERE).
- Propagation complète des bornes maximales ; le stationnement grand régional, le découpage nocturne multi-jours, les coûts et la répartition du CA sont disponibles.
- Représentation des horaires ou opérations multiples si le contrat Dashdoc comporte plusieurs points logistiques.
