## General Rules

- Follow `/docs/PRD.md`, `/docs/SCOPE.md` and `/docs/ARCHITECTURE.md` as the source of truth.
- Do not invent requirements, business rules, entities, integrations or technologies.
- If something important is undefined, treat it as `UNRESOLVED` instead of assuming.
- Keep changes small, focused and consistent with the existing architecture.

## Code Quality

- Follow Clean Code principles.
- Prefer simple and readable solutions over clever abstractions.
- Apply DRY when duplication represents the same knowledge, but avoid premature abstraction.
- Follow SOLID where it improves clarity and maintainability.
- Use clear and intention-revealing names.
- Keep functions and classes focused on a single responsibility.
- Avoid unnecessary complexity, deep nesting and oversized files.
- Prefer composition over inheritance when appropriate.
- Remove dead code, unused imports and temporary workarounds.

## Software Engineering

- Respect separation of concerns.
- Keep domain rules in the backend, not duplicated in the frontend.
- Validate inputs at system boundaries.
- Handle errors explicitly and consistently.
- Preserve backward compatibility unless a breaking change is intentional and documented.
- Prefer deterministic and testable code.
- Add or update tests for relevant behavior changes.
- Do not optimize prematurely; measure before introducing complexity.
- Avoid adding dependencies when the existing stack can solve the problem adequately.

## Architecture

- Maintain the monorepo structure.
- Frontend: TypeScript + React + Vite + shadcn/ui.
- Backend: Java + Spring Boot.
- Persistence: PostgreSQL.
- Redis: cache and ephemeral data only when justified.
- Infrastructure: Docker / Docker Compose.
- Keep the backend as a modular monolith unless explicitly changed.
- Do not introduce microservices, new frameworks or alternative databases without documented approval.

## Before Finishing a Change

Check that:

- the implementation matches the documented scope;
- no unnecessary functionality was added;
- naming and structure are consistent;
- duplicated logic was avoided where appropriate;
- errors and edge cases were considered;
- tests were added or updated when relevant;
- documentation was updated if behavior or architecture changed.
