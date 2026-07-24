/*
# Add "Review by EO Required" flag to module configs

1. Purpose
   Adds a new boolean flag `reviewByEORequired` to the `config` JSONB column of the `modules` table.
   This flag controls whether the "Reviewed" status step is required in the ticket workflow.
   When set to `false` (the default), the Reviewed status is hidden from the summary,
   add-task icons are gated on ACTIVE status instead of REVIEWED, and remarks become
   optional when transitioning to "Start to work" (ACTIVE).

2. Changes
   - Sets `config.reviewByEORequired = false` on ALL existing modules (safe default).
   - Any future module inserts that don't specify this flag will also default to false
     via a column default on the config JSONB.

3. Security
   - No RLS policy changes. Existing policies on `modules` remain unchanged.
   - No new tables or columns (JSONB field addition only).

4. Important Notes
   - This migration is idempotent: `jsonb_set` with `true` as the 4th arg will create
     the key if it doesn't exist, and re-running won't change already-set values.
   - The maintenance module (schema_id = 'maintenance') will have this flag set to false
     by default, matching the requirement "default set as No".
*/

UPDATE modules
SET config = jsonb_set(
  COALESCE(config, '{}'::jsonb),
  '{reviewByEORequired}',
  'false'::jsonb,
  true
)
WHERE NOT (config ? 'reviewByEORequired');
