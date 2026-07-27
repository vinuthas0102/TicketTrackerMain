/*
# Module-linked categories + property master table

## Purpose
1. Make master categories module-specific so each module (Maintenance, Complaints,
   Grievances, RTI, PEP) has its own set of categories instead of one shared flat list.
2. Add a new master_properties table so Property IDs can be managed in Master Setup
   and selected from a dropdown when creating tickets (replacing hardcoded PROP001/PROP002).

## Changes

### 1. master_categories - add module_id column + replace unique constraint
- Added nullable `module_id uuid` column referencing modules(id).
- Dropped the old name-only UNIQUE constraint (master_categories_name_key) because
  the same category name must be allowed under different modules.
- Added a new unique constraint on (module_id, name) so duplicates within a module
  are still rejected, while the same name can exist under different modules.
- Added an index on module_id for filtered lookups.
- Backfilled rows from the existing per-module modules.config.categories JSON arrays,
  inserting one row per (module, category) pair with the correct module_id.
  Legacy flat rows (module_id NULL) are left in place for backward compatibility.

### 2. master_properties - new table
- id (uuid, primary key)
- name (text, unique, not null) - the property identifier e.g. PROP001
- is_active (boolean, default true)
- display_order (int, default 0)
- created_at, updated_at (timestamptz)
- Indexes on is_active and display_order.
- update_updated_at trigger for automatic updated_at.

### 3. Security
- RLS enabled on master_properties.
- Permissive public policy matching the existing master_categories / master_locations /
  master_departments tables (shared reference data, no auth gating).

### 4. Seed data
- master_properties seeded with PROP001 and PROP002 for continuity with existing tickets.
*/

-- ============================================================================
-- 1. master_categories: add module_id + replace unique constraint
-- ============================================================================
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'master_categories' AND column_name = 'module_id'
  ) THEN
    ALTER TABLE master_categories ADD COLUMN module_id uuid REFERENCES modules(id) ON DELETE SET NULL;
  END IF;
END $$;

-- Drop the old name-only unique constraint so the same name can exist per module.
ALTER TABLE master_categories DROP CONSTRAINT IF EXISTS master_categories_name_key;

CREATE INDEX IF NOT EXISTS idx_master_categories_module_id ON master_categories(module_id);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uq_master_categories_module_name'
  ) THEN
    ALTER TABLE master_categories
      ADD CONSTRAINT uq_master_categories_module_name UNIQUE (module_id, name);
  END IF;
END $$;

-- ============================================================================
-- 2. Backfill master_categories from modules.config.categories
-- ============================================================================
INSERT INTO master_categories (module_id, name, is_active, display_order, created_at, updated_at)
SELECT
  m.id,
  cat::text AS name,
  true AS is_active,
  row_number() OVER (PARTITION BY m.id ORDER BY ordinality) AS display_order,
  now(),
  now()
FROM modules m,
     jsonb_array_elements_text(m.config->'categories') WITH ORDINALITY AS cat
WHERE m.config->'categories' IS NOT NULL
  AND jsonb_typeof(m.config->'categories') = 'array'
ON CONFLICT (module_id, name) DO NOTHING;

-- ============================================================================
-- 3. master_properties table
-- ============================================================================
CREATE TABLE IF NOT EXISTS master_properties (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL UNIQUE,
  is_active boolean NOT NULL DEFAULT true,
  display_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_master_properties_active ON master_properties(is_active);
CREATE INDEX IF NOT EXISTS idx_master_properties_order ON master_properties(display_order);

ALTER TABLE master_properties ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow all operations on master_properties" ON master_properties;
CREATE POLICY "Allow all operations on master_properties"
ON master_properties FOR ALL
TO public
USING (true)
WITH CHECK (true);

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_master_properties_updated_at ON master_properties;
CREATE TRIGGER trg_master_properties_updated_at
BEFORE UPDATE ON master_properties
FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- ============================================================================
-- 4. Seed master_properties
-- ============================================================================
INSERT INTO master_properties (name, is_active, display_order)
VALUES ('PROP001', true, 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO master_properties (name, is_active, display_order)
VALUES ('PROP002', true, 2)
ON CONFLICT (name) DO NOTHING;
