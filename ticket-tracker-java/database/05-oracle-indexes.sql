/*
  Oracle Database Indexes for Performance Optimization

  This script creates indexes on frequently queried columns and foreign keys.
  Indexes improve query performance but add overhead to INSERT/UPDATE operations.

  Run this script after 04-oracle-triggers.sql
*/

-- ==================================================================================
-- USERS Table Indexes
-- ==================================================================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_department ON users(department);
CREATE INDEX idx_users_active ON users(active);
CREATE INDEX idx_users_last_login ON users(last_login);
CREATE INDEX idx_users_role_finance ON users(role) WHERE role = 'finance';

-- ==================================================================================
-- MODULES Table Indexes
-- ==================================================================================
CREATE INDEX idx_modules_active ON modules(active);
CREATE INDEX idx_modules_schema_id ON modules(schema_id);

-- ==================================================================================
-- TICKETS Table Indexes
-- ==================================================================================
CREATE INDEX idx_tickets_module_id ON tickets(module_id);
CREATE INDEX idx_tickets_created_by ON tickets(created_by);
CREATE INDEX idx_tickets_assigned_to ON tickets(assigned_to);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_property_id ON tickets(property_id);
CREATE INDEX idx_tickets_property_location ON tickets(property_location);
CREATE INDEX idx_tickets_finance_officer ON tickets(finance_officer_id);
CREATE INDEX idx_tickets_finance_status ON tickets(latest_finance_status);
CREATE INDEX idx_tickets_requires_finance ON tickets(requires_finance_approval);
CREATE INDEX idx_tickets_created_at ON tickets(created_at DESC);
CREATE INDEX idx_tickets_due_date ON tickets(due_date);

-- Composite indexes for common query patterns
CREATE INDEX idx_tickets_status_module ON tickets(status, module_id);
CREATE INDEX idx_tickets_assigned_status ON tickets(assigned_to, status);

-- ==================================================================================
-- WORKFLOW_STEPS Table Indexes
-- ==================================================================================
CREATE INDEX idx_workflow_steps_ticket_id ON workflow_steps(ticket_id);
CREATE INDEX idx_workflow_steps_assigned_to ON workflow_steps(assigned_to);
CREATE INDEX idx_workflow_steps_parent_step_id ON workflow_steps(parent_step_id);
CREATE INDEX idx_workflow_steps_ticket_parent ON workflow_steps(ticket_id, parent_step_id);
CREATE INDEX idx_workflow_steps_status ON workflow_steps(status);
CREATE INDEX idx_workflow_steps_created_by ON workflow_steps(created_by);
CREATE INDEX idx_workflow_steps_due_date ON workflow_steps(due_date);

-- Composite indexes for hierarchical queries
CREATE INDEX idx_workflow_steps_levels ON workflow_steps(level_1, level_2, level_3);

-- Composite index for ticket assignment lookups (optimizes EXISTS subqueries)
CREATE INDEX idx_workflow_steps_ticket_assigned ON workflow_steps(ticket_id, assigned_to);

-- ==================================================================================
-- WORKFLOW_COMMENTS Table Indexes
-- ==================================================================================
CREATE INDEX idx_workflow_comments_step_id ON workflow_comments(step_id);
CREATE INDEX idx_workflow_comments_created_by ON workflow_comments(created_by);
CREATE INDEX idx_workflow_comments_created_at ON workflow_comments(created_at DESC);

-- ==================================================================================
-- DOCUMENTS Table Indexes
-- ==================================================================================
CREATE INDEX idx_documents_ticket_id ON documents(ticket_id);
CREATE INDEX idx_documents_step_id ON documents(step_id);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_is_mandatory ON documents(is_mandatory);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at DESC);

-- ==================================================================================
-- FILE_ATTACHMENTS Table Indexes
-- ==================================================================================
CREATE INDEX idx_file_attachments_ticket_id ON file_attachments(ticket_id);
CREATE INDEX idx_file_attachments_step_id ON file_attachments(step_id);
CREATE INDEX idx_file_attachments_uploaded_by ON file_attachments(uploaded_by);

-- ==================================================================================
-- AUDIT_LOGS Table Indexes
-- ==================================================================================
CREATE INDEX idx_audit_logs_ticket_id ON audit_logs(ticket_id);
CREATE INDEX idx_audit_logs_step_id ON audit_logs(step_id);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_logs_action_category ON audit_logs(action_category);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at DESC);

-- Composite indexes for audit queries
CREATE INDEX idx_audit_logs_ticket_step ON audit_logs(ticket_id, step_id);
CREATE INDEX idx_audit_logs_user_date ON audit_logs(performed_by, performed_at DESC);

-- ==================================================================================
-- MODULE_FIELD_CONFIGURATIONS Table Indexes
-- ==================================================================================
CREATE INDEX idx_module_field_configs_module ON module_field_configurations(module_id, context);
CREATE INDEX idx_module_field_configs_order ON module_field_configurations(module_id, context, display_order);
CREATE INDEX idx_module_field_configs_visible ON module_field_configurations(is_visible);

