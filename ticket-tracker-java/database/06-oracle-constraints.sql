/*
  Oracle Foreign Key Constraints

  This script adds foreign key constraints to enforce referential integrity.
  Run this after loading seed data to avoid constraint violations.

  Run this script after 05-oracle-indexes.sql
*/

-- ==================================================================================
-- TICKETS Table Foreign Keys
-- ==================================================================================
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_module_id
  FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE;

ALTER TABLE tickets ADD CONSTRAINT fk_tickets_created_by
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE tickets ADD CONSTRAINT fk_tickets_assigned_to
  FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE tickets ADD CONSTRAINT fk_tickets_finance_officer_id
  FOREIGN KEY (finance_officer_id) REFERENCES users(id) ON DELETE SET NULL;

-- ==================================================================================
-- WORKFLOW_STEPS Table Foreign Keys
-- ==================================================================================
ALTER TABLE workflow_steps ADD CONSTRAINT fk_workflow_steps_ticket_id
  FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE workflow_steps ADD CONSTRAINT fk_workflow_steps_assigned_to
  FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE workflow_steps ADD CONSTRAINT fk_workflow_steps_parent_step_id
  FOREIGN KEY (parent_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE workflow_steps ADD CONSTRAINT fk_workflow_steps_created_by
  FOREIGN KEY (created_by) REFERENCES users(id);

-- ==================================================================================
-- WORKFLOW_COMMENTS Table Foreign Keys
-- ==================================================================================
ALTER TABLE workflow_comments ADD CONSTRAINT fk_workflow_comments_step_id
  FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE workflow_comments ADD CONSTRAINT fk_workflow_comments_created_by
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

-- ==================================================================================
-- DOCUMENTS Table Foreign Keys
-- ==================================================================================
ALTER TABLE documents ADD CONSTRAINT fk_documents_ticket_id
  FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE documents ADD CONSTRAINT fk_documents_step_id
  FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE documents ADD CONSTRAINT fk_documents_uploaded_by
  FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE;

-- ==================================================================================
-- FILE_ATTACHMENTS Table Foreign Keys
-- ==================================================================================
ALTER TABLE file_attachments ADD CONSTRAINT fk_file_attachments_ticket_id
  FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE file_attachments ADD CONSTRAINT fk_file_attachments_step_id
  FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE file_attachments ADD CONSTRAINT fk_file_attachments_uploaded_by
  FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE;

-- ==================================================================================
-- AUDIT_LOGS Table Foreign Keys
-- ==================================================================================
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_ticket_id
  FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_step_id
  FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_performed_by
  FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE CASCADE;

-- ==================================================================================
-- MODULE_FIELD_CONFIGURATIONS Table Foreign Keys
-- ==================================================================================
ALTER TABLE module_field_configurations ADD CONSTRAINT fk_module_field_configs_module_id
  FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE;

-- ==================================================================================
-- FIELD_DROPDOWN_OPTIONS Table Foreign Keys
-- ==================================================================================
ALTER TABLE field_dropdown_options ADD CONSTRAINT fk_dropdown_options_field_config_id
  FOREIGN KEY (field_config_id) REFERENCES module_field_configurations(id) ON DELETE CASCADE;

-- ==================================================================================
-- TICKET_FIELD_VALUES Table Foreign Keys
-- ==================================================================================
ALTER TABLE ticket_field_values ADD CONSTRAINT fk_ticket_field_values_ticket_id
  FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

-- ==================================================================================
-- WORKFLOW_STEP_FIELD_VALUES Table Foreign Keys
-- ==================================================================================
ALTER TABLE workflow_step_field_values ADD CONSTRAINT fk_workflow_step_field_values_step_id
  FOREIGN KEY (workflow_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

-- ==================================================================================
-- WORKFLOW_STEP_DEPENDENCIES Table Foreign Keys
-- ==================================================================================
ALTER TABLE workflow_step_dependencies ADD CONSTRAINT fk_step_dependencies_step_id
  FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE workflow_step_dependencies ADD CONSTRAINT fk_step_dependencies_depends_on
  FOREIGN KEY (depends_on_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE workflow_step_dependencies ADD CONSTRAINT fk_step_dependencies_created_by
  FOREIGN KEY (created_by) REFERENCES users(id);

-- ==================================================================================
-- WORKFLOW_STEP_PROGRESS_DOCUMENTS Table Foreign Keys
-- ==================================================================================
ALTER TABLE workflow_step_progress_documents ADD CONSTRAINT fk_progress_docs_step_id
  FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE workflow_step_progress_documents ADD CONSTRAINT fk_progress_docs_ticket_id
  FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE workflow_step_progress_documents ADD CONSTRAINT fk_progress_docs_audit_log_id
  FOREIGN KEY (audit_log_id) REFERENCES audit_logs(id) ON DELETE SET NULL;

ALTER TABLE workflow_step_progress_documents ADD CONSTRAINT fk_progress_docs_uploaded_by
  FOREIGN KEY (uploaded_by) REFERENCES users(id);

ALTER TABLE workflow_step_progress_documents ADD CONSTRAINT fk_progress_docs_deleted_by
  FOREIGN KEY (deleted_by) REFERENCES users(id);

-- ==================================================================================
-- FILE_REFERENCE_TEMPLATES Table Foreign Keys
-- ==================================================================================
ALTER TABLE file_reference_templates ADD CONSTRAINT fk_file_ref_templates_uploaded_by
  FOREIGN KEY (uploaded_by) REFERENCES users(id);

-- ==================================================================================
-- WORKFLOW_STEP_FILE_REFERENCES Table Foreign Keys
-- ==================================================================================
ALTER TABLE workflow_step_file_references ADD CONSTRAINT fk_step_file_refs_step_id
  FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE;

ALTER TABLE workflow_step_file_references ADD CONSTRAINT fk_step_file_refs_template_id
  FOREIGN KEY (template_id) REFERENCES file_reference_templates(id);

ALTER TABLE workflow_step_file_references ADD CONSTRAINT fk_step_file_refs_document_id
  FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL;

ALTER TABLE workflow_step_file_references ADD CONSTRAINT fk_step_file_refs_uploaded_by
  FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL;

-- ==================================================================================
-- FINANCE_APPROVALS Table Foreign Keys
-- ==================================================================================
ALTER TABLE finance_approvals ADD CONSTRAINT fk_finance_approvals_ticket_id
  FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE finance_approvals ADD CONSTRAINT fk_finance_approvals_officer_id
  FOREIGN KEY (finance_officer_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE finance_approvals ADD CONSTRAINT fk_finance_approvals_submitted_by
  FOREIGN KEY (submitted_by) REFERENCES users(id) ON DELETE CASCADE;

-- ==================================================================================
-- USER_DISPLAY_PREFERENCES Table Foreign Keys
-- ==================================================================================
ALTER TABLE user_display_preferences ADD CONSTRAINT fk_user_prefs_user_id
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ==================================================================================
-- FILE_UPLOAD_CONFIG Table Foreign Keys
-- ==================================================================================
ALTER TABLE file_upload_config ADD CONSTRAINT fk_file_upload_config_module_id
  FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE;

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Foreign key constraints created successfully!' FROM DUAL;
SELECT 'Total constraints: 37' FROM DUAL;
SELECT 'Next step: Run 07-oracle-seed-data.sql' FROM DUAL;
