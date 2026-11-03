# Création et planification des transports sur plusieurs jours

> Spécification fonctionnelle préparatoire. Les points restant à arbitrer sont regroupés en fin de document.

## Étapes implémentées : données, points de passage et coupures nocturnes multi-jours

- `Service` conserve `loadingDurationMinutes`, `unloadingDurationMinutes` et les fenêtres sources `departureStart`, `departureEnd`, `arrivalStart`, `arrivalEnd`.
- `Route` conserve quatre bornes : `departureStart`, `departureEnd`, `arrivalStart`, `arrivalEnd`. Elles ne sont jamais saisies par l'utilisateur. La planification calcule désormais les horaires de chaque trajet, même pour Dashdoc et pour un trajet unique ; les bornes maximales restent nulles. Les fenêtres sources restent sur le service.
- Les champs entité `startDate` / `endDate` sont supprimés. Les bornes minimales départ/arrivée réutilisent les colonnes physiques existantes `start_date` / `end_date` pour préserver les données, et les dates calculées remplacent `plannedStart` / `plannedEnd` dans les contrats API et le planning.
- `Route.waypoints` est une colonne PostgreSQL `jsonb` contenant la liste ordonnée des positions utilisateur (`lat`, `lng`, `name`, `address`), sans les points liés aux événements.
- Les requêtes de création/modification acceptent `waypoints` et un objet `serviceTiming` regroupant les durées et fenêtres sources du service. Elles n'acceptent pas de bornes de route ; celles-ci sont calculées côté serveur et retournées dans les réponses. Les champs `externalId` et `externalSource` permettent de distinguer une création manuelle d'une source Dashdoc.
- Les durées et fenêtres sont optionnelles ; les durées négatives et les fenêtres inversées sont refusées. Aucune valeur de 45 minutes n'est imposée à une saisie manuelle : cette initialisation reste à câbler avec le futur import Dashdoc.
- Le formulaire permet la saisie optionnelle des fenêtres sources du service, mais jamais des bornes calculées de route. Les points utilisateur sont restaurés à la réouverture et conservés lors d'un recalcul lié aux événements ; les points d'événements sont ajoutés après les points utilisateur dans l'ordre des événements.
- En création manuelle ou Dashdoc, les bornes minimales de Route représentent la conduite effectivement planifiée, pas la simple copie des créneaux logistiques.
- Pour un conducteur `REGIONAL`, une mission nécessitant une nuit passe par son stationnement : origine initiale → stationnement (avec les waypoints), puis stationnement → destination (sans waypoints). Le retour peut dépasser 20 h. Une destination inaccessible avant 20 h depuis le stationnement est signalée comme incompatible.
- Les grands régionaux (`SHORT_DISTANCE`) utilisent leur stationnement s'il est à moins de 50 km du tracé et si la livraison reste réalisable le lendemain en repartant à partir de 5 h. Sinon, ils appliquent la coupure sur route à 20 h.
- Les grands routiers, les grands régionaux non éligibles au stationnement et les trajets sans conducteur affecté suivent la coupure sur route à 20 h dès que la conduite doit se poursuivre. Cela s'applique sans fenêtre source et au-delà de J+1, autant de nuits que nécessaire. Les pauses réglementaires restent exclues.
- Le planning expose les trajets séparément et répartit le CA du service au prorata des distances, avec le reliquat d'arrondi sur le dernier trajet. La fiche détail expose chaque segment.
- Le schéma est enrichi par Hibernate avec la configuration existante `spring.jpa.hibernate.ddl-auto=update`. Les nouvelles colonnes sont nullables pour les données existantes ; les anciens points utilisateur non enregistrés ne peuvent pas être reconstruits depuis cette évolution.

La propagation de toutes les bornes maximales et l'import Dashdoc réel restent à implémenter.

### Coupure nocturne : découpage HERE à 20 h implémenté

