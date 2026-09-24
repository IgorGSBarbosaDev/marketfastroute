# ADR-004 — Operational 3D Store Map

## Status

Accepted

## Context

The customer route must be legible as a walkable path through the store. The
previous Three.js scene drew a decorative route that was unrelated to the
active map or the backend's calculated route. The customer workflow also needs
to retain the existing 2D map as an alternate view.

## Decision

- Render the selected store's active map in both SVG/2D and Three.js/3D.
- Use the active map API response as the source for sectors, aisle geometry,
  shelf blocks and points of interest in the 3D scene.
- Use the existing route API response as the source for the 3D route. Draw its
  ordered path points directly; do not calculate or optimize a second path in
  the frontend.
- Highlight selected product stops using route stop order, product names and
  the matching product locations already loaded by the shopping workflow.
- Open the 3D view after a route is calculated. Let the customer switch to the
  SVG/2D view at any time without losing the route.
- Keep Three.js behind a dynamic import. Support orbit, pan, zoom, route
  framing and reset controls. If WebGL is unavailable or interrupted, offer the
  SVG/2D view.
- Model the seeded Mercado Aurora as a fictional medium supermarket with
  entrance-side produce and bakery, central grocery, perimeter dairy and frozen
  sections, fourteen 2.5 m product aisles, three cross-aisles and clear shelf
  setbacks.
- Keep fixtures and packaging stylized and procedural. The 3D view is
  illustrative and is not a photorealistic digital twin.
- Do not change backend route calculation or persistence contracts.

## Consequences

- The 3D map changes when the selected store or its active map changes.
- The route in both views comes from the same published graph and backend
  response.
- Misplaced navigation nodes in any published store map can still produce a
  route in the wrong physical place; the map editor must keep graph nodes in
  walkable corridors. The fictional demo fixture now follows that rule.
- The Mercado Aurora seed can refresh only while its version 3 map is a draft.
  It refuses to rewrite a published map.
- Three.js adds work to the browser while the 3D map is visible, so geometry
  and pixel ratio remain bounded and the 2D map stays available.

## Supersedes

This decision supersedes ADR-003, which kept the Three.js scene separate from
the operational map and route.
