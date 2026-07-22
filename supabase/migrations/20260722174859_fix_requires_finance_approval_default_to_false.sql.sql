/*
# Fix requires_finance_approval NULL values to false (default NO)

## Problem
The `tickets.requires_finance_approval` column allows NULL values.
Frontend code was using `!== false` checks, which treated NULL as "approval required",
blocking status changes even when the flag was intended to default to NO.

## Fix
1. Set all NULL values in `tickets.requires_finance_approval` to `false`.
2. Set the column default to `false` so future inserts without an explicit value get `false`.
3. Add a NOT NULL constraint to prevent future NULL values.

## Tables Modified
- `tickets`: `requires_finance_approval` column backfilled to `false`, default set to `false`, NOT NULL constraint added.

## Security
- No RLS policy changes. This is a data integrity fix only.

## Important Notes
1. This migration is idempotent — safe to re-run.
2. No data is lost; NULL values are replaced with the intended default (`false`).
3. The column already exists; this migration only updates existing data and constraints.
*/

-- Step 1: Backfill all NULL values to false
UPDATE tickets
SET requires_finance_approval = false
WHERE requires_finance_approval IS NULL;

-- Step 2: Set column default to false
ALTER TABLE tickets
ALTER COLUMN requires_finance_approval SET DEFAULT false;

-- Step 3: Enforce NOT NULL constraint
ALTER TABLE tickets
ALTER COLUMN requires_finance_approval SET NOT NULL;
