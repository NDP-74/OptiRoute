# frontend

## Sélecteurs avec création

`CreateSelect` (`src/components/ui/CreateSelect.vue`) propose une sélection nullable
et une action de création, sans modification ni suppression. Il reçoit `v-model`,
`options` (`{ value, label }[]`), `label` (nom accessible), `placeholder`,
`createLabel` et `disabled`. L'événement `create` permet au parent d'ouvrir le modal
adapté ; après création, le parent ajoute l'option et met à jour la sélection.

Les formulaires conducteur, de planification et de modification d'un trajet
(`PlanningRouteModal` et `RouteSegmentPanel`) l'utilisent pour les tracteurs et semi-remorques :
**Nouveau véhicule** ouvre le modal existant et sélectionne le véhicule créé,
sans enregistrer ni réinitialiser le formulaire en cours.

Le même principe s'applique aux chauffeurs dans `RouteSegmentPanel` et
`PlanningRouteModal`, et aux donneurs d'ordre dans `ServiceInfoPanel` et
`PlanningRouteModal`. Le chauffeur créé est sélectionné avec ses véhicules
habituels, y compris ceux créés depuis son propre formulaire. Le donneur d'ordre
créé est sélectionné et utilisé pour le titre du service lors de l'enregistrement.

## Titre des services

À chaque création ou modification depuis le planning, le titre du service est
recalculé : nom du donneur d'ordre s'il est renseigné, sinon « Trajet à vide »
si la case correspondante est cochée, sinon « Départ - Arrivée » avec les noms
des lieux. Changer la case « Trajet à vide » actualise le titre enregistré même
sans relancer le calcul d'itinéraire.

## Points d'intérêt

Sur la carte des points d'intérêt, cliquer sur un marqueur ouvre le panneau existant
avec son adresse et son nom préremplis. Modifier ces champs puis enregistrer met
à jour le point et son marqueur. Une nouvelle adresse doit être sélectionnée dans
les suggestions (ou saisie sous forme de coordonnées `latitude, longitude`).
Le bouton **Annuler** permet de revenir au mode ajout.

Le bouton **Supprimer** ouvre une confirmation. Un point utilisé par un événement
ne peut pas être supprimé ; un message explicite est alors affiché.

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