- La règle est locale à `Europe/Paris`, changements d'heure inclus : pas de conduite entre 20 h et 5 h, sauf reprise avant 5 h indispensable à une livraison et retour au stationnement retenu pour un régional ou grand régional. Le test d'éligibilité au stationnement du grand régional, lui, n'autorise jamais de reprise avant 5 h.
- Le calcul de progression commence à `departureStart + loadingDurationMinutes` lorsque la fenêtre de chargement est renseignée. Sinon, l'heure/mode de recherche est utilisé ; en mode arrivée, le départ est déterminé à rebours en retirant aussi les périodes nocturnes. Une durée de manutention absente vaut zéro sans modifier la valeur source.
- HERE est interrogé avec `return=polyline,summary,tolls` et `spans=duration,length,dynamicSpeedInfo`. `dynamicSpeedInfo` donne une granularité plus fine que les seules durées et distances.
- Les flexible polylines sont décodées par section. Chaque span relie son `offset` au suivant, ou à la fin de la section ; sa longueur n'est pas un nombre de points. Les durées des spans sont ramenées à la durée totale de leur section pour absorber les arrondis.
- La position à 20 h est trouvée par cumul des durées puis interpolation en distance sur la géométrie du span concerné. Aucun ratio global distance/durée ni temps de pause n'est utilisé.
- Une arrivée routière à 20 h exactement ne crée pas de portion vide. Un départ demandé à partir de 20 h est reporté au lendemain à 5 h. Un départ avant 5 h est reporté à 5 h sauf si ce report rend la livraison impossible.
- Le trajet du jour est recalculé en mode arrivée à 20 h. La suite est planifiée à partir de 5 h le lendemain, ou au plus tard dans les créneaux de conduite disponibles pour atteindre la livraison. La cible finale est `arrivalEnd - unloadingDurationMinutes` (ou `arrivalStart` sans borne haute, ou `routeTime` en mode arrivée sans fenêtre). Sans échéance, la reprise est à 5 h. Aucun chargement n'est retranché une nouvelle fois.
- Si la conduite restante ne tient pas entre 5 h et l'échéance, le calcul avance exceptionnellement la reprise avant 5 h, au plus tôt à minuit le lendemain de la coupure. S'il faut repartir avant cette date, le planning est refusé. Toutes les coupures suivantes restent à 20 h.
- Les deux trajets utilisent les mêmes coordonnées d'arrêt. Les waypoints sont répartis selon les sections traversées ; un waypoint situé exactement au point de coupure devient une extrémité, sans être ajouté une seconde fois. Le mode rapide/économique sélectionné est conservé.
- Le recalcul peut modifier les durées. S'il impose de partir avant l'heure disponible, de chevaucher la coupure précédente ou de dépasser la livraison maximale, le planning est refusé sans sauvegarde partielle. Les tronçons restants sont recalculés à leur propre horaire.
- Les conflits métier retournent HTTP `422`, code `TRANSPORT_PLANNING_ERROR`, avec une explication. Une chronologie HERE absente ou invalide provoque une erreur, sans estimation silencieuse de remplacement.
- Le découpage est commun à `POST /api/transports`, `POST /api/transports/from-route` et `PUT /api/transports/{id}/from-route`. Les coûts sont recalculés séparément ; les mécanismes existants répartissent le CA.
- Le point d'arrêt est théorique, pas une aire de stationnement validée. PTV ne fournit pas encore la chronologie normalisée : un découpage demandé avec ce fournisseur est explicitement refusé.

