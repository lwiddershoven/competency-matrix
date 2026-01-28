-- V2: Add role grouping and ordering fields for matrix overview
-- Feature: 004-matrix-overview
-- Purpose: Enable roles to be grouped by family and ordered by seniority

-- Add new columns for role grouping
ALTER TABLE rolename ADD COLUMN role_family VARCHAR(50);
ALTER TABLE rolename ADD COLUMN seniority_order INTEGER;

-- Add NOT NULL constraints after data population
ALTER TABLE rolename ALTER COLUMN role_family SET NOT NULL;
ALTER TABLE rolename ALTER COLUMN seniority_order SET NOT NULL;

-- Note: role_family = Role family grouping (e.g., Developer, Architect, Operations) for matrix display
-- Note: seniority_order = Ordering within role family (1=Junior, 2=Medior, etc.)
