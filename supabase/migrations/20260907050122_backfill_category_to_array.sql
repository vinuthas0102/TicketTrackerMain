/*
# Backfill category from single string to JSON array

1. Purpose
   The "Category" field on tickets is being changed from single-select to multi-select.
   Category is stored inside the `data` JSONB column on the `tickets` table as `data.category`.
   Currently `data.category` is a single string (e.g., "Civil Maintenance").
   This migration backfills all existing tickets so `data.category` becomes a JSON array
   (e.g., ["Civil Maintenance"]), making the data format consistent with the new multi-select UI.

2. Changes
   - Updates all rows in `tickets` where `data->>'category'` is a string (not an array)
   - Converts the single string value to a single-element JSON array
   - Leaves rows that already have an array value untouched
   - Leaves rows with no category untouched

3. Security
   - No schema changes, no new tables, no RLS policy changes
   - This is a data-only migration

4. Important Notes
   - This migration is idempotent: it only converts string values, not existing arrays
   - No data is lost: the original category string is preserved as the sole element in the new array
*/

UPDATE tickets
SET data = jsonb_set(
  data,
  '{category}',
  to_jsonb(ARRAY[data->>'category'])
)
WHERE data ? 'category'
  AND jsonb_typeof(data->'category') = 'string';
