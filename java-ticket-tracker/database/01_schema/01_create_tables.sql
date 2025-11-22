/*
  Oracle Database Schema for Ticket Tracker System

  Converted from PostgreSQL (Supabase) to Oracle Database

  This script creates all 20+ tables required for the ticket tracking system.

  IMPORTANT DATA TYPE MAPPINGS:
  - PostgreSQL UUID → Oracle VARCHAR2(36) or RAW(16)
  - PostgreSQL text → Oracle VARCHAR2(4000) or CLOB
  - PostgreSQL jsonb → Oracle JSON (12c+) or CLOB
  - PostgreSQL boolean → Oracle NUMBER(1) (0=false, 1=true)
  - PostgreSQL timestamptz → Oracle TIMESTAMP WITH TIME ZONE
  - PostgreSQL array[] → Oracle VARCHAR2(4000) with comma-separated values or nested table

  Run this script as the ticket_tracker database user.
*/

-- ============================================================================
-- TABLE 1: USERS
-- User accounts with role-based access control
-- ============================================================================

CREATE TABLE users (
    id VARCHAR2(36) PRIMARY KEY,
    name VARCHAR2(200) NOT NULL,
    email VARCHAR2(200) UNIQUE NOT NULL,
    role VARCHAR2(50) NOT NULL CHECK (role IN ('employee', 'eo', 'dept_officer', 'vendor', 'finance_officer', 'admin')),
    department VARCHAR2(200) NOT NULL,
    password_hash VARCHAR2(255),
    avatar VARCHAR2(500),
    active NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP
);

COMMENT ON TABLE users IS 'User accounts with role-based access control';
COMMENT ON COLUMN users.active IS '0=inactive, 1=active';

-- ============================================================================
-- TABLE 2: MODULES
-- Workflow modules/categories
-- ============================================================================

CREATE TABLE modules (
    id VARCHAR2(36) PRIMARY KEY,
    name VARCHAR2(200) NOT NULL,
    description VARCHAR2(1000),
    icon VARCHAR2(100) DEFAULT 'FileText',
    color VARCHAR2(100) DEFAULT 'from-blue-500 to-indigo-500',
    schema_id VARCHAR2(100) NOT NULL,
    config CLOB,
    active NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT chk_modules_config CHECK (config IS JSON)
);

COMMENT ON TABLE modules IS 'Workflow modules and categories';

-- ============================================================================
-- TABLE 3: TICKETS
-- Main workflow instances/tickets
-- ============================================================================

CREATE TABLE tickets (
    id VARCHAR2(36) PRIMARY KEY,
    ticket_number VARCHAR2(50) UNIQUE NOT NULL,
    module_id VARCHAR2(36) NOT NULL,
    title VARCHAR2(500) NOT NULL,
    description CLOB NOT NULL,
    status VARCHAR2(50) DEFAULT 'open',
    priority VARCHAR2(50),
    created_by VARCHAR2(36) NOT NULL,
    assigned_to VARCHAR2(36),
    due_date TIMESTAMP WITH TIME ZONE,
    start_date TIMESTAMP WITH TIME ZONE,
    data CLOB,
    property_id VARCHAR2(100) DEFAULT 'PROP001',
    property_location VARCHAR2(200) DEFAULT 'Location01',
    completion_documents_required NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_tickets_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_tickets_data CHECK (data IS JSON)
);

COMMENT ON TABLE tickets IS 'Main workflow instances and tickets';

-- ============================================================================
-- TABLE 4: WORKFLOW_STEPS
-- Hierarchical workflow steps within tickets
-- ============================================================================

