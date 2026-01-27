-- V2: Add role grouping and ordering fields for matrix overview
-- Feature: 004-matrix-overview
-- Purpose: Enable roles to be grouped by family and ordered by seniority

-- Add new columns for role grouping
ALTER TABLE rolename ADD COLUMN role_family VARCHAR(50);
ALTER TABLE rolename ADD COLUMN seniority_order INTEGER;

-- Populate existing roles with grouping data
UPDATE rolename SET role_family = 'Developer', seniority_order = 1
  WHERE name = 'Junior Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 2
  WHERE name = 'Medior Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 3
  WHERE name = 'Senior Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 4
  WHERE name = 'Specialist Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 5
  WHERE name = 'Lead Developer';
UPDATE rolename SET role_family = 'Architect', seniority_order = 1
  WHERE name = 'Lead Developer / Software Architect';
UPDATE rolename SET role_family = 'Architect', seniority_order = 2
  WHERE name = 'Software Architect';
UPDATE rolename SET role_family = 'Architect', seniority_order = 3
  WHERE name = 'Solution Architect';
UPDATE rolename SET role_family = 'Operations', seniority_order = 1
  WHERE name = 'DevOps Engineer';

-- Set default values for any other roles not in the seed data
-- This handles test data and ensures migration doesn't fail
UPDATE rolename SET role_family = 'Other', seniority_order = 999
  WHERE role_family IS NULL OR seniority_order IS NULL;

-- Add NOT NULL constraints after data population
ALTER TABLE rolename ALTER COLUMN role_family SET NOT NULL;
ALTER TABLE rolename ALTER COLUMN seniority_order SET NOT NULL;

-- Note: role_family = Role family grouping (e.g., Developer, Architect, Operations) for matrix display
-- Note: seniority_order = Ordering within role family (1=Junior, 2=Medior, etc.)
