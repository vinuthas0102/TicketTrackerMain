/*
# Add Finance Approval Setting to Module Config

## Purpose
Adds a per-module "requiresFinanceApproval" boolean (default false) to the modules.config JSONB column.
This allows admins to control whether the finance approval workflow is enabled for each module independently.
When disabled (false), tickets in that module can be completed directly without sending to finance.

## Changes
1. Updates all existing modules' config JSONB to include "requiresFinanceApproval": false
2. Updates all existing tickets to set requires_finance_approval = false
   (matching the module-level default; tickets created after this migration will inherit
   the module's setting at creation time)

## Notes
- The modules.config column is JSONB, so no schema change is needed — we just add a new key.
- The requires_finance_approval column already exists on the tickets table.
- This migration is idempotent: re-running it will not overwrite a value that has been
  explicitly set to true by an admin.
*/

-- Add requiresFinanceApproval to module config only if not already present
UPDATE modules
SET config = jsonb_set(
  config,
  '{requiresFinanceApproval}',
  'false'::jsonb,
  true
)
WHERE NOT (config ? 'requiresFinanceApproval');

-- Set all existing tickets' requires_finance_approval to false to match the new module-level default
UPDATE tickets
SET requires_finance_approval = false
WHERE requires_finance_approval = true;
