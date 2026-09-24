# ADR-003 — Three.js Fictional Market Demonstration

## Status

Superseded by [ADR-004 — Operational 3D Map](ADR-004-operational-3d-map.md)

## Context

The operational store map must represent persisted store data and remain
readable on phones. The project also needs a visually engaging example of how
a complex fictional supermarket could look in three dimensions.

## Decision

- Add the `three` package to the existing React/Vite frontend.
- Build one illustrative scene for a clearly labeled fictional market.
- Render the scene with Three.js over WebGL. Keep it behind a dedicated
  demonstration route or panel and load it only when requested.
- Model the fictional market as authored demo content: multiple aisles,
  intersections, turns, sectors, shelf blocks, entrance, cart area and
  checkout. Label it as a fictional demonstration.
- Keep customer navigation as SVG built from the active map API response. Use
  API route data for its path, stops and endpoints.
- Do not convert the fictional scene to or from API/store data. Do not use the
  scene to calculate, preview or select operational routes.
- Keep a static illustration or SVG fallback when WebGL cannot be created or
  when reduced-motion preferences call for a static presentation.
- Play one short, eight-second camera presentation when the demonstration is
  entered, then stop the render loop. Let the visitor pause, replay, or rotate
  the static scene; stop the loop when the view is hidden or unmounted.
- Limit geometry/material count, disable costly shadows and clamp device pixel
  ratio for ordinary phones.

## Consequences

- Three.js is a narrow, explicit exception to the existing stack policy.
- It increases frontend bundle size, so use dynamic import and keep it out of
  the shopping and operational map route's initial bundle.
- The fictional 3D scene is an illustration and has no operational authority.
- The SVG route map remains the source-of-truth visualization for the MVP.

## References

- [Three.js WebGLRenderer](https://threejs.org/docs/pages/WebGLRenderer.html)
- [Three.js: Creating a scene](https://threejs.org/manual/pages/creating-a-scene.html)
- [Apple Human Interface Guidelines: Motion](https://developer.apple.com/design/human-interface-guidelines/motion)
