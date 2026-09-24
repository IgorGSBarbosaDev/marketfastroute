# ADR-002 — Coordinate Contract and Map Publication

## Status

Accepted

## Context

The map schema stores width, height, position, size and rotation, but the
original documents did not define the coordinate origin or rotation unit. The
API also allowed a map to become active without checking whether its geometry
and navigation graph could support the route promised by the PRD.

## Decisions

### Coordinates

- Map coordinates use map units. `scale_meters_per_unit` converts those units
  to meters for visual sizing; route edge costs remain expressed in meters.
- The origin `(0, 0)` is the map's top-left corner. `x` increases to the right
  and `y` increases downward.
- `x` and `y` for rectangular structures identify the unrotated top-left
  corner. `width` and `height` are positive map-unit dimensions.
- `rotation` is in degrees clockwise around the rectangle's center.
- Map nodes and points of interest are points in the same coordinate plane.
- A product location may omit both `x` and `y`; when both exist they identify
  a point in the same plane. It always uses its navigation node for routing.
- Rotated structures must fit inside the map's axis-aligned bounds. The
  validator uses the rotated rectangle's axis-aligned bounding box.

No migration rewrites existing map rows. Administrators can inspect a map's
validation report and correct it explicitly before publication.

### Publication

- New maps start as `DRAFT`; creating a map directly as `ACTIVE` or `ARCHIVED`
  is rejected.
- Map status transitions are one-way: `DRAFT` may remain a draft or become
  `ACTIVE`; `ACTIVE` may remain active or become `ARCHIVED`; `ARCHIVED` is
  terminal. To republish changed data, create a new draft version.
- A published map's metadata, structures, graph, points of interest and product
  locations are immutable. Structural and location writes require a `DRAFT`
  map; archiving changes only the status and does not rewrite that version.
- Administrators can validate a draft at
  `GET /api/v1/admin/maps/{mapId}/validation` before changing its status to
  `ACTIVE`.
- Publication requires at least one active sector, aisle, navigation node,
  navigation edge and product location; one usable entry and checkout;
  in-bounds active geometry; and a connected route network for every stop the
  route algorithm may select.
- ADR-001 endpoint selection remains authoritative: one active navigable
  point of interest takes precedence; if absent, one active node of the
  matching type is used. Ambiguous or missing endpoints block publication.
- Only the route's selected location per product participates in graph
  connectivity checks: the primary active location when present, otherwise
  the first location in stable identifier order. Every active location still
  needs an active node and valid coordinates when supplied.
- Writes to a map version lock its row and require `DRAFT`. Activation locks
  the store row before the map row, then validates inside the same transaction;
  this serializes activation against edits already in progress and makes later
  writes recheck the committed status.
- A different active map is never archived automatically. The administrator
  must archive the current version explicitly before activating a new version.
- The store row serializes concurrent activation attempts for separate draft
  versions; the database's partial unique index remains the final integrity
  guard.
- Invalid activation returns `422 MAP_NOT_PUBLISHABLE` with structured issues.
  The read-only validation report returns `publishable` and that issue list.

## Consequences

- The existing schema remains unchanged; no existing data is transformed.
- Existing maps outside the coordinate contract may need manual adjustment
  before activation or reactivation.
- Runtime routing and publication validation share one endpoint resolver.
- The admin workflow can explain invalid geometry and disconnected route
  nodes before activation.

## Alternatives Considered

- Automatically converting existing coordinates was rejected because the
  current database volume and saved map intent cannot be inspected or safely
  inferred from the new coordinate convention.
- Activating any structurally valid row was rejected because it can expose
  maps that fail the customer route flow.
- Archiving the previous active map during publication was rejected; the
  current model and user-approved operation require an explicit archive step.
