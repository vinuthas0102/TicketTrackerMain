/*
# Add remarks and actual_completed_at to workflow_steps

1. Purpose
- Add a "Remarks" field to workflow steps, primarily for sub-tasks, editable by Manager (DO) and EO roles.
- Add an "Actual Completed Date" field to workflow steps (tasks and sub-tasks) that is auto-filled when the step status transitions to COMPLETED.

2. Schema changes
- `workflow_steps.remarks` (text, nullable): free-form remarks for sub-tasks.
- `workflow_steps.actual_completed_at` (timestamptz, nullable): the date/time the step was actually marked completed; set automatically on completion.

3. Security
- No changes to RLS policies. Existing policies on workflow_steps continue to govern access.

4. Notes
- Both columns are nullable so existing rows are unaffected.
- `actual_completed_at` is distinct from the existing `completed_at` column; it represents the user-visible "Actual Completed Date" field. The application layer sets it on completion transitions.
*/

ALTER TABLE workflow_steps
  ADD COLUMN IF NOT EXISTS remarks text,
  ADD COLUMN IF NOT EXISTS actual_completed_at timestamptz;
