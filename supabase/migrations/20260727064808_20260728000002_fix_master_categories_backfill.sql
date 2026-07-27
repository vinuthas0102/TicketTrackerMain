/*
# Fix master_categories backfill (correct category names)

## Purpose
The previous migration backfilled master_categories from modules.config.categories,
but the category names were inserted as composite-row text (e.g. `("Service Quality",1)`)
instead of the plain category string (e.g. `Service Quality`). This migration deletes
the malformed rows and re-inserts them with the correct name values.

## Changes
1. Delete all master_categories rows where module_id IS NOT NULL (the bad backfill).
   Legacy rows with module_id NULL are untouched.
2. Re-insert one row per (module, category) using jsonb_array_elements_text value
   column directly so the name is the plain category string.
3. ON CONFLICT (module_id, name) DO NOTHING keeps it idempotent.
*/

-- Remove the malformed backfilled rows (names look like ("Civil Maintenance",1))
DELETE FROM master_categories WHERE module_id IS NOT NULL;

-- Re-insert with correct names (use the value column from the ordinality table)
INSERT INTO master_categories (module_id, name, is_active, display_order, created_at, updated_at)
SELECT
  m.id,
  elem.value AS name,
  true AS is_active,
  row_number() OVER (PARTITION BY m.id ORDER BY elem.ordinality) AS display_order,
  now(),
  now()
FROM modules m
CROSS JOIN LATERAL jsonb_array_elements_text(m.config->'categories') WITH ORDINALITY AS elem(value, ordinality)
WHERE m.config->'categories' IS NOT NULL
  AND jsonb_typeof(m.config->'categories') = 'array'
ON CONFLICT (module_id, name) DO NOTHING;