Les paramètres des spans sont décrits dans la [documentation HERE](https://docs.here.com/routing/docs/routing-v8-span). Un exemple de test API figure dans le [guide Dashdoc](./synchronisation-dashdoc.md#45-tester-un-grand-routier-avec-coupure-à-20-h-here).

## 1. Objectif et vocabulaire

Une même mission commerciale peut nécessiter plusieurs trajets répartis sur plusieurs journées.

- **Service** : mission commerciale portant notamment le client, le chiffre d'affaires, les contraintes horaires et les durées de chargement/livraison.
- **Trajet / itinéraire (`Route`)** : déplacement opérationnel daté, rattaché à un service, avec ses plages de départ et d'arrivée, lieux, conducteur, véhicule, kilomètres et coûts.
- **Point d'arrêt de nuit** : lieu clôturant le trajet du jour et servant de départ au trajet du lendemain.

**Principe : une séparation sur plusieurs jours crée plusieurs trajets pour un seul service. Elle ne duplique pas la mission commerciale ni son CA.**

## 2. Modes de création

Deux entrées existent :

| Entrée | Usage | Principe actuel |
|---|---|---|
| `POST /api/transports/from-route` | Création depuis l'interface | Enregistrement de l'itinéraire sélectionné |
| `POST /api/transports` | Future importation / synchronisation automatique Dashdoc | Calcul automatique de l'itinéraire avant enregistrement |

Les règles de contraintes horaires, de séparation journalière, de coûts et de CA doivent être communes aux deux entrées.

Pour une création depuis l'interface, le traitement d'un éventuel découpage doit respecter le choix utilisateur ; toute modification nécessaire de l'itinéraire doit être visible.

## 3. Données horaires à conserver sur le service

### 3.1 Fenêtres de chargement et de livraison

Le service doit conserver les contraintes sources, indépendamment des horaires calculés de ses trajets :

| Donnée | Signification |
|---|---|
| Début de fenêtre de chargement | Première date/heure autorisée pour le chargement |
| Fin de fenêtre de chargement | Dernière date/heure autorisée pour le chargement |
| Début de fenêtre de livraison | Première date/heure autorisée pour la livraison |
| Fin de fenêtre de livraison | Dernière date/heure autorisée pour la livraison |

Ces valeurs doivent être des **dates et heures complètes**, pas uniquement des heures.

Les plages horaires sont importées depuis Dashdoc. Pour une création depuis l'interface, elles sont renseignées dans des champs manuels **optionnels**. Une borne absente ne doit pas être inventée ; si elle est indispensable à un calcul, l'impossibilité de réaliser ce calcul doit être signalée.

Les horaires calculés des trajets ne doivent pas remplacer ces contraintes : elles restent nécessaires pour recalculer la mission ou vérifier un arrêt intermédiaire.

### 3.2 Source Dashdoc

Dans l'[exemple disponible](./dashdocResponse.txt), les créneaux apparaissent dans :

- `deliveries[].origin.slots[]` : fenêtres du point de chargement ;
- `deliveries[].destination.slots[]` : fenêtres du point de livraison ;
- chaque créneau contient `start` et `end`.

La correspondance exacte avec l'API utilisée devra être confirmée lors de l'intégration.

**Attention : ces champs sont des listes.** En présence de plusieurs livraisons ou créneaux, conserver leur association au point logistique et au service. Ne pas fusionner plusieurs créneaux disjoints en une plage continue artificielle.

Le choix entre quatre champs simples et une collection de contraintes rattachée au service dépendra du périmètre multi-points retenu.

### 3.3 Autres informations nécessaires

La planification doit également disposer de :

- l'ordre des points logistiques ;
- leur localisation ;
- l'affectation du conducteur et son type ;
- son lieu de stationnement ;
- l'état « déjà chargé la veille » ;
- l'identifiant externe et la source du service.

Les durées de chargement, de déchargement et d'attente doivent être distinguées des fenêtres horaires.

### 3.4 Durées de chargement et de déchargement

Deux nouveaux champs distincts doivent être conservés **sur le service, et non sur chaque transport / trajet** :

| Donnée | Saisie manuelle | Valeur par défaut lors d'un import Dashdoc |
|---|---|---|
| Durée de chargement | Renseignée manuellement pour le service | 45 minutes |
| Durée de déchargement | Renseignée manuellement pour le service | 45 minutes |

Les valeurs de 45 minutes sont des valeurs initiales d'import, pas des constantes imposées à tous les services. Une synchronisation ne doit pas écraser une durée ajustée manuellement par une valeur par défaut.

Ces durées interviennent dans l'enchaînement des opérations et la vérification des horaires : le véhicule ne peut repartir d'un point logistique qu'après la fin de l'opération et de l'éventuelle attente.

Un découpage sur plusieurs jours ne duplique pas ces durées : elles s'appliquent aux opérations réelles de chargement et de déchargement, pas aux points d'arrêt de nuit. Si le service est déjà chargé la veille, le chargement ne doit pas être ajouté à nouveau au départ du lendemain.

## 4. Principes de planification

### 4.1 Plages horaires d'un itinéraire

Un itinéraire doit porter et afficher quatre bornes, au lieu d'une seule heure fixe de départ et d'arrivée :

| Donnée | Signification |
|---|---|
| Départ minimal | Première date/heure de départ possible |
| Départ maximal | Dernière date/heure de départ possible |
| Arrivée minimale | Première date/heure d'arrivée possible |
| Arrivée maximale | Dernière date/heure d'arrivée possible |

Chaque minimum doit être inférieur ou égal au maximum correspondant. Un horaire imposé peut être représenté par une plage dont les deux bornes sont égales.

Ces plages opérationnelles ne remplacent pas les fenêtres de chargement/livraison conservées sur le service.

### 4.2 Référence technique du calcul

**Le calcul initial de l'itinéraire se base sur l'heure minimale d'arrivée**, en mode « Arriver à », lorsque cette source est disponible.

La planification nocturne recalcule chaque portion à son propre horaire effectif : départ disponible pour la progression du jour, arrivée à 20 h pour la coupure, puis départ le lendemain ou arrivée finale dans la fenêtre. Une échéance maximale peut donc devenir la cible d'arrivée finale retenue.

Les bornes maximales servent à représenter la flexibilité, à vérifier la faisabilité et à déterminer le départ selon la formule ci-dessous ; elles ne constituent pas une seconde référence de calcul des kilomètres, de la durée ou des autres informations de l'itinéraire.

Pour un déplacement vers un point de chargement, la cible d'arrivée est la borne basse de la fenêtre de chargement. Pour un déplacement vers un point de livraison, elle est la borne basse de la fenêtre de livraison.

Cette référence concerne l'arrivée au point logistique, et non le départ après manutention. Le calcul doit vérifier la compatibilité avec les autres contraintes, dont les heures de départ imposées et les durées de chargement/déchargement.

### 4.3 Cible et limite horaires

- **Borne basse** : horaire d'arrivée à viser si possible.
- **Borne haute** : limite à respecter pour considérer le planning comme réalisable.
- Une arrivée avant la borne basse implique une attente avant l'opération.
- Une arrivée après la borne haute constitue un conflit à signaler.

L'impossibilité de viser la borne basse ne signifie donc pas nécessairement que la mission est impossible : une arrivée plus tardive reste envisageable dans la fenêtre.

Si une arrivée plus tardive est retenue pour le trajet, elle devient son arrivée minimale réalisable et sa référence technique de calcul, sans modifier la fenêtre source du service.

### 4.4 Chronologie, pauses et fuseau horaire

Conserver des instants non ambigus et appliquer les règles locales de planification, notamment **20 h**, dans le fuseau métier. Le planning actuel utilise `Europe/Paris`.

Une fenêtre horaire ne représente pas une durée de manutention. Les durées de chargement/déchargement du service et les attentes aux points logistiques doivent être distinguées de la durée de parcours.

**Les temps de pause ne sont pas pris en compte dans ce calcul et ne seront pas ajoutés par cette logique. Ils seront gérés séparément.** Cette planification ne constitue donc pas une validation réglementaire des temps de conduite et de repos.

Cette exclusion ne supprime pas les règles métier de découpage à 20 h et de retour au stationnement décrites dans ce document.

La plage **20 h–5 h est une indisponibilité métier**, distincte des pauses réglementaires. Elle ne s'ajoute ni à la durée routière ni aux kilomètres ni aux coûts routiers ; elle décale le calendrier des trajets.

### 4.5 Détermination du départ

Pour tous les types de conducteurs, le départ est déterminé par :

```text
Départ = date/heure maximale de livraison
         − durée de chargement restant à effectuer
         − durée de l'itinéraire
```

Cette formule s'applique directement à une portion sans coupure. Pour plusieurs journées, retirer uniquement les temps de conduite dans les créneaux disponibles, en conservant les nuits à l'arrêt ; une simple soustraction de la durée totale ne suffit pas.

Par défaut, pas de conduite de 20 h à 5 h. Le départ retenu est le plus tardif compatible avec la livraison ; il peut donc être après 5 h. Un départ avant 5 h est autorisé seulement si nécessaire pour respecter l'échéance, sans supprimer ni retarder la coupure à 20 h. Sans échéance de livraison, reprendre à 5 h.

Exemple : départ à 15 h, 10 h de conduite, livraison le lendemain à 12 h → première portion 15 h–20 h (5 h), nuit à l'arrêt, seconde portion 7 h–12 h (5 h). Si la livraison est à 9 h, la seconde portion commence exceptionnellement à 4 h.

Si le camion est déjà chargé, la durée de chargement restant à effectuer vaut zéro. Une opération déjà réalisée ne doit pas être déduite à nouveau à chaque reprise.

La durée de l'itinéraire utilisée par la formule provient du calcul basé sur l'arrivée minimale. La borne maximale de livraison sert ici à déterminer le départ, sans remplacer cette référence technique.

Le départ obtenu doit être vérifié contre les plages sources, l'enchaînement des opérations et les coupures nocturnes. La formule ne dispense pas de ces vérifications et ne doit pas conduire à modifier silencieusement les contraintes sources.

## 5. Séparation récursive d'une mission sur plusieurs journées

**Le découpage est déclenché à 20 h dès lors que le déplacement doit se poursuivre au-delà**, et non uniquement lorsqu'une livraison porte la date du lendemain. Un déplacement terminé avant 20 h ne nécessite pas de coupure artificielle.

Exception : lorsqu'un retour au stationnement est retenu selon les règles du conducteur, il n'y a pas d'heure obligatoire d'arrivée au stationnement ; ce retour peut dépasser 20 h.

Pour chaque journée nécessitant une interruption :

1. Déterminer le point d'arrêt selon le type de conducteur.
2. Calculer le trajet du jour jusqu'à ce point.
3. Calculer la suite depuis ce même point, en déterminant le départ selon la section 4.5.
4. Vérifier la faisabilité temporelle de l'ensemble.
5. Si le déplacement restant nécessite encore une coupure, réappliquer récursivement les mêmes règles.
6. Rattacher tous les trajets au même service, calculer leurs coûts et répartir le CA.

La logique s'applique **quel que soit le nombre de jours**, sans limitation à deux étapes. La destination de chaque trajet doit être exactement l'origine du suivant.

L'arrêt de nuit ne constitue ni une livraison ni un déchargement : l'état de chargement est conservé.

Le calcul doit intégrer les éventuels détours vers le stationnement ; il ne suffit pas de couper la géométrie initiale.

## 6. Choix du point d'arrêt

### 6.1 Grand routier — `LONG_HAUL`

- Estimer la progression du conducteur jusqu'à **20 h**.
- Utiliser la position estimée à **20 h exactement** comme point de coupure.
- Terminer le trajet du jour à cet endroit.
- Utiliser ce même lieu comme origine du lendemain.

L'estimation doit utiliser les temps de parcours, et non une simple proportion des kilomètres.

Elle tient compte des opérations et attentes déjà effectuées, mais n'ajoute aucun temps de pause.

La coupure est une **position théorique estimée**, pas la recherche d'une aire de stationnement réelle. Elle ne doit pas être décalée avant ou après 20 h pour rejoindre une aire.

Il faudra déterminer cette position sur le tracé à partir de la progression temporelle : exploiter les temps cumulés des segments fournis par PTV/HERE lorsqu'ils sont disponibles, puis estimer la position dans le segment contenant 20 h. La disponibilité et la précision de ces informations restent à vérifier techniquement ; ne pas assimiler une fraction de temps total à la même fraction de distance totale.

### 6.2 Grand régional — `SHORT_DISTANCE`

Utiliser le lieu de stationnement du conducteur **uniquement si les deux conditions suivantes sont remplies** :

1. Le trajet passe à **moins de 50 km** du stationnement.
2. La livraison reste réalisable **le lendemain en repartant à partir de 5 h**, en respectant la borne haute de livraison, le déchargement et la chronologie de la mission. Une reprise à 5 h exactement est admise ; un départ avant 5 h n'est pas autorisé pour retenir le stationnement.

Cette vérification doit intégrer les déplacements et opérations restant à effectuer.

La proximité implémentée est la **distance minimale à vol d'oiseau entre le stationnement et les segments du tracé**, pas seulement ses sommets. Le calcul local projette le point sur chaque segment de grand cercle sur une sphère de rayon 6 371 000 m, en bornant la projection aux extrémités du segment. Le seuil est strict : `distance < 50 000 m`. La mesure est indépendante du fournisseur et n'utilise pas la distance routière ni le détour.

La géométrie décodée de HERE est réutilisée ; aucun service externe de distance ou de proximité n'est appelé. Pour PTV, ce calcul géographique restera inchangé une fois son adaptateur de géométrie/chronologie disponible.

Lorsque le stationnement est proche :

1. Repérer sa projection la plus proche sur le trajet restant. Répartir les waypoints selon la section correspondante, en conservant chaque waypoint sur l'une des deux portions, y compris si la projection tombe exactement dessus. En cas de passages multiples à la même distance, retenir le premier dans l'ordre du tracé.
2. Calculer réellement le trajet origine du jour → stationnement avec les waypoints précédents.
3. Calculer réellement stationnement → destination avec les waypoints restants. La cible est la borne haute de livraison moins le déchargement (borne basse à défaut), limitée à 20 h le lendemain. Sans échéance source, calculer un départ à 5 h.
4. Accepter uniquement si le départ calculé est à partir de 5 h le lendemain, après l'arrivée au stationnement, et si la livraison routière se termine au plus tard à 20 h et avant l'échéance.

Les durées et coûts retenus sont ceux de ces itinéraires routiers réels, pas ceux de la projection géographique. Une rivière, un accès éloigné ou un détour peuvent donc invalider un stationnement pourtant proche à vol d'oiseau.

Si les deux conditions sont remplies :

- fin du jour au stationnement ;
- reprise le lendemain depuis le stationnement.

Il n'y a pas d'heure obligatoire d'arrivée au stationnement : le retour peut se terminer après 20 h.

Sinon :

- appliquer la règle du grand routier, avec estimation d'un arrêt à 20 h.

La décision est réévaluée sur le trajet restant à chaque nuit ; si la destination nécessite encore plusieurs jours depuis le stationnement, celui-ci n'est pas retenu pour cette nuit. Sans coordonnées de stationnement, appliquer aussi la règle du grand routier. Une réponse de routage invalide ou une panne fournisseur reste une erreur explicite, pas un repli silencieux.

Le test est déclenché si la conduite dépasse 20 h ou si l'échéance de livraison est un jour ultérieur ; il ne crée pas de détour pour un trajet réalisable et livré le même jour. Les portions candidates non retenues ne sont pas enregistrées.

### 6.3 Régional — `REGIONAL`

Utiliser le lieu de stationnement comme point d'arrêt entre les deux journées.

Le retour au stationnement et la reprise du lendemain font partie du calcul réel des trajets.

Il n'y a pas d'heure obligatoire d'arrivée au stationnement : le retour peut dépasser 20 h. Le départ suivant est déterminé par la formule de la section 4.5.

Si cette obligation rend la mission irréalisable, signaler le conflit. Ne pas appliquer automatiquement la règle du grand routier.

### 6.4 Synthèse

| Type de conducteur | Point d'arrêt |
|---|---|
| Grand routier | Position estimée à 20 h exactement |
| Grand régional | Stationnement si distance au tracé < 50 km et livraison réalisable le lendemain avec départ ≥ 5 h ; sinon règle grand routier |
| Régional | Stationnement, sans heure obligatoire de retour |

## 7. Import du planning des conducteurs régionaux

Ces règles définissent les horaires proposés par défaut lors de l'import.

### 7.1 Premier trajet déjà chargé la veille

Créer le premier trajet de la journée :

- **Origine** : lieu de stationnement ;
- **Destination** : point de livraison Dashdoc ;
- **Mode horaire** : « Arriver à » ;
- **Horaire cible** : borne basse de la fenêtre de livraison.

Le départ est déterminé selon la section 4.5, sans retrancher un chargement déjà réalisé. L'itinéraire et ses informations restent calculés sur l'arrivée minimale ; le départ retenu doit être compatible avec les plages d'arrivée.

Le chargement ayant déjà eu lieu, ne pas recréer un déplacement vers le point de chargement.

### 7.2 Premier trajet non chargé la veille

Créer le premier déplacement :

- **Origine** : lieu de stationnement ;
- **Destination** : premier point de chargement Dashdoc ;
- **Départ** : déterminé selon la formule de la section 4.5, en tenant compte du chargement restant et de l'itinéraire nécessaire jusqu'à la livraison ;
- **Référence technique de calcul** : arrivée minimale au premier point de chargement ;
- **État** : trajet à vide jusqu'au chargement.

Le départ est à partir de 05 h par défaut, au plus tard pour respecter la livraison ; il peut être avancé si indispensable. Le déplacement d'approche et la suite de la mission doivent être vérifiés ensemble pour respecter les fenêtres de chargement et de livraison.

Vérifier ensuite sa compatibilité avec la fenêtre de chargement :

- arrivée trop tôt : prévoir l'attente ;
- arrivée dans la fenêtre : poursuivre la planification ;
- arrivée après la borne haute : signaler un conflit.

### 7.3 Suite de la journée

Pour chaque point logistique suivant :

1. Viser une arrivée à la borne basse de sa fenêtre Dashdoc.
2. Vérifier la faisabilité depuis le point précédent.
3. Si cette cible est impossible, vérifier une arrivée plus tardive dans la fenêtre.
4. Signaler tout dépassement de la borne haute.

Intégrer les durées de chargement/déchargement du service aux opérations concernées. Ne pas créer de chevauchement entre les trajets successifs d'un même conducteur.

## 8. Calcul des coûts

Chaque trajet issu du découpage est traité comme un trajet normal :

- kilomètres et durées réellement calculés ;
- carburant et péages propres au trajet ;
- coûts conducteur, véhicule et structure selon les [règles existantes](./cost-calculation.md) ;
- application des répartitions journalières habituelles.

**Aucune formule spéciale ne s'applique au seul motif que la mission chevauche plusieurs jours.**

Les détours vers le stationnement doivent être inclus dans les distances et coûts. Le temps de repos nocturne ne doit pas être ajouté à la durée de conduite.

## 9. Répartition du chiffre d'affaires

Le CA commercial total reste porté par le service.

Pour un nombre quelconque de trajets :

```text
CA trajet i = CA service × distance trajet i
                         / somme des distances de tous les trajets du service
```

Exemple :

```text
CA service : 1 000 €
Trajet 1   :   300 km → 375 €
Trajet 2   :   500 km → 625 €
```

Utiliser les distances des trajets effectivement retenus, y compris les détours de stationnement intégrés au découpage.

Garantir que la somme des montants arrondis est exactement égale au CA du service, en affectant le reliquat d'arrondi au dernier trajet.

Une distance totale nulle ne permet pas cette répartition : le cas doit être signalé et traité explicitement.

**Ne jamais afficher le CA complet du service sur chacun de ses trajets.**

L'inclusion des déplacements d'approche à vide dans cette répartition reste à arbitrer selon leur rattachement au service.

## 10. Cohérence entre création manuelle et synchronisation

Les deux modes de création doivent partager les mêmes règles métier après normalisation des données d'entrée.

Pour Dashdoc :

- identifier chaque service par sa source et son identifiant externe ;
- mettre à jour le service existant lors d'une nouvelle synchronisation ;
- ne pas créer un doublon de service ou de trajets ;
- recalculer les trajets concernés si les contraintes changent ;
- définir une politique explicite pour les trajets modifiés manuellement, commencés ou terminés.

La création ou mise à jour du service et de ses trajets doit être cohérente : aucun résultat partiellement enregistré ne doit être présenté comme un planning valide.

## 11. Impacts identifiés dans l'existant

Le modèle permet à plusieurs `Route` de référencer un même `Service`. Le premier cas multi-trajets est câblé, mais les traitements ne sont pas encore complets :

- la création découpe les missions `REGIONAL` nécessitant une nuit en passant par le stationnement, avec maintien de l'exception de retour après 20 h ;
- les autres types suivent les coupures HERE à 20 h sur autant de nuits que nécessaire, même sans fenêtres de service ;
- la mise à jour remplace ou ajuste les trajets du service selon le découpage recalculé ;
- la fiche détail et le planning exposent les segments, avec un identifiant de route distinct ;
- le planning répartit le CA par distance au lieu de le répéter sur chaque trajet ;
- le grand régional choisit son stationnement après contrôle géographique local et vérification routière d'une reprise à partir de 5 h ;
- la propagation complète des fenêtres sources reste à implémenter.

L'évolution complète couvre les sujets suivants ; la section « Première étape implémentée » précise ceux déjà câblés :

1. La persistance des contraintes horaires sur le service.
2. Les données d'import et les contrats de création.
3. La planification et le découpage.
4. La création et la mise à jour de plusieurs trajets.
5. La distinction entre identifiant du service et identifiant du trajet dans le planning.
6. La consultation de tous les trajets d'un service.
7. La répartition et l'agrégation du CA.
8. Le comportement des événements et ajustements manuels lors d'un recalcul.
9. La synchronisation sans doublons.
10. La persistance et l'affichage des quatre bornes horaires de chaque trajet.
11. L'utilisation de l'arrivée minimale pour le calcul initial, puis des horaires effectivement retenus pour chaque portion.
12. La saisie des durées de chargement/déchargement sur le service et leur initialisation à 45 minutes chacune lors des imports Dashdoc.
13. Le découpage récursif sans limite de jours, avec estimation de position à 20 h et exception pour les retours au stationnement.
14. Le calcul du départ selon la livraison maximale dans les créneaux de conduite, reprise avant 05 h seulement si nécessaire, sans recompter un chargement déjà effectué.
15. La saisie optionnelle des plages horaires dans l'interface.

## 12. Points à arbitrer avant développement

| Sujet | Décision nécessaire |
|---|---|
| Estimation à 20 h | HERE est câblé ; quel adaptateur de chronologie retenir pour PTV ? |
| Mesure de proximité | Implémentée à vol d'oiseau sur les segments du tracé ; ne pas substituer une mesure de détour sans évolution métier explicite. |
| Propagation sur plusieurs jours | Le calcul à rebours et les nuits sont câblés ; quelle recherche d'alternatives ajouter lorsqu'un recalcul dépendant de l'heure rend le planning proposé incompatible ? |
| Horaires du premier jour régional | Quelle heure utiliser pour déterminer le retour du premier jour vers le stationnement quand seule la borne `arrivalEnd` du lendemain est connue ? |
| Manutention multi-points | Comment appliquer les deux durées du service si une mission comporte plusieurs chargements ou déchargements ? |
| Chargé la veille | Quelle donnée permet de déterminer cet état de manière fiable ? |
| Créneaux multiples | Comment sélectionner un créneau parmi plusieurs possibilités ? |
| Données manquantes | Quelle procédure appliquer si conducteur, stationnement ou fenêtre sont absents ? |
| Approche à vide | À quel service rattacher ce déplacement et participe-t-il à la répartition du CA ? |

Ces points ne doivent pas être transformés en hypothèses implicites dans le code.

## 13. Critères de recette

- Une mission sur deux jours produit un service unique et deux trajets ordonnés.
- Une mission sur trois jours ou plus réapplique récursivement les mêmes règles et conserve un service unique.
- Chaque coupure sur route intervient à la position estimée à 20 h exactement.
- Un retour au stationnement retenu peut se terminer après 20 h, sans coupure intermédiaire imposée à 20 h.
- Les fenêtres sources restent disponibles après calcul.
- Chaque trajet porte et affiche un départ minimal/maximal et une arrivée minimale/maximale cohérents.
- Le calcul initial vise l'arrivée minimale source ; les portions sont recalculées selon leurs horaires effectifs et les échéances retenues.
- Les temps de pause ne sont pas ajoutés au calcul des horaires ou à l'estimation de progression à 20 h.
- Les durées de chargement/déchargement sont stockées sur le service et saisissables manuellement.
- Un import Dashdoc initialise ces durées à 45 minutes chacune ; une synchronisation préserve les ajustements manuels.
- Les durées de manutention s'appliquent aux opérations réelles et ne sont pas dupliquées lors d'un découpage nocturne.
- La destination du premier trajet correspond à l'origine du second.
- Pour un `REGIONAL` dont `arrivalEnd` est le lendemain, deux trajets passent par le stationnement et le second vise `arrivalEnd - déchargement`.
- Les revenus présentés dans le planning sont répartis entre les trajets et leur somme égale exactement le CA du service.
- Un grand régional utilise son stationnement uniquement si les deux conditions sont remplies.
- Une distance exactement égale à 50 km ne satisfait pas la règle « moins de 50 km ».
- Une arrivée exactement à la borne haute respecte la fenêtre.
- Un régional conserve la règle du retour au stationnement ; toute impossibilité est signalée.
- Un régional déjà chargé utilise « Arriver à » la borne basse de livraison.
- Le départ est le plus tardif compatible avec la livraison et les coupures de 20 h à 05 h ; une reprise avant 05 h n'est autorisée que si nécessaire, sans recompter le chargement.
- Un chargement effectué la veille n'est pas déduit à nouveau pour calculer la reprise.
- Les plages horaires proviennent de Dashdoc ou des champs manuels optionnels de l'interface ; les bornes absentes ne sont pas inventées.
- Les coûts correspondent aux trajets et aux journées réels.
- La somme des CA des trajets égale exactement le CA du service.
- Le planning et la fiche détail exposent tous les trajets sans dupliquer le CA.
- Une nouvelle synchronisation du même service ne crée pas de doublon.
- La règle de 20 h et les calculs de départ restent corrects lors des changements d'heure.
