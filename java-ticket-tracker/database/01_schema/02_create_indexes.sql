/*
  Oracle Database Indexes for Ticket Tracker System

  Performance-optimized indexes for all tables
*/

-- Users table indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_department ON users(department);
CREATE INDEX idx_users_active ON users(active);
CREATE INDEX idx_users_last_login ON users(last_login);

-- Modules table indexes
CREATE INDEX idx_modules_active ON modules(active);
CREATE INDEX idx_modules_schema_id ON modules(schema_id);

-- Tickets table indexes
CREATE INDEX idx_tickets_module_id ON tickets(module_id);
CREATE INDEX idx_tickets_created_by ON tickets(created_by);
CREATE INDEX idx_tickets_assigned_to ON tickets(assigned_to);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_property_id ON tickets(property_id);
CREATE INDEX idx_tickets_property_location ON tickets(property_location);
CREATE INDEX idx_tickets_ticket_number ON tickets(ticket_number);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_tickets_due_date ON tickets(due_date);

-- Workflow steps table indexes
CREATE INDEX idx_workflow_steps_ticket_id ON workflow_steps(ticket_id);
CREATE INDEX idx_workflow_steps_assigned_to ON workflow_steps(assigned_to);
CREATE INDEX idx_workflow_steps_parent_step_id ON workflow_steps(parent_step_id);
CREATE INDEX idx_workflow_steps_status ON workflow_steps(status);
CREATE INDEX idx_workflow_steps_ticket_parent ON workflow_steps(ticket_id, parent_step_id);
CREATE INDEX idx_workflow_steps_ticket_status ON workflow_steps(ticket_id, status);

-- Workflow comments table indexes
CREATE INDEX idx_workflow_comments_step_id ON workflow_comments(step_id);
CREATE INDEX idx_workflow_comments_created_by ON workflow_comments(created_by);
CREATE INDEX idx_workflow_comments_created_at ON workflow_comments(created_at);

-- Documents table indexes
CREATE INDEX idx_documents_ticket_id ON documents(ticket_id);
CREATE INDEX idx_documents_step_id ON documents(step_id);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_is_mandatory ON documents(is_mandatory);
CREATE INDEX idx_documents_ticket_completion ON documents(ticket_id, is_completion_certificate) WHERE is_completion_certificate = 1;
CREATE INDEX idx_documents_step_completion ON documents(step_id, is_completion_certificate) WHERE is_completion_certificate = 1;

-- File attachments table indexes
CREATE INDEX idx_file_attachments_ticket_id ON file_attachments(ticket_id);
CREATE INDEX idx_file_attachments_step_id ON file_attachments(step_id);
CREATE INDEX idx_file_attachments_uploaded_by ON file_attachments(uploaded_by);

-- Audit logs table indexes
CREATE INDEX idx_audit_logs_ticket_id ON audit_logs(ticket_id);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- Field definitions table indexes
CREATE INDEX idx_field_definitions_field_key ON field_definitions(field_key);
CREATE INDEX idx_field_definitions_field_type ON field_definitions(field_type);

-- Module field configurations table indexes
CREATE INDEX idx_module_field_configs_module ON module_field_configurations(module_id, context);
CREATE INDEX idx_module_field_configs_order ON module_field_configurations(module_id, context, display_order);
CREATE INDEX idx_module_field_configs_visible ON module_field_configurations(module_id, is_visible);

-- Field dropdown options table indexes
CREATE INDEX idx_dropdown_options_field ON field_dropdown_options(field_config_id, display_order);
CREATE INDEX idx_dropdown_options_active ON field_dropdown_options(is_active);

-- Ticket field values table indexes
CREATE INDEX idx_ticket_field_values_ticket ON ticket_field_values(ticket_id);
CREATE INDEX idx_ticket_field_values_field_key ON ticket_field_values(field_key);

-- Workflow step field values table indexes
CREATE INDEX idx_workflow_step_field_values_step ON workflow_step_field_values(workflow_step_id);
CREATE INDEX idx_workflow_step_field_values_key ON workflow_step_field_values(field_key);

-- Workflow step dependencies table indexes
CREATE INDEX idx_step_dependencies_step_id ON workflow_step_dependencies(step_id);
CREATE INDEX idx_step_dependencies_depends_on ON workflow_step_dependencies(depends_on_step_id);
CREATE INDEX idx_step_dependencies_active ON workflow_step_dependencies(is_active);

-- File reference templates table indexes
CREATE INDEX idx_file_ref_templates_module ON file_reference_templates(module_id);
CREATE INDEX idx_file_ref_templates_order ON file_reference_templates(display_order);

-- File references table indexes
CREATE INDEX idx_file_references_step_id ON file_references(step_id);
CREATE INDEX idx_file_references_template_id ON file_references(template_id);
CREATE INDEX idx_file_references_status ON file_references(status);
CREATE INDEX idx_file_references_uploaded_by ON file_references(uploaded_by);

-- Progress documents table indexes
CREATE INDEX idx_progress_documents_step_id ON workflow_step_progress_documents(step_id);
CREATE INDEX idx_progress_documents_uploaded_by ON workflow_step_progress_documents(uploaded_by);
CREATE INDEX idx_progress_documents_uploaded_at ON workflow_step_progress_documents(uploaded_at);

-- Finance approval workflow table indexes
CREATE INDEX idx_finance_approval_ticket_id ON finance_approval_workflow(ticket_id);
CREATE INDEX idx_finance_approval_status ON finance_approval_workflow(status);
CREATE INDEX idx_finance_approval_submitted_by ON finance_approval_workflow(submitted_by);
CREATE INDEX idx_finance_approval_reviewed_by ON finance_approval_workflow(reviewed_by);

-- User display preferences table indexes
CREATE INDEX idx_user_display_pref_user ON user_display_preferences(user_id);
CREATE INDEX idx_user_display_pref_module ON user_display_preferences(module_id);

-- User roles table indexes
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_module_id ON user_roles(module_id);
CREATE INDEX idx_user_roles_active ON user_roles(active);

COMMIT;

-- Verify index creation
SELECT index_name, table_name FROM user_indexes ORDER BY table_name, index_name;