-- ==================================================================================
-- FIELD_DROPDOWN_OPTIONS Table Indexes
-- ==================================================================================
CREATE INDEX idx_dropdown_options_field ON field_dropdown_options(field_config_id, display_order);
CREATE INDEX idx_dropdown_options_active ON field_dropdown_options(is_active);

-- ==================================================================================
-- TICKET_FIELD_VALUES Table Indexes
-- ==================================================================================
CREATE INDEX idx_ticket_field_values_ticket ON ticket_field_values(ticket_id);
CREATE INDEX idx_ticket_field_values_field_key ON ticket_field_values(field_key);

-- ==================================================================================
-- WORKFLOW_STEP_FIELD_VALUES Table Indexes
-- ==================================================================================
CREATE INDEX idx_workflow_step_field_values_step ON workflow_step_field_values(workflow_step_id);
CREATE INDEX idx_workflow_step_field_values_key ON workflow_step_field_values(field_key);

-- ==================================================================================
-- WORKFLOW_STEP_DEPENDENCIES Table Indexes
-- ==================================================================================
CREATE INDEX idx_workflow_step_deps_step_id ON workflow_step_dependencies(step_id);
CREATE INDEX idx_workflow_step_deps_depends_on ON workflow_step_dependencies(depends_on_step_id);
CREATE INDEX idx_workflow_step_deps_active ON workflow_step_dependencies(step_id, is_active);
CREATE INDEX idx_workflow_step_deps_created_by ON workflow_step_dependencies(created_by);

-- ==================================================================================
-- WORKFLOW_STEP_PROGRESS_DOCUMENTS Table Indexes
-- ==================================================================================
CREATE INDEX idx_progress_docs_step_id ON workflow_step_progress_documents(step_id);
CREATE INDEX idx_progress_docs_ticket_id ON workflow_step_progress_documents(ticket_id);
CREATE INDEX idx_progress_docs_audit_log_id ON workflow_step_progress_documents(audit_log_id);
CREATE INDEX idx_progress_docs_is_deleted ON workflow_step_progress_documents(step_id, is_deleted);
CREATE INDEX idx_progress_docs_uploaded_by ON workflow_step_progress_documents(uploaded_by);
CREATE INDEX idx_progress_docs_uploaded_at ON workflow_step_progress_documents(uploaded_at DESC);

-- ==================================================================================
-- FILE_REFERENCE_TEMPLATES Table Indexes
-- ==================================================================================
CREATE INDEX idx_file_ref_templates_name ON file_reference_templates(template_name);
CREATE INDEX idx_file_ref_templates_uploaded_by ON file_reference_templates(uploaded_by);
CREATE INDEX idx_file_ref_templates_active ON file_reference_templates(is_active);
CREATE INDEX idx_file_ref_templates_created_at ON file_reference_templates(created_at DESC);

-- ==================================================================================
-- WORKFLOW_STEP_FILE_REFERENCES Table Indexes
-- ==================================================================================
CREATE INDEX idx_step_file_refs_step_id ON workflow_step_file_references(step_id);
CREATE INDEX idx_step_file_refs_template_id ON workflow_step_file_references(template_id);
CREATE INDEX idx_step_file_refs_document_id ON workflow_step_file_references(document_id);
CREATE INDEX idx_step_file_refs_uploaded_by ON workflow_step_file_references(uploaded_by);
CREATE INDEX idx_step_file_refs_composite ON workflow_step_file_references(step_id, template_id);
CREATE INDEX idx_step_file_refs_mandatory ON workflow_step_file_references(step_id, is_mandatory);

-- ==================================================================================
-- FINANCE_APPROVALS Table Indexes
-- ==================================================================================
CREATE INDEX idx_finance_approvals_ticket ON finance_approvals(ticket_id);
CREATE INDEX idx_finance_approvals_officer ON finance_approvals(finance_officer_id);
CREATE INDEX idx_finance_approvals_status ON finance_approvals(status);
CREATE INDEX idx_finance_approvals_submitted_at ON finance_approvals(submitted_at DESC);
CREATE INDEX idx_finance_approvals_submitted_by ON finance_approvals(submitted_by);
CREATE INDEX idx_finance_approvals_decided_at ON finance_approvals(decided_at DESC);

-- Composite index for pending approvals by officer
CREATE INDEX idx_finance_approvals_pending ON finance_approvals(finance_officer_id, status);

-- ==================================================================================
-- USER_DISPLAY_PREFERENCES Table Indexes
-- ==================================================================================
-- No additional indexes needed (user_id is unique key and primary lookups)

-- ==================================================================================
-- FILE_UPLOAD_CONFIG Table Indexes
-- ==================================================================================
CREATE INDEX idx_file_upload_config_field_key ON file_upload_config(field_key);
CREATE INDEX idx_file_upload_config_module_id ON file_upload_config(module_id);

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Indexes created successfully!' FROM DUAL;
SELECT 'Total indexes: 70+' FROM DUAL;
SELECT 'Next step: Run 06-oracle-constraints.sql' FROM DUAL;
