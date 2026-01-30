-- Update proficiency level names from English to Dutch
-- Migration to support Dutch level names: BASIS, REDELIJK, GOED, UITSTEKEND

-- Drop the old constraint
ALTER TABLE role_skill_requirement DROP CONSTRAINT IF EXISTS chk_required_level;

-- Add new constraint with Dutch level names
ALTER TABLE role_skill_requirement ADD CONSTRAINT chk_required_level
    CHECK (required_level IN ('BASIS', 'REDELIJK', 'GOED', 'UITSTEKEND'));

-- Update existing data (if any) to use Dutch names
UPDATE role_skill_requirement SET required_level = 'BASIS' WHERE required_level = 'BASIC';
UPDATE role_skill_requirement SET required_level = 'REDELIJK' WHERE required_level = 'DECENT';
UPDATE role_skill_requirement SET required_level = 'GOED' WHERE required_level = 'GOOD';
UPDATE role_skill_requirement SET required_level = 'UITSTEKEND' WHERE required_level = 'EXCELLENT';
