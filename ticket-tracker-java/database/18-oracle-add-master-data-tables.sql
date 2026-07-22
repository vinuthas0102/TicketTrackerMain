-- ============================================================================
-- Master Data Tables: Categories, Departments, Locations, Config, Counter
-- ============================================================================

-- Master Categories table
CREATE TABLE master_categories (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(255) NOT NULL UNIQUE,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    display_order NUMBER(10) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
CREATE INDEX idx_master_categories_active ON master_categories(is_active);
CREATE INDEX idx_master_categories_order ON master_categories(display_order);

-- Master Departments table
CREATE TABLE master_departments (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(255) NOT NULL UNIQUE,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    display_order NUMBER(10) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
CREATE INDEX idx_master_departments_active ON master_departments(is_active);
CREATE INDEX idx_master_departments_order ON master_departments(display_order);

-- Master Locations table
CREATE TABLE master_locations (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(255) NOT NULL UNIQUE,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    display_order NUMBER(10) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
CREATE INDEX idx_master_locations_active ON master_locations(is_active);
CREATE INDEX idx_master_locations_order ON master_locations(display_order);

-- Master Config table (for company code and other system-wide settings)
CREATE TABLE master_config (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    config_key VARCHAR2(100) NOT NULL UNIQUE,
    value VARCHAR2(500) NOT NULL,
    description VARCHAR2(500),
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

-- Ticket Number Counter table (tracks sequential counters per location+module)
CREATE TABLE ticket_number_counter (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    location_prefix VARCHAR2(10) NOT NULL,
    module_code VARCHAR2(20) NOT NULL,
    counter NUMBER(10) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_ticket_counter_loc_mod UNIQUE (location_prefix, module_code)
);
CREATE INDEX idx_ticket_counter_location ON ticket_number_counter(location_prefix);
CREATE INDEX idx_ticket_counter_module ON ticket_number_counter(module_code);

-- ============================================================================
-- Updated_at triggers
-- ============================================================================
CREATE OR REPLACE TRIGGER trg_master_categories_updated_at
BEFORE UPDATE ON master_categories FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_master_departments_updated_at
BEFORE UPDATE ON master_departments FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_master_locations_updated_at
BEFORE UPDATE ON master_locations FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_master_config_updated_at
BEFORE UPDATE ON master_config FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_ticket_number_counter_updated_at
BEFORE UPDATE ON ticket_number_counter FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- ============================================================================
-- Seed Data: Master Categories
-- ============================================================================
INSERT INTO master_categories (name, display_order) VALUES ('Civil Maintenance', 1);
INSERT INTO master_categories (name, display_order) VALUES ('Electrical Maintenance', 2);
INSERT INTO master_categories (name, display_order) VALUES ('Plumbing & Sanitary', 3);
INSERT INTO master_categories (name, display_order) VALUES ('Carpentry', 4);
INSERT INTO master_categories (name, display_order) VALUES ('HVAC / Air Conditioning', 5);
INSERT INTO master_categories (name, display_order) VALUES ('Water Supply', 6);
INSERT INTO master_categories (name, display_order) VALUES ('Sewage & Drainage', 7);
INSERT INTO master_categories (name, display_order) VALUES ('Road & External Area', 8);
INSERT INTO master_categories (name, display_order) VALUES ('Housekeeping, Fire & Safety', 9);
INSERT INTO master_categories (name, display_order) VALUES ('Security Systems', 10);
INSERT INTO master_categories (name, display_order) VALUES ('Street Lighting', 11);
INSERT INTO master_categories (name, display_order) VALUES ('Utility Services', 12);

-- ============================================================================
-- Seed Data: Master Locations
-- ============================================================================
INSERT INTO master_locations (name, display_order) VALUES ('Location01', 1);
INSERT INTO master_locations (name, display_order) VALUES ('Location02', 2);

-- ============================================================================
-- Seed Data: Master Departments
-- ============================================================================
INSERT INTO master_departments (name, display_order) SELECT DISTINCT department, ROW_NUMBER() OVER (ORDER BY department) FROM users WHERE department IS NOT NULL AND department != '' AND ROWNUM >= 1;
INSERT INTO master_departments (name, display_order) SELECT 'Civil', 1 FROM dual WHERE NOT EXISTS (SELECT 1 FROM master_departments WHERE name = 'Civil');
INSERT INTO master_departments (name, display_order) SELECT 'Electrical', 2 FROM dual WHERE NOT EXISTS (SELECT 1 FROM master_departments WHERE name = 'Electrical');
INSERT INTO master_departments (name, display_order) SELECT 'Mechanical', 3 FROM dual WHERE NOT EXISTS (SELECT 1 FROM master_departments WHERE name = 'Mechanical');

-- ============================================================================
-- Seed Data: Master Config
-- ============================================================================
INSERT INTO master_config (config_key, value, description) VALUES ('company_code', 'NMDC', 'Company code used in ticket number generation');

-- ============================================================================
-- Update Modules: Add moduleCode to config JSON
-- ============================================================================
UPDATE modules SET config = '{"moduleCode":"MAINT"}' WHERE schema_id = 'maintenance' AND (config IS NULL OR config NOT LIKE '%"moduleCode"%');
UPDATE modules SET config = '{"moduleCode":"COMP"}' WHERE schema_id = 'complaints' AND (config IS NULL OR config NOT LIKE '%"moduleCode"%');
UPDATE modules SET config = '{"moduleCode":"GREV"}' WHERE schema_id = 'grievances' AND (config IS NULL OR config NOT LIKE '%"moduleCode"%');
UPDATE modules SET config = '{"moduleCode":"RTI"}' WHERE schema_id = 'rti' AND (config IS NULL OR config NOT LIKE '%"moduleCode"%');
UPDATE modules SET config = '{"moduleCode":"PEP"}' WHERE schema_id = 'pep' AND (config IS NULL OR config NOT LIKE '%"moduleCode"%');