CREATE TABLE workflow_steps (
    id VARCHAR2(36) PRIMARY KEY,
    ticket_id VARCHAR2(36) NOT NULL,
    step_number VARCHAR2(50) NOT NULL,
    title VARCHAR2(500) NOT NULL,
    description CLOB,
    status VARCHAR2(50) DEFAULT 'pending',
    assigned_to VARCHAR2(36),
    created_by VARCHAR2(36),
    parent_step_id VARCHAR2(36),
    level_1 NUMBER(10),
    level_2 NUMBER(10),
    level_3 NUMBER(10),
    dependencies VARCHAR2(4000),
    is_parallel NUMBER(1) DEFAULT 0,
    dependency_mode VARCHAR2(20),
    is_dependency_locked NUMBER(1) DEFAULT 0,
    progress NUMBER(5,2) DEFAULT 0,
    mandatory_documents VARCHAR2(4000),
    optional_documents VARCHAR2(4000),
    completion_certificate_required NUMBER(1) DEFAULT 0,
    due_date TIMESTAMP WITH TIME ZONE,
    start_date TIMESTAMP WITH TIME ZONE,
    data CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_steps_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_steps_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_steps_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_steps_parent FOREIGN KEY (parent_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT chk_steps_no_self_parent CHECK (parent_step_id IS NULL OR parent_step_id != id),
    CONSTRAINT chk_steps_data CHECK (data IS JSON)
);

COMMENT ON TABLE workflow_steps IS 'Hierarchical workflow steps within tickets';
COMMENT ON COLUMN workflow_steps.dependencies IS 'Comma-separated list of step IDs';
COMMENT ON COLUMN workflow_steps.mandatory_documents IS 'Comma-separated list of document names';
COMMENT ON COLUMN workflow_steps.optional_documents IS 'Comma-separated list of document names';

-- ============================================================================
-- TABLE 5: WORKFLOW_COMMENTS
-- Comments on workflow steps
-- ============================================================================

CREATE TABLE workflow_comments (
    id VARCHAR2(36) PRIMARY KEY,
    step_id VARCHAR2(36) NOT NULL,
    content CLOB NOT NULL,
    created_by VARCHAR2(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_comments_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE workflow_comments IS 'Comments and notes on workflow steps';

-- ============================================================================
-- TABLE 6: DOCUMENTS
-- File attachments for tickets and steps
-- ============================================================================

CREATE TABLE documents (
    id VARCHAR2(36) PRIMARY KEY,
    ticket_id VARCHAR2(36),
    step_id VARCHAR2(36),
    name VARCHAR2(500) NOT NULL,
    type VARCHAR2(100) NOT NULL,
    size NUMBER(15) NOT NULL,
    url VARCHAR2(1000),
    storage_path VARCHAR2(1000) NOT NULL,
    uploaded_by VARCHAR2(36) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    is_mandatory NUMBER(1) DEFAULT 0,
    is_completion_certificate NUMBER(1) DEFAULT 0,
    CONSTRAINT fk_documents_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_documents_reference CHECK ((ticket_id IS NOT NULL) OR (step_id IS NOT NULL))
);

COMMENT ON TABLE documents IS 'File attachments for tickets and workflow steps';

-- ============================================================================
-- TABLE 7: FILE_ATTACHMENTS (Legacy)
-- Legacy file attachments table
-- ============================================================================

CREATE TABLE file_attachments (
    id VARCHAR2(36) PRIMARY KEY,
    ticket_id VARCHAR2(36),
    step_id VARCHAR2(36),
    file_name VARCHAR2(500) NOT NULL,
    file_size NUMBER(15) NOT NULL,
    file_type VARCHAR2(100) NOT NULL,
    file_url VARCHAR2(1000) NOT NULL,
    uploaded_by VARCHAR2(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_file_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_attachments_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_attachments_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE file_attachments IS 'Legacy file attachments';

-- ============================================================================
-- TABLE 8: AUDIT_LOGS
-- Audit trail for all ticket operations
-- ============================================================================

CREATE TABLE audit_logs (
    id VARCHAR2(36) PRIMARY KEY,
    ticket_id VARCHAR2(36) NOT NULL,
    performed_by VARCHAR2(36) NOT NULL,
    action VARCHAR2(200) NOT NULL,
    old_data CLOB,
    new_data CLOB,
    description VARCHAR2(1000),
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_audit_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_performed_by FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE audit_logs IS 'Audit trail for all ticket and workflow operations';

-- ============================================================================
-- TABLE 9: FIELD_DEFINITIONS
-- Dynamic field type definitions
-- ============================================================================

CREATE TABLE field_definitions (
    id VARCHAR2(36) PRIMARY KEY,
    field_type VARCHAR2(50) NOT NULL CHECK (field_type IN ('text', 'number', 'date', 'dropdown', 'multi_select', 'checkbox', 'file_upload', 'textarea', 'alphanumeric')),
    field_key VARCHAR2(200) UNIQUE NOT NULL,
    label VARCHAR2(200) NOT NULL,
    description VARCHAR2(1000),
    icon VARCHAR2(100),
    default_validation_rules CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT chk_field_def_rules CHECK (default_validation_rules IS JSON)
);

COMMENT ON TABLE field_definitions IS 'Definitions for dynamic field types';

-- ============================================================================
-- TABLE 10: MODULE_FIELD_CONFIGURATIONS
-- Module-specific field configurations
-- ============================================================================

CREATE TABLE module_field_configurations (
    id VARCHAR2(36) PRIMARY KEY,
    module_id VARCHAR2(36) NOT NULL,
    field_key VARCHAR2(200) NOT NULL,
    field_type VARCHAR2(50) NOT NULL CHECK (field_type IN ('text', 'number', 'date', 'dropdown', 'multi_select', 'checkbox', 'file_upload', 'textarea', 'alphanumeric')),
    label VARCHAR2(200) NOT NULL,
    context VARCHAR2(50) NOT NULL CHECK (context IN ('ticket', 'workflow_step')),
    display_order NUMBER(10) DEFAULT 0,
    is_required NUMBER(1) DEFAULT 0,
    is_visible NUMBER(1) DEFAULT 1,
    is_system_field NUMBER(1) DEFAULT 0,
    default_value VARCHAR2(1000),
    validation_rules CLOB,
    role_visibility CLOB,
    conditional_visibility CLOB,
    placeholder VARCHAR2(500),
    help_text VARCHAR2(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_field_config_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT uq_field_config UNIQUE (module_id, field_key, context),
    CONSTRAINT chk_field_config_validation CHECK (validation_rules IS JSON),
    CONSTRAINT chk_field_config_role_vis CHECK (role_visibility IS JSON),
    CONSTRAINT chk_field_config_cond_vis CHECK (conditional_visibility IS JSON)
);

COMMENT ON TABLE module_field_configurations IS 'Module-specific field configurations';

-- ============================================================================
-- TABLE 11: FIELD_DROPDOWN_OPTIONS
-- Dropdown options for fields
-- ============================================================================

CREATE TABLE field_dropdown_options (
    id VARCHAR2(36) PRIMARY KEY,
    field_config_id VARCHAR2(36) NOT NULL,
    option_value VARCHAR2(500) NOT NULL,
    option_label VARCHAR2(500) NOT NULL,
    display_order NUMBER(10) DEFAULT 0,
    is_active NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_dropdown_field_config FOREIGN KEY (field_config_id) REFERENCES module_field_configurations(id) ON DELETE CASCADE
);

COMMENT ON TABLE field_dropdown_options IS 'Dropdown options for configured fields';

-- ============================================================================
-- TABLE 12: TICKET_FIELD_VALUES
-- Dynamic field values for tickets
-- ============================================================================

CREATE TABLE ticket_field_values (
    id VARCHAR2(36) PRIMARY KEY,
    ticket_id VARCHAR2(36) NOT NULL,
    field_key VARCHAR2(200) NOT NULL,
    field_value CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_ticket_field_values_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT uq_ticket_field_values UNIQUE (ticket_id, field_key)
);

COMMENT ON TABLE ticket_field_values IS 'Dynamic field values for tickets';

-- ============================================================================
-- TABLE 13: WORKFLOW_STEP_FIELD_VALUES
-- Dynamic field values for workflow steps
-- ============================================================================

CREATE TABLE workflow_step_field_values (
    id VARCHAR2(36) PRIMARY KEY,
    workflow_step_id VARCHAR2(36) NOT NULL,
    field_key VARCHAR2(200) NOT NULL,
    field_value CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_step_field_values_step FOREIGN KEY (workflow_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT uq_step_field_values UNIQUE (workflow_step_id, field_key)
);

COMMENT ON TABLE workflow_step_field_values IS 'Dynamic field values for workflow steps';

-- ============================================================================
-- TABLE 14: WORKFLOW_STEP_DEPENDENCIES
-- Step-to-step dependencies
-- ============================================================================

CREATE TABLE workflow_step_dependencies (
    id VARCHAR2(36) PRIMARY KEY,
    step_id VARCHAR2(36) NOT NULL,
    depends_on_step_id VARCHAR2(36) NOT NULL,
    created_by VARCHAR2(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    is_active NUMBER(1) DEFAULT 1,
    CONSTRAINT fk_step_dep_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_step_dep_depends_on FOREIGN KEY (depends_on_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_step_dep_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE workflow_step_dependencies IS 'Dependencies between workflow steps';

-- ============================================================================
-- TABLE 15: FILE_REFERENCE_TEMPLATES
-- File reference templates for workflow steps
-- ============================================================================

CREATE TABLE file_reference_templates (
    id VARCHAR2(36) PRIMARY KEY,
    module_id VARCHAR2(36) NOT NULL,
    name VARCHAR2(200) NOT NULL,
    description VARCHAR2(1000),
    file_type VARCHAR2(100),
    is_mandatory NUMBER(1) DEFAULT 0,
    display_order NUMBER(10) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_file_ref_template_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE
);

COMMENT ON TABLE file_reference_templates IS 'Templates for file references in workflow steps';

-- ============================================================================
-- TABLE 16: FILE_REFERENCES
-- File references attached to workflow steps
-- ============================================================================

CREATE TABLE file_references (
    id VARCHAR2(36) PRIMARY KEY,
    step_id VARCHAR2(36) NOT NULL,
    template_id VARCHAR2(36),
    file_name VARCHAR2(500) NOT NULL,
    file_type VARCHAR2(100),
    file_size NUMBER(15),
    storage_path VARCHAR2(1000),
    uploaded_by VARCHAR2(36),
    uploaded_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR2(50) DEFAULT 'pending',
    remarks VARCHAR2(2000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_file_ref_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_ref_template FOREIGN KEY (template_id) REFERENCES file_reference_templates(id) ON DELETE SET NULL,
    CONSTRAINT fk_file_ref_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE file_references IS 'File references attached to workflow steps';

-- ============================================================================
-- TABLE 17: WORKFLOW_STEP_PROGRESS_DOCUMENTS
-- Progress documents for workflow steps
-- ============================================================================

CREATE TABLE workflow_step_progress_documents (
    id VARCHAR2(36) PRIMARY KEY,
    step_id VARCHAR2(36) NOT NULL,
    document_name VARCHAR2(500) NOT NULL,
    document_type VARCHAR2(100),
    document_size NUMBER(15),
    storage_path VARCHAR2(1000) NOT NULL,
    progress_percentage NUMBER(5,2),
    uploaded_by VARCHAR2(36) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    remarks VARCHAR2(2000),
    CONSTRAINT fk_progress_doc_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_doc_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE workflow_step_progress_documents IS 'Progress documents uploaded during workflow step execution';

-- ============================================================================
-- TABLE 18: FINANCE_APPROVAL_WORKFLOW
-- Finance approval workflow tracking
-- ============================================================================

CREATE TABLE finance_approval_workflow (
    id VARCHAR2(36) PRIMARY KEY,
    ticket_id VARCHAR2(36) NOT NULL,
    submitted_by VARCHAR2(36) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    reviewed_by VARCHAR2(36),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR2(50) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    approval_remarks CLOB,
    supporting_documents VARCHAR2(4000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_finance_approval_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_finance_submitted_by FOREIGN KEY (submitted_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_finance_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE finance_approval_workflow IS 'Finance approval workflow for tickets';
COMMENT ON COLUMN finance_approval_workflow.supporting_documents IS 'Comma-separated list of document IDs';

-- ============================================================================
-- TABLE 19: USER_DISPLAY_PREFERENCES
-- User display preferences and settings
-- ============================================================================

CREATE TABLE user_display_preferences (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(36) NOT NULL,
    module_id VARCHAR2(36) NOT NULL,
    icon_display_mode VARCHAR2(50) DEFAULT 'grid',
    preferences CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_user_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_pref_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_display_pref UNIQUE (user_id, module_id),
    CONSTRAINT chk_user_pref CHECK (preferences IS JSON)
);

COMMENT ON TABLE user_display_preferences IS 'User-specific display preferences and UI settings';

-- ============================================================================
-- TABLE 20: USER_ROLES
-- Extended user role management
-- ============================================================================

CREATE TABLE user_roles (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(36) NOT NULL,
    role_name VARCHAR2(100) NOT NULL,
    module_id VARCHAR2(36),
    permissions CLOB,
    assigned_by VARCHAR2(36),
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    active NUMBER(1) DEFAULT 1,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_user_roles_perm CHECK (permissions IS JSON)
);

COMMENT ON TABLE user_roles IS 'Extended role-based access control for users';

-- ============================================================================
-- End of table creation
-- ============================================================================

-- Verify table creation
SELECT table_name FROM user_tables ORDER BY table_name;

COMMIT;
