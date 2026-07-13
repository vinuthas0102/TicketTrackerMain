/*
  # Add Unique Constraint on Workflow Step Numbers

  ## Summary
  Prevents duplicate task numbers from being created within the same ticket.

  ## Problem
  Users were able to create subtasks or sub-subtasks with the same number 
  (e.g., two tasks both numbered "1.0.0" or two subtasks both numbered "1.1.0").
  There was no database-level enforcement preventing this.

  ## Changes

  ### Modified Tables
  - `workflow_steps`
    - Added UNIQUE constraint on `(ticket_id, level_1, level_2, level_3)`
    - This ensures that within a single ticket, no two workflow steps can share
      the same hierarchical position (combination of all three level values)

  ## Security
  - No RLS changes

  ## Important Notes
  1. This migration first removes any existing duplicates by keeping only the
     earliest created row for each duplicate group before adding the constraint.
  2. The constraint applies per-ticket, so the same level combination can exist
     across different tickets.
*/

DO $$
BEGIN
  -- Remove duplicate step numbers within the same ticket, keeping the oldest (by created_at)
  DELETE FROM workflow_steps
  WHERE id IN (
    SELECT id FROM (
      SELECT 
        id,
        ROW_NUMBER() OVER (
          PARTITION BY ticket_id, level_1, level_2, level_3 
          ORDER BY created_at ASC
        ) AS rn
      FROM workflow_steps
      WHERE level_1 IS NOT NULL AND level_2 IS NOT NULL AND level_3 IS NOT NULL
    ) ranked
    WHERE rn > 1
  );
END $$;

ALTER TABLE workflow_steps
  DROP CONSTRAINT IF EXISTS workflow_steps_unique_step_number;

ALTER TABLE workflow_steps
  ADD CONSTRAINT workflow_steps_unique_step_number
  UNIQUE (ticket_id, level_1, level_2, level_3);
