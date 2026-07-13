/*
  # Add Request Type, Step Type, and C&E Inspection Users

  ## Summary
  Adds the Request Type feature to the ticket tracker across all modules.

  ## Changes

  ### 1. tickets table
  - New column: `request_type` (text, nullable) - stores the selected request type
    Values: "Pre-Occupation Maintenance", "Vacation Handover", "Annual Maintenance", "Emergency Maintenance"

  ### 2. workflow_steps table
  - New column: `step_type` (text, nullable) - tags auto-generated inspection steps
    Values: `civil_inspection` or `electrical_inspection`; NULL for all regular steps

  ### 3. modules table - config update
  - All active modules get a `requestTypes` array added to their JSONB config
  - "Vacation Handover" and "Annual Maintenance" have `requiresCEInspection: true`

  ### 4. users table - new C&E inspection users
  - Civil Manager user (dept_officer, department: "Civil Manager")
  - Electrical Manager user (dept_officer, department: "Electrical Manager")
*/

-- 1. Add request_type to tickets
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'tickets' AND column_name = 'request_type'
  ) THEN
    ALTER TABLE tickets ADD COLUMN request_type text;
  END IF;
END $$;

-- 2. Add step_type to workflow_steps
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'workflow_steps' AND column_name = 'step_type'
  ) THEN
    ALTER TABLE workflow_steps ADD COLUMN step_type text;
  END IF;
END $$;

-- 3. Update all active module configs to include requestTypes
UPDATE modules
SET config = config || jsonb_build_object(
  'requestTypes', jsonb_build_array(
    jsonb_build_object('label', 'Pre-Occupation Maintenance', 'value', 'Pre-Occupation Maintenance', 'requiresCEInspection', false),
    jsonb_build_object('label', 'Vacation Handover', 'value', 'Vacation Handover', 'requiresCEInspection', true),
    jsonb_build_object('label', 'Annual Maintenance', 'value', 'Annual Maintenance', 'requiresCEInspection', true),
    jsonb_build_object('label', 'Emergency Maintenance', 'value', 'Emergency Maintenance', 'requiresCEInspection', false)
  )
)
WHERE active = true;

-- 4. Insert Civil Manager user if not exists
INSERT INTO users (id, email, name, role, department, active)
SELECT
  gen_random_uuid(),
  'civil.manager@tickettracker.com',
  'Civil Manager',
  'dept_officer',
  'Civil Manager',
  true
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE department = 'Civil Manager'
);

-- 5. Insert Electrical Manager user if not exists
INSERT INTO users (id, email, name, role, department, active)
SELECT
  gen_random_uuid(),
  'electrical.manager@tickettracker.com',
  'Electrical Manager',
  'dept_officer',
  'Electrical Manager',
  true
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE department = 'Electrical Manager'
);
