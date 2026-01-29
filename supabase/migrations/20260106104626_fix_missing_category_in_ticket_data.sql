/*
  # Fix Missing Category in Ticket Data

  ## Summary
  This migration ensures all tickets have a category field in their data JSONB column.
  Many tickets may have been created without the category field, causing display issues
  in the "Copy from Existing Ticket" modal.

  ## Changes
  1. Updates tickets with NULL or empty data to include default category and department
  2. Adds category field to tickets that have data but are missing the category key
  3. Ensures data integrity for existing tickets

  ## Impact
  - Fixes display issues in Copy Ticket modal
  - Ensures all tickets have required category and department fields
  - No data loss - only adds missing fields with sensible defaults
*/

-- First, update tickets with NULL or completely empty data
UPDATE tickets
SET data = jsonb_build_object(
  'department', COALESCE((data->>'department'), 'GENERAL'),
  'category', COALESCE((data->>'category'), 'General')
)
WHERE data IS NULL OR data = '{}'::jsonb;

-- Then, add category to tickets that have some data but are missing the category key
UPDATE tickets
SET data = jsonb_set(
  data,
  '{category}',
  '"General"'::jsonb,
  true
)
WHERE data IS NOT NULL
  AND data != '{}'::jsonb
  AND NOT (data ? 'category');

-- Also ensure department exists in all tickets
UPDATE tickets
SET data = jsonb_set(
  data,
  '{department}',
  '"GENERAL"'::jsonb,
  true
)
WHERE data IS NOT NULL
  AND data != '{}'::jsonb
  AND NOT (data ? 'department');

-- Add an index to speed up JSONB queries on category
CREATE INDEX IF NOT EXISTS idx_tickets_data_category ON tickets USING gin ((data -> 'category'));
CREATE INDEX IF NOT EXISTS idx_tickets_data_department ON tickets USING gin ((data -> 'department'));
