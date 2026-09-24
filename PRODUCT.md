# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- Primary: customers shopping in large supermarkets who know what they want to buy but need help finding those products inside a specific store.
- Secondary: store administrators and employees responsible for store, catalog, map, navigation graph, and product-location data.

## Product Purpose

Market Fast Route helps a customer select a store, find available products, build a shopping list, and follow a route through that store from its configured entrance to its checkout area.

## Positioning

The product combines a store-specific indoor map, product locations, and backend route calculation to order a multi-product shopping trip. The route is a deterministic MVP heuristic and is not guaranteed to be globally optimal.

## Operating Context

- Customers use a responsive web interface on a phone, tablet, or desktop while planning or shopping in a supermarket.
- Administrators configure store and map data through a local or private-network deployment. The MVP does not require customer accounts or administrative authentication and must not be exposed to the public internet.
- The product does not know the customer's live indoor position. The displayed route begins at the configured store entrance and ends at its configured checkout.

## Capabilities and Constraints

- Customer scope: store selection; product search by name, category, SKU, or EAN when supplied; shopping-list add, remove, and quantity changes; route calculation; store-specific map, sectors, aisles, points of interest, product locations, zoom, and pan.
- Administration scope: stores, categories, products, store availability, versioned maps, sectors, aisles, shelf blocks, points of interest, graph nodes and edges, and product locations. Existing logical activation and no-physical-delete behavior are retained.
- The operational map has SVG/2D and Three.js/3D views from the same active map returned by the API. Dijkstra calculates shortest graph segments; a deterministic nearest-neighbor heuristic orders multiple stops.
- The 3D scene renders store geometry and the backend route directly from API data. It uses stylized, procedural fixtures and labels; the fictional Mercado Aurora seed is clearly identified as demonstration data.
- Shopping lists and routes are not persisted as backend entities. Product price, stock, payment, customer accounts, indoor positioning, and external integrations are outside the MVP.
- Technical stack: React, TypeScript, Vite, shadcn/ui, Java, Spring Boot, PostgreSQL, Docker Compose. Three.js is approved for the operational 3D view and remains a documented architecture exception.
- Confirmed open product/operation decisions remain in the source-of-truth docs; do not silently invent business rules.

## Brand Commitments

- Product name: Market Fast Route.
- No logo, brand palette, or existing visual identity is established in the repository.
- Google Maps and Waze are product-description references, not permission to copy their branding or interface.

## Evidence on Hand

- Product and technical requirements: `docs/PRD.md`, `docs/SCOPE.md`, `docs/ARCHITECTURE.md`, and `docs/DATA_MODEL.md`.
- Routing decision: `docs/ADR-001-routing.md`.
- Public and administrative API source code and PostgreSQL/Flyway migrations.
- No real supermarket map, real store catalog, customer testimonials, performance measurements, or brand artwork is present. Any demo-store data and scene must be labeled fictional.

## Product Principles

1. Help customers locate products with minimal interaction.
2. Keep the displayed route and store map grounded in the selected store's active API data.
3. Keep domain rules in the backend and present their errors clearly.
4. Preserve orientation and map visibility across responsive layouts.
5. Keep demonstrations honest: synthetic data and illustrative 3D must be labeled and never presented as live positioning or real-store truth.

## Accessibility & Inclusion

- Support responsive phone, tablet, and desktop layouts.
- Support keyboard operation, visible focus, adequate contrast, and a textual route representation.
- Respect `prefers-reduced-motion`, with controls for nonessential Three.js animation and a static fallback when WebGL is unavailable.
