This directory contains the versioned PostgreSQL schema migrations for the
MVP. Flyway executes files in version order using the naming convention
`V<version>__<description>.sql`.

The data model and the migrations must evolve together. Once a migration has
been applied to a shared environment, do not edit it; create a new version.
