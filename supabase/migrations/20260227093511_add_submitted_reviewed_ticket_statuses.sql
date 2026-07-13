/*
  # Add SUBMITTED and REVIEWED ticket statuses

  ## Summary
  This migration adds two new ticket status values to support a clearer submission and review workflow:
  - `submitted`: Set when a user submits a ticket (replaces automatic DRAFT state on creation)
  - `reviewed`: Set when an EO reviews a submitted ticket (explicit confirmation of review)

  ## Changes
  1. Modified Tables
    - `tickets`: Updated the `tickets_status_check` constraint to include the new statuses

  ## Notes
  - The existing constraint is dropped and recreated with the new values
  - All existing data is preserved; only the constraint is updated
  - New flow: User creates ticket → status = 'submitted' → EO reviews → status = 'reviewed'
*/

ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_status_check;

ALTER TABLE tickets
ADD CONSTRAINT tickets_status_check
CHECK (status IN (
  'draft', 'submitted', 'reviewed', 'created', 'approved', 'active',
  'sent_to_finance', 'approved_by_finance', 'rejected_by_finance',
  'completed', 'closed', 'cancelled'
));
