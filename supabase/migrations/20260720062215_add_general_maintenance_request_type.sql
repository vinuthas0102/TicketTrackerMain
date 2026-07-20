/*
# Add "General Maintenance" request type to all active modules

1. Purpose
   - A new "General Maintenance" request type is needed so that employee-role
     users can be defaulted to it on the ticket creation form.
   - Currently the `requestTypes` JSONB array on each module's `config` column
     contains: Pre-Occupation Maintenance, Vacation Handover, Annual Maintenance,
     Emergency Maintenance.
   - This migration appends a new entry `General Maintenance`
     (requiresCEInspection = false) to the `requestTypes` array of every active
     module, only if it is not already present.

2. Modified Tables
   - `modules` — no schema changes; only the `config` JSONB column is updated
     for rows where `active = true`.

3. Security
   - No RLS policy changes. Existing policies on `modules` remain in effect.

4. Idempotency
   - The update checks whether `General Maintenance` already exists in the
     `requestTypes` array before appending, so re-running this migration is
     safe and will not create duplicate entries.
*/

UPDATE modules
SET config = jsonb_set(
  config,
  '{requestTypes}',
  (config -> 'requestTypes') || jsonb_build_array(
    jsonb_build_object(
      'label', 'General Maintenance',
      'value', 'General Maintenance',
      'requiresCEInspection', false
    )
  )
)
WHERE active = true
  AND NOT EXISTS (
    SELECT 1
    FROM jsonb_array_elements(config -> 'requestTypes') AS rt
    WHERE rt ->> 'value' = 'General Maintenance'
  );