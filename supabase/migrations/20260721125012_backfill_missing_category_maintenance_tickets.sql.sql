/*
# Backfill missing category for Maintenance Tracker tickets

## Summary
Some Maintenance Tracker tickets have no `category` key in their `data` jsonb
column (data->>'category' is NULL). Set those to "Civil Maintenance" (the first
category in the new Maintenance Tracker category list) so every ticket has a
valid category under the updated dropdown.

## Changes
- `tickets.data` for Maintenance Tracker tickets with a missing/NULL category
  is updated to include `category: "Civil Maintenance"`.

## Security
- No RLS or policy changes. Data-only update.
*/

UPDATE tickets
SET data = jsonb_set(COALESCE(data, '{}'::jsonb), '{category}', '"Civil Maintenance"'::jsonb),
    updated_at = now()
WHERE module_id = (
        SELECT id FROM modules WHERE name = 'Maintenance Tracker'
      )
  AND (data->>'category' IS NULL);
