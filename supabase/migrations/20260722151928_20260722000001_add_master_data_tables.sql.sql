/*
# Add Master Data Tables for Categories, Departments, Locations, Config, and Ticket Number Counter

## Overview
This migration creates the foundational master data infrastructure for the ticket tracking system:
1. Three master tables for categories, departments, and locations (admin-managed CRUD)
2. A system config table for configurable values like the company code
3. A ticket number counter table for generating sequential ticket IDs per location+module
4. Seeds all tables with existing hardcoded values

## New Tables

### master_categories
- `id` (uuid, PK) - Unique identifier
- `name` (text, unique, not null) - Category name (e.g., "Civil Maintenance")
- `is_active` (boolean, default true) - Soft delete / deactivation flag
- `display_order` (integer, default 0) - Sort order for UI display
- `created_at` (timestamptz) - Creation timestamp
- `updated_at` (timestamptz) - Last update timestamp

### master_departments
- `id` (uuid, PK) - Unique identifier
- `name` (text, unique, not null) - Department name (e.g., "Civil")
- `is_active` (boolean, default true) - Soft delete / deactivation flag
- `display_order` (integer, default 0) - Sort order for UI display
- `created_at` (timestamptz) - Creation timestamp
- `updated_at` (timestamptz) - Last update timestamp

### master_locations
- `id` (uuid, PK) - Unique identifier
- `name` (text, unique, not null) - Location name (e.g., "Location01")
- `is_active` (boolean, default true) - Soft delete / deactivation flag
- `display_order` (integer, default 0) - Sort order for UI display
- `created_at` (timestamptz) - Creation timestamp
- `updated_at` (timestamptz) - Last update timestamp

### master_config
- `id` (uuid, PK) - Unique identifier
- `key` (text, unique, not null) - Config key (e.g., "company_code")
- `value` (text, not null) - Config value (e.g., "NMDC")
- `description` (text) - Human-readable description of the config
- `updated_at` (timestamptz) - Last update timestamp

### ticket_number_counter
- `id` (uuid, PK) - Unique identifier
- `location_prefix` (text, not null) - First 3 chars of location name, uppercase
- `module_code` (text, not null) - Module-specific code (e.g., "MAINT", "COMP")
- `counter` (integer, default 0) - Running counter for this location+module combo
- `created_at` (timestamptz) - Creation timestamp
- `updated_at` (timestamptz) - Last update timestamp
- Unique constraint on (location_prefix, module_code)

## Security
- RLS enabled on all new tables with permissive policies (matching existing pattern: TO public, USING (true) WITH CHECK (true))
- Application-level authorization enforced in the service layer (EO-only access for mutations)

## Important Notes
1. The `master_config` table is seeded with `company_code = 'NMDC'`
2. The `master_categories` table is seeded with the 12 existing hardcoded categories
3. The `master_locations` table is seeded with "Location01" and "Location02"
4. The `master_departments` table is seeded with departments extracted from existing user data
5. The `ticket_number_counter` table starts empty; counters are created on-demand when the first ticket for a given location+module combo is generated
6. Module codes are stored in the existing `modules.config` JSONB field under the key `moduleCode`
*/

-- ============================================================================
-- MASTER CATEGORIES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS master_categories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text UNIQUE NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  display_order integer NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_master_categories_active ON master_categories(is_active);
CREATE INDEX IF NOT EXISTS idx_master_categories_order ON master_categories(display_order);

ALTER TABLE master_categories ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all operations on master_categories" ON master_categories;
CREATE POLICY "Allow all operations on master_categories" ON master_categories FOR ALL TO public USING (true) WITH CHECK (true);

DROP TRIGGER IF EXISTS update_master_categories_updated_at ON master_categories;
CREATE TRIGGER update_master_categories_updated_at BEFORE UPDATE ON master_categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- MASTER DEPARTMENTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS master_departments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text UNIQUE NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  display_order integer NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_master_departments_active ON master_departments(is_active);
CREATE INDEX IF NOT EXISTS idx_master_departments_order ON master_departments(display_order);

ALTER TABLE master_departments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all operations on master_departments" ON master_departments;
CREATE POLICY "Allow all operations on master_departments" ON master_departments FOR ALL TO public USING (true) WITH CHECK (true);

DROP TRIGGER IF EXISTS update_master_departments_updated_at ON master_departments;
CREATE TRIGGER update_master_departments_updated_at BEFORE UPDATE ON master_departments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- MASTER LOCATIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS master_locations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text UNIQUE NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  display_order integer NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_master_locations_active ON master_locations(is_active);
CREATE INDEX IF NOT EXISTS idx_master_locations_order ON master_locations(display_order);

