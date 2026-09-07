-- Add due_date_change_reason column to workflow_steps table
-- Stores the reason provided by a technician when changing the due date
-- (required when Ticket Closure by Technician flag is enabled)

ALTER TABLE workflow_steps ADD COLUMN IF NOT EXISTS due_date_change_reason text;
