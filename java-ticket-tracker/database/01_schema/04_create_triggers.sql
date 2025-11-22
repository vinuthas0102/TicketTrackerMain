/*
  Oracle Database Triggers for Ticket Tracker System

  Triggers for:
  1. Auto-generating UUIDs for primary keys
  2. Auto-updating updated_at timestamps
  3. Auto-generating ticket numbers
*/

-- ============================================================================
-- BEFORE INSERT TRIGGERS FOR UUID GENERATION
-- ============================================================================

-- Users table
CREATE OR REPLACE TRIGGER trg_users_before_insert
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Modules table
CREATE OR REPLACE TRIGGER trg_modules_before_insert
BEFORE INSERT ON modules
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Tickets table
CREATE OR REPLACE TRIGGER trg_tickets_before_insert
BEFORE INSERT ON tickets
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
    IF :NEW.ticket_number IS NULL THEN
        :NEW.ticket_number := 'TKT-' || TO_CHAR(seq_ticket_number.NEXTVAL, 'FM000000');
    END IF;
END;
/

-- Workflow steps table
CREATE OR REPLACE TRIGGER trg_workflow_steps_before_insert
BEFORE INSERT ON workflow_steps
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Workflow comments table
CREATE OR REPLACE TRIGGER trg_workflow_comments_before_insert
BEFORE INSERT ON workflow_comments
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Documents table
CREATE OR REPLACE TRIGGER trg_documents_before_insert
BEFORE INSERT ON documents
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- File attachments table
CREATE OR REPLACE TRIGGER trg_file_attachments_before_insert
BEFORE INSERT ON file_attachments
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Audit logs table
CREATE OR REPLACE TRIGGER trg_audit_logs_before_insert
BEFORE INSERT ON audit_logs
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Field definitions table
CREATE OR REPLACE TRIGGER trg_field_definitions_before_insert
BEFORE INSERT ON field_definitions
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Module field configurations table
CREATE OR REPLACE TRIGGER trg_module_field_configs_before_insert
BEFORE INSERT ON module_field_configurations
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Field dropdown options table
CREATE OR REPLACE TRIGGER trg_dropdown_options_before_insert
BEFORE INSERT ON field_dropdown_options
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Ticket field values table
CREATE OR REPLACE TRIGGER trg_ticket_field_values_before_insert
BEFORE INSERT ON ticket_field_values
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Workflow step field values table
CREATE OR REPLACE TRIGGER trg_step_field_values_before_insert
BEFORE INSERT ON workflow_step_field_values
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Workflow step dependencies table
CREATE OR REPLACE TRIGGER trg_step_dependencies_before_insert
BEFORE INSERT ON workflow_step_dependencies
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- File reference templates table
CREATE OR REPLACE TRIGGER trg_file_ref_templates_before_insert
BEFORE INSERT ON file_reference_templates
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- File references table
CREATE OR REPLACE TRIGGER trg_file_references_before_insert
BEFORE INSERT ON file_references
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Progress documents table
CREATE OR REPLACE TRIGGER trg_progress_documents_before_insert
BEFORE INSERT ON workflow_step_progress_documents
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- Finance approval workflow table
CREATE OR REPLACE TRIGGER trg_finance_approval_before_insert
BEFORE INSERT ON finance_approval_workflow
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- User display preferences table
CREATE OR REPLACE TRIGGER trg_user_display_pref_before_insert
BEFORE INSERT ON user_display_preferences
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- User roles table
CREATE OR REPLACE TRIGGER trg_user_roles_before_insert
BEFORE INSERT ON user_roles
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := generate_uuid();
    END IF;
END;
/

-- ============================================================================
-- BEFORE UPDATE TRIGGERS FOR UPDATED_AT TIMESTAMP
-- ============================================================================

-- Users table
CREATE OR REPLACE TRIGGER trg_users_before_update
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Modules table
CREATE OR REPLACE TRIGGER trg_modules_before_update
BEFORE UPDATE ON modules
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Tickets table
CREATE OR REPLACE TRIGGER trg_tickets_before_update
BEFORE UPDATE ON tickets
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Workflow steps table
CREATE OR REPLACE TRIGGER trg_workflow_steps_before_update
BEFORE UPDATE ON workflow_steps
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
    IF :NEW.status = 'completed' AND :OLD.status != 'completed' THEN
        :NEW.completed_at := SYSTIMESTAMP;
    END IF;
END;
/

-- Field definitions table
CREATE OR REPLACE TRIGGER trg_field_definitions_before_update
BEFORE UPDATE ON field_definitions
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Module field configurations table
CREATE OR REPLACE TRIGGER trg_module_field_configs_before_update
BEFORE UPDATE ON module_field_configurations
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Ticket field values table
CREATE OR REPLACE TRIGGER trg_ticket_field_values_before_update
BEFORE UPDATE ON ticket_field_values
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Workflow step field values table
CREATE OR REPLACE TRIGGER trg_step_field_values_before_update
BEFORE UPDATE ON workflow_step_field_values
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- File reference templates table
CREATE OR REPLACE TRIGGER trg_file_ref_templates_before_update
BEFORE UPDATE ON file_reference_templates
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- File references table
CREATE OR REPLACE TRIGGER trg_file_references_before_update
BEFORE UPDATE ON file_references
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Finance approval workflow table
CREATE OR REPLACE TRIGGER trg_finance_approval_before_update
BEFORE UPDATE ON finance_approval_workflow
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- User display preferences table
CREATE OR REPLACE TRIGGER trg_user_display_pref_before_update
BEFORE UPDATE ON user_display_preferences
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

COMMIT;

-- Verify trigger creation
SELECT trigger_name, table_name, triggering_event FROM user_triggers ORDER BY table_name, trigger_name;