ALTER TABLE master_locations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all operations on master_locations" ON master_locations;
CREATE POLICY "Allow all operations on master_locations" ON master_locations FOR ALL TO public USING (true) WITH CHECK (true);

DROP TRIGGER IF EXISTS update_master_locations_updated_at ON master_locations;
CREATE TRIGGER update_master_locations_updated_at BEFORE UPDATE ON master_locations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- MASTER CONFIG TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS master_config (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  key text UNIQUE NOT NULL,
  value text NOT NULL,
  description text,
  updated_at timestamptz DEFAULT now()
);

ALTER TABLE master_config ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all operations on master_config" ON master_config;
CREATE POLICY "Allow all operations on master_config" ON master_config FOR ALL TO public USING (true) WITH CHECK (true);

DROP TRIGGER IF EXISTS update_master_config_updated_at ON master_config;
CREATE TRIGGER update_master_config_updated_at BEFORE UPDATE ON master_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TICKET NUMBER COUNTER TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS ticket_number_counter (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  location_prefix text NOT NULL,
  module_code text NOT NULL,
  counter integer NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now(),
  CONSTRAINT uq_ticket_counter_location_module UNIQUE (location_prefix, module_code)
);

CREATE INDEX IF NOT EXISTS idx_ticket_counter_location ON ticket_number_counter(location_prefix);
CREATE INDEX IF NOT EXISTS idx_ticket_counter_module ON ticket_number_counter(module_code);

ALTER TABLE ticket_number_counter ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all operations on ticket_number_counter" ON ticket_number_counter;
CREATE POLICY "Allow all operations on ticket_number_counter" ON ticket_number_counter FOR ALL TO public USING (true) WITH CHECK (true);

DROP TRIGGER IF EXISTS update_ticket_number_counter_updated_at ON ticket_number_counter;
CREATE TRIGGER update_ticket_number_counter_updated_at BEFORE UPDATE ON ticket_number_counter FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- SEED DATA: MASTER CATEGORIES
-- ============================================================================
INSERT INTO master_categories (name, display_order) VALUES
  ('Civil Maintenance', 1),
  ('Electrical Maintenance', 2),
  ('Plumbing & Sanitary', 3),
  ('Carpentry', 4),
  ('HVAC / Air Conditioning', 5),
  ('Water Supply', 6),
  ('Sewage & Drainage', 7),
  ('Road & External Area', 8),
  ('Housekeeping, Fire & Safety', 9),
  ('Security Systems', 10),
  ('Street Lighting', 11),
  ('Utility Services', 12)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- SEED DATA: MASTER LOCATIONS
-- ============================================================================
INSERT INTO master_locations (name, display_order) VALUES
  ('Location01', 1),
  ('Location02', 2)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- SEED DATA: MASTER DEPARTMENTS
-- Extract unique departments from existing users table
-- ============================================================================
INSERT INTO master_departments (name, display_order)
SELECT DISTINCT department, ROW_NUMBER() OVER (ORDER BY department)
FROM users
WHERE department IS NOT NULL AND department != ''
ON CONFLICT (name) DO NOTHING;

-- If no users exist yet, seed with default departments
INSERT INTO master_departments (name, display_order) VALUES
  ('Civil', 1),
  ('Electrical', 2),
  ('Mechanical', 3),
  ('Civil Manager', 4),
  ('Electrical Manager', 5)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- SEED DATA: MASTER CONFIG
-- ============================================================================
INSERT INTO master_config (key, value, description) VALUES
  ('company_code', 'NMDC', 'Company code used in ticket number generation (e.g., TKT-NMDCLOC-MAINT-000001)')
ON CONFLICT (key) DO NOTHING;

-- ============================================================================
-- UPDATE MODULES: Add moduleCode to config JSONB
-- ============================================================================
UPDATE modules
SET config = jsonb_set(
  COALESCE(config, '{}'::jsonb),
  '{moduleCode}',
  COALESCE(
    (config->>'moduleCode')::jsonb,
    CASE
      WHEN schema_id = 'maintenance' THEN '"MAINT"'::jsonb
      WHEN schema_id = 'complaints' THEN '"COMP"'::jsonb
      WHEN schema_id = 'grievances' THEN '"GREV"'::jsonb
      WHEN schema_id = 'rti' THEN '"RTI"'::jsonb
      WHEN schema_id = 'pep' THEN '"PEP"'::jsonb
      ELSE '"TKT"'::jsonb
    END
  )
)
WHERE config->>'moduleCode' IS NULL;