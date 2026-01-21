-- Initial schema migration from Liquibase
-- Original: db/changelog/001-initial-schema.sql

CREATE TABLE competency_category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE rolename (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE skill (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id INTEGER NOT NULL,
    basic_description TEXT,
    decent_description TEXT,
    good_description TEXT,
    excellent_description TEXT,
    CONSTRAINT fk_skill_category FOREIGN KEY (category_id)
        REFERENCES competency_category(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT uk_skill_name_category UNIQUE (name, category_id)
);

CREATE INDEX idx_skill_category_id ON skill(category_id);

CREATE TABLE role_skill_requirement (
    id SERIAL PRIMARY KEY,
    role_id INTEGER NOT NULL,
    skill_id INTEGER NOT NULL,
    required_level VARCHAR(20) NOT NULL,
    CONSTRAINT fk_role_skill_role FOREIGN KEY (role_id)
        REFERENCES rolename(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_role_skill_skill FOREIGN KEY (skill_id)
        REFERENCES skill(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_required_level CHECK (required_level IN ('BASIC', 'DECENT', 'GOOD', 'EXCELLENT')),
    CONSTRAINT uk_role_skill UNIQUE (role_id, skill_id)
);

CREATE INDEX idx_role_skill_role_id ON role_skill_requirement(role_id);
CREATE INDEX idx_role_skill_skill_id ON role_skill_requirement(skill_id);

CREATE TABLE role_progression (
    id SERIAL PRIMARY KEY,
    from_role_id INTEGER NOT NULL,
    to_role_id INTEGER NOT NULL,
    CONSTRAINT fk_progression_from_role FOREIGN KEY (from_role_id)
        REFERENCES rolename(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_progression_to_role FOREIGN KEY (to_role_id)
        REFERENCES rolename(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uk_role_progression UNIQUE (from_role_id, to_role_id)
);

CREATE INDEX idx_progression_from_role_id ON role_progression(from_role_id);
CREATE INDEX idx_progression_to_role_id ON role_progression(to_role_id);
