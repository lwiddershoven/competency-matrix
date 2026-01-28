# API Contracts

**Feature**: 006-competencies-split

## No Contracts Required

This feature involves only internal data loading changes. There are no:

- REST API changes
- GraphQL schema changes
- External interfaces
- Public APIs

The competencies data loading mechanism is internal to the application. The database schema and all REST endpoints remain unchanged.

## Why No Contracts?

The feature modifies how YAML files are read from disk (single file → multiple files), but:

1. The in-memory data model is identical
2. The database schema is unchanged
3. The REST endpoints return the same JSON structure
4. The UI receives the same data structure

**External Perspective**: From outside the application, this change is invisible. Only the internal file organization changes.
