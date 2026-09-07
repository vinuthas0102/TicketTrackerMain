-- Add due_date_change_reason column to workflow_steps table
-- This column stores the reason provided by a technician when changing the due date
-- (required when Ticket Closure by Technician flag is enabled)

ALTER TABLE workflow_steps ADD due_date_change_reason CLOB NULL;
