/*
  Oracle Triggers for Automatic Field Updates

  This script creates triggers for:
  - Automatic updated_at timestamp updates
  - Business logic enforcement
  - Audit trail automation

  Run this script after 03-oracle-sequences.sql
*/

-- ==================================================================================
-- Trigger: Auto-update updated_at for USERS
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for MODULES
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_modules_updated_at
BEFORE UPDATE ON modules
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for TICKETS
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_tickets_updated_at
BEFORE UPDATE ON tickets
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for WORKFLOW_STEPS
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_workflow_steps_updated_at
BEFORE UPDATE ON workflow_steps
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Set completed_at when status changes to completed
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_workflow_steps_completed_at
BEFORE UPDATE OF status ON workflow_steps
FOR EACH ROW
WHEN (NEW.status = 'completed' AND OLD.status != 'completed')
BEGIN
  :NEW.completed_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for FIELD_DEFINITIONS
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_field_definitions_updated_at
BEFORE UPDATE ON field_definitions
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for MODULE_FIELD_CONFIGURATIONS
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_module_field_configs_updated_at
BEFORE UPDATE ON module_field_configurations
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for TICKET_FIELD_VALUES
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_ticket_field_values_updated_at
BEFORE UPDATE ON ticket_field_values
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for WORKFLOW_STEP_FIELD_VALUES
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_workflow_step_field_values_updated_at
BEFORE UPDATE ON workflow_step_field_values
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for WORKFLOW_STEP_PROGRESS_DOCUMENTS
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_progress_documents_updated_at
BEFORE UPDATE ON workflow_step_progress_documents
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for FILE_REFERENCE_TEMPLATES
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_file_ref_templates_updated_at
BEFORE UPDATE ON file_reference_templates
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for WORKFLOW_STEP_FILE_REFERENCES
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_step_file_refs_updated_at
BEFORE UPDATE ON workflow_step_file_references
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for FINANCE_APPROVALS
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_finance_approvals_updated_at
BEFORE UPDATE ON finance_approvals
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Set decided_at when finance approval status changes from pending
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_finance_approvals_decided_at
BEFORE UPDATE OF status ON finance_approvals
FOR EACH ROW
WHEN (NEW.status IN ('approved', 'rejected') AND OLD.status = 'pending')
BEGIN
  :NEW.decided_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for USER_DISPLAY_PREFERENCES
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_user_prefs_updated_at
BEFORE UPDATE ON user_display_preferences
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Trigger: Auto-update updated_at for FILE_UPLOAD_CONFIG
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_file_upload_config_updated_at
BEFORE UPDATE ON file_upload_config
FOR EACH ROW
BEGIN
  :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ==================================================================================
-- Business Logic Trigger: Increment finance submission count
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_finance_submission_increment
AFTER INSERT ON finance_approvals
FOR EACH ROW
BEGIN
  UPDATE tickets
  SET finance_submission_count = finance_submission_count + 1,
      latest_finance_status = :NEW.status
  WHERE id = :NEW.ticket_id;
END;
/

-- ==================================================================================
-- Business Logic Trigger: Update ticket finance status on approval decision
-- ==================================================================================
CREATE OR REPLACE TRIGGER trg_finance_approval_status_update
AFTER UPDATE OF status ON finance_approvals
FOR EACH ROW
WHEN (NEW.status IN ('approved', 'rejected'))
BEGIN
  UPDATE tickets
  SET latest_finance_status = :NEW.status
  WHERE id = :NEW.ticket_id;
END;
/

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Triggers created successfully!' FROM DUAL;
SELECT 'Total triggers: 18' FROM DUAL;
SELECT 'Next step: Run 05-oracle-indexes.sql' FROM DUAL;
