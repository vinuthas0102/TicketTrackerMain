/*
  # Fix requires_finance_approval Column Defaults and NULL Values

  1. Changes
    - Update all NULL requires_finance_approval values to true
    - Update all ACTIVE tickets to require finance approval
    - Add NOT NULL constraint with DEFAULT true
    - Add index for better query performance

  2. Rationale
    - All tickets should require finance approval by default
    - Ensures consistency across all tickets
    - Fixes missing "Send to Finance" option in status dropdown

  3. Notes
    - Safe to run multiple times (idempotent)
    - No data loss
    - All existing tickets will require finance approval unless explicitly disabled
*/

-- Step 1: Update all NULL values to true
UPDATE tickets
SET requires_finance_approval = true
WHERE requires_finance_approval IS NULL;

-- Step 2: Ensure all ACTIVE tickets have finance approval requirement enabled
UPDATE tickets
SET requires_finance_approval = true
WHERE status = 'ACTIVE' AND (requires_finance_approval IS NULL OR requires_finance_approval = false);

-- Step 3: Set default value for the column if not already set
ALTER TABLE tickets
ALTER COLUMN requires_finance_approval SET DEFAULT true;

-- Step 4: Add NOT NULL constraint (after ensuring no NULLs exist)
ALTER TABLE tickets
ALTER COLUMN requires_finance_approval SET NOT NULL;

-- Step 5: Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_tickets_requires_finance_approval
ON tickets(requires_finance_approval);

-- Step 6: Create index for common query pattern (status + finance approval)
CREATE INDEX IF NOT EXISTS idx_tickets_status_finance
ON tickets(status, requires_finance_approval);
