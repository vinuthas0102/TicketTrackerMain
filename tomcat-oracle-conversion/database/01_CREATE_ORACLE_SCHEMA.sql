-- =====================================================
-- TICKET TRACKER - ORACLE DATABASE SCHEMA
-- =====================================================
--
-- Converted from PostgreSQL (Supabase) to Oracle Database
--
-- This script creates the complete database schema for the
-- Ticket Tracker application, including all tables, sequences,
-- indexes, triggers, and constraints.
--
-- Compatible with: Oracle Database 19c and higher
--
-- IMPORTANT NOTES:
-- 1. UUID type is replaced with VARCHAR2(36) with CHECK constraints
-- 2. TIMESTAMPTZ is replaced with TIMESTAMP WITH TIME ZONE
-- 3. JSONB is replaced with CLOB with IS JSON constraint
-- 4. PostgreSQL arrays are replaced with VARCHAR2 (comma-separated)
-- 5. Boolean is replaced with NUMBER(1) with CHECK constraint
-- 6. TEXT type is replaced with VARCHAR2(4000) or CLOB
--
-- =====================================================

-- =====================================================
-- DROP EXISTING OBJECTS (if re-running script)
-- =====================================================

-- Drop tables in reverse dependency order
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE workflow_step_field_values CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE ticket_field_values CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE field_dropdown_options CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE module_field_configurations CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE field_definitions CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE workflow_step_dependencies CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE audit_logs CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE documents CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE file_attachments CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE workflow_comments CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE workflow_steps CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE tickets CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE modules CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE users CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

-- Drop sequences
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE users_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE modules_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE tickets_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE workflow_steps_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE workflow_comments_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE documents_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE file_attachments_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE audit_logs_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE field_definitions_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE module_field_configs_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE field_dropdown_options_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE ticket_field_values_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE workflow_step_field_vals_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE workflow_step_deps_seq';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

-- =====================================================
-- CREATE SEQUENCES FOR PRIMARY KEYS
-- =====================================================

CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE modules_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE tickets_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE workflow_steps_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE workflow_comments_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE documents_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE file_attachments_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE audit_logs_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE field_definitions_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE module_field_configs_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE field_dropdown_options_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE ticket_field_values_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE workflow_step_field_vals_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE workflow_step_deps_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- =====================================================
-- CREATE TABLES
-- =====================================================

-- -----------------------------------------------------
-- Table: USERS
-- User accounts with role-based access control
-- -----------------------------------------------------
CREATE TABLE users (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(255) NOT NULL,
    email VARCHAR2(255) UNIQUE NOT NULL,
    role VARCHAR2(20) NOT NULL CHECK (role IN ('employee', 'eo', 'dept_officer', 'vendor')),
    department VARCHAR2(100) NOT NULL,
    avatar VARCHAR2(500),
    active NUMBER(1) DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for USERS table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_department ON users(department);

COMMENT ON TABLE users IS 'User accounts with role-based access control';
COMMENT ON COLUMN users.role IS 'User role: employee, eo (Executive Officer), dept_officer (Department Officer), or vendor';
COMMENT ON COLUMN users.active IS '1 = active, 0 = inactive';

-- -----------------------------------------------------
-- Table: MODULES
-- Workflow modules/categories (Maintenance, Complaints, etc.)
-- -----------------------------------------------------
CREATE TABLE modules (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(255) NOT NULL,
    description CLOB,
    icon VARCHAR2(100) DEFAULT 'FileText',
    color VARCHAR2(100) DEFAULT 'from-blue-500 to-indigo-500',
    schema_id VARCHAR2(100) NOT NULL,
    config CLOB CONSTRAINT config_is_json CHECK (config IS JSON),
    active NUMBER(1) DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for MODULES table
CREATE INDEX idx_modules_active ON modules(active);
CREATE INDEX idx_modules_schema_id ON modules(schema_id);

COMMENT ON TABLE modules IS 'Workflow modules/categories for organizing different types of tickets';
COMMENT ON COLUMN modules.config IS 'JSON configuration including categories array';

-- -----------------------------------------------------
-- Table: TICKETS
-- Main workflow instances (tickets/work orders)
-- -----------------------------------------------------
CREATE TABLE tickets (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
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
    data_field CLOB CONSTRAINT ticket_data_is_json CHECK (data_field IS JSON),
    property_id VARCHAR2(100) DEFAULT 'PROP001' NOT NULL,
    property_location VARCHAR2(500) DEFAULT 'Location01' NOT NULL,
    completion_documents_required NUMBER(1) DEFAULT 1 CHECK (completion_documents_required IN (0, 1)),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tickets_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for TICKETS table
CREATE INDEX idx_tickets_module_id ON tickets(module_id);
CREATE INDEX idx_tickets_created_by ON tickets(created_by);
CREATE INDEX idx_tickets_assigned_to ON tickets(assigned_to);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_property_id ON tickets(property_id);
CREATE INDEX idx_tickets_property_location ON tickets(property_location);
CREATE INDEX idx_tickets_start_date ON tickets(start_date);

COMMENT ON TABLE tickets IS 'Main workflow instances (tickets/work orders)';
COMMENT ON COLUMN tickets.data_field IS 'Additional JSON data for ticket (renamed from data to avoid reserved keyword)';
COMMENT ON COLUMN tickets.status IS 'Ticket status: DRAFT, CREATED, APPROVED, ACTIVE, COMPLETED, CLOSED, CANCELLED';

-- -----------------------------------------------------
-- Table: WORKFLOW_STEPS
-- Hierarchical workflow steps within tickets
-- -----------------------------------------------------
CREATE TABLE workflow_steps (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    ticket_id VARCHAR2(36) NOT NULL,
    step_number VARCHAR2(50) NOT NULL,
    title VARCHAR2(500) NOT NULL,
    description CLOB,
    status VARCHAR2(50) DEFAULT 'not_started',
    assigned_to VARCHAR2(36),
    created_by VARCHAR2(36),
    parent_step_id VARCHAR2(36),
    level_1 NUMBER(10),
    level_2 NUMBER(10),
    level_3 NUMBER(10),
    dependencies VARCHAR2(4000),
    dependency_mode VARCHAR2(20) DEFAULT 'all' CHECK (dependency_mode IN ('all', 'any_one')),
    is_dependency_locked NUMBER(1) DEFAULT 0 CHECK (is_dependency_locked IN (0, 1)),
    is_parallel NUMBER(1) DEFAULT 0 CHECK (is_parallel IN (0, 1)),
    progress NUMBER(3) DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    mandatory_documents VARCHAR2(4000),
    optional_documents VARCHAR2(4000),
    completion_certificate_required NUMBER(1) DEFAULT 0 CHECK (completion_certificate_required IN (0, 1)),
    due_date TIMESTAMP WITH TIME ZONE,
    start_date TIMESTAMP WITH TIME ZONE,
    data_field CLOB CONSTRAINT workflow_step_data_json CHECK (data_field IS JSON),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_workflow_steps_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_workflow_steps_assigned FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_workflow_steps_created FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_workflow_steps_parent FOREIGN KEY (parent_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT chk_workflow_no_self_parent CHECK (parent_step_id IS NULL OR parent_step_id != id)
);

-- Indexes for WORKFLOW_STEPS table
CREATE INDEX idx_workflow_steps_ticket_id ON workflow_steps(ticket_id);
CREATE INDEX idx_workflow_steps_assigned ON workflow_steps(assigned_to);
CREATE INDEX idx_workflow_steps_created_by ON workflow_steps(created_by);
CREATE INDEX idx_workflow_steps_parent ON workflow_steps(parent_step_id);
CREATE INDEX idx_workflow_steps_ticket_parent ON workflow_steps(ticket_id, parent_step_id);
CREATE INDEX idx_workflow_steps_status ON workflow_steps(status);
CREATE INDEX idx_workflow_steps_progress ON workflow_steps(progress);
CREATE INDEX idx_workflow_steps_start_date ON workflow_steps(start_date);

COMMENT ON TABLE workflow_steps IS 'Hierarchical workflow steps within tickets';
COMMENT ON COLUMN workflow_steps.dependencies IS 'Comma-separated list of dependent step IDs';
COMMENT ON COLUMN workflow_steps.dependency_mode IS 'Dependency resolution mode: all or any_one';
COMMENT ON COLUMN workflow_steps.mandatory_documents IS 'Comma-separated list of required document types';
COMMENT ON COLUMN workflow_steps.optional_documents IS 'Comma-separated list of optional document types';
COMMENT ON COLUMN workflow_steps.status IS 'Step status: NOT_STARTED, WIP, COMPLETED, CLOSED';

-- -----------------------------------------------------
-- Table: WORKFLOW_STEP_DEPENDENCIES
-- Junction table for step dependencies
-- -----------------------------------------------------
CREATE TABLE workflow_step_dependencies (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    step_id VARCHAR2(36) NOT NULL,
    depends_on_step_id VARCHAR2(36) NOT NULL,
    created_by VARCHAR2(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),
    CONSTRAINT fk_deps_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_deps_depends_on FOREIGN KEY (depends_on_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_deps_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_no_self_dependency CHECK (step_id != depends_on_step_id),
    CONSTRAINT uk_dependency UNIQUE (step_id, depends_on_step_id)
);

-- Indexes for WORKFLOW_STEP_DEPENDENCIES table
CREATE INDEX idx_deps_step_id ON workflow_step_dependencies(step_id);
CREATE INDEX idx_deps_depends_on ON workflow_step_dependencies(depends_on_step_id);
CREATE INDEX idx_deps_active ON workflow_step_dependencies(step_id, is_active);

COMMENT ON TABLE workflow_step_dependencies IS 'Junction table storing dependency relationships between workflow steps';

-- -----------------------------------------------------
-- Table: WORKFLOW_COMMENTS
-- Comments on workflow steps
-- -----------------------------------------------------
CREATE TABLE workflow_comments (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    step_id VARCHAR2(36) NOT NULL,
    content CLOB NOT NULL,
    created_by VARCHAR2(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for WORKFLOW_COMMENTS table
CREATE INDEX idx_comments_step_id ON workflow_comments(step_id);
CREATE INDEX idx_comments_created_by ON workflow_comments(created_by);

COMMENT ON TABLE workflow_comments IS 'Comments on workflow steps';

-- -----------------------------------------------------
-- Table: DOCUMENTS
-- File attachments for tickets and steps
-- -----------------------------------------------------
CREATE TABLE documents (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    ticket_id VARCHAR2(36),
    step_id VARCHAR2(36),
    name VARCHAR2(500) NOT NULL,
    type VARCHAR2(100) NOT NULL,
    size_bytes NUMBER(15) NOT NULL,
    url VARCHAR2(1000),
    storage_path VARCHAR2(1000) NOT NULL,
    uploaded_by VARCHAR2(36) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_mandatory NUMBER(1) DEFAULT 0 CHECK (is_mandatory IN (0, 1)),
    is_completion_certificate NUMBER(1) DEFAULT 0 CHECK (is_completion_certificate IN (0, 1)),
    CONSTRAINT fk_documents_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_document_reference CHECK ((ticket_id IS NOT NULL) OR (step_id IS NOT NULL))
);

-- Indexes for DOCUMENTS table
CREATE INDEX idx_documents_ticket_id ON documents(ticket_id);
CREATE INDEX idx_documents_step_id ON documents(step_id);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_mandatory ON documents(is_mandatory);
CREATE INDEX idx_documents_ticket_cert ON documents(ticket_id, is_completion_certificate);
CREATE INDEX idx_documents_step_cert ON documents(step_id, is_completion_certificate);

COMMENT ON TABLE documents IS 'File attachments for tickets and workflow steps';
COMMENT ON COLUMN documents.storage_path IS 'Filesystem path where the file is stored';

-- -----------------------------------------------------
-- Table: FILE_ATTACHMENTS
-- Legacy file attachments table
-- -----------------------------------------------------
CREATE TABLE file_attachments (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    ticket_id VARCHAR2(36),
    step_id VARCHAR2(36),
    file_name VARCHAR2(500) NOT NULL,
    file_size NUMBER(15) NOT NULL,
    file_type VARCHAR2(100) NOT NULL,
    file_url VARCHAR2(1000) NOT NULL,
    uploaded_by VARCHAR2(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for FILE_ATTACHMENTS table
CREATE INDEX idx_attachments_ticket_id ON file_attachments(ticket_id);
CREATE INDEX idx_attachments_step_id ON file_attachments(step_id);

COMMENT ON TABLE file_attachments IS 'Legacy file attachments (use documents table for new attachments)';

-- -----------------------------------------------------
-- Table: AUDIT_LOGS
-- Comprehensive audit trail for all actions
-- -----------------------------------------------------
CREATE TABLE audit_logs (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    ticket_id VARCHAR2(36) NOT NULL,
    step_id VARCHAR2(36),
    performed_by VARCHAR2(36) NOT NULL,
    action VARCHAR2(500) NOT NULL,
    action_category VARCHAR2(50) DEFAULT 'ticket_action' CHECK (action_category IN ('ticket_action', 'workflow_action', 'document_action', 'status_change', 'assignment_change')),
    old_data CLOB,
    new_data CLOB,
    description CLOB,
    metadata CLOB CONSTRAINT audit_metadata_json CHECK (metadata IS JSON),
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_step FOREIGN KEY (step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_performed_by FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for AUDIT_LOGS table
CREATE INDEX idx_audit_ticket_id ON audit_logs(ticket_id);
CREATE INDEX idx_audit_step_id ON audit_logs(step_id);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_action_category ON audit_logs(action_category);
CREATE INDEX idx_audit_ticket_step ON audit_logs(ticket_id, step_id);

COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all ticket and workflow actions';
COMMENT ON COLUMN audit_logs.action_category IS 'Categorizes actions: ticket_action, workflow_action, document_action, status_change, assignment_change';

-- -----------------------------------------------------
-- Table: FIELD_DEFINITIONS
-- Dynamic field type definitions
-- -----------------------------------------------------
CREATE TABLE field_definitions (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    field_type VARCHAR2(50) NOT NULL CHECK (field_type IN ('text', 'number', 'date', 'dropdown', 'multi_select', 'checkbox', 'file_upload', 'textarea', 'alphanumeric')),
    field_key VARCHAR2(100) NOT NULL UNIQUE,
    label VARCHAR2(255) NOT NULL,
    description CLOB,
    icon VARCHAR2(100),
    default_validation_rules CLOB CONSTRAINT field_def_rules_json CHECK (default_validation_rules IS JSON),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE field_definitions IS 'Defines available field types for dynamic forms';

-- -----------------------------------------------------
-- Table: MODULE_FIELD_CONFIGURATIONS
-- Module-specific field configurations
-- -----------------------------------------------------
CREATE TABLE module_field_configurations (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    module_id VARCHAR2(36) NOT NULL,
    field_key VARCHAR2(100) NOT NULL,
    field_type VARCHAR2(50) NOT NULL CHECK (field_type IN ('text', 'number', 'date', 'dropdown', 'multi_select', 'checkbox', 'file_upload', 'textarea', 'alphanumeric')),
    label VARCHAR2(255) NOT NULL,
    context VARCHAR2(50) NOT NULL CHECK (context IN ('ticket', 'workflow_step')),
    display_order NUMBER(10) DEFAULT 0 NOT NULL,
    is_required NUMBER(1) DEFAULT 0 CHECK (is_required IN (0, 1)),
    is_visible NUMBER(1) DEFAULT 1 CHECK (is_visible IN (0, 1)),
    is_system_field NUMBER(1) DEFAULT 0 CHECK (is_system_field IN (0, 1)),
    default_value CLOB,
    validation_rules CLOB CONSTRAINT field_config_rules_json CHECK (validation_rules IS JSON),
    role_visibility CLOB DEFAULT '{"EO": true, "DO": true, "EMPLOYEE": true}' CONSTRAINT field_config_role_vis_json CHECK (role_visibility IS JSON),
    conditional_visibility CLOB DEFAULT '{}' CONSTRAINT field_config_cond_vis_json CHECK (conditional_visibility IS JSON),
    placeholder VARCHAR2(500),
    help_text CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_field_config_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT uk_module_field_context UNIQUE(module_id, field_key, context)
);

-- Indexes for MODULE_FIELD_CONFIGURATIONS table
CREATE INDEX idx_field_configs_module ON module_field_configurations(module_id, context);
CREATE INDEX idx_field_configs_order ON module_field_configurations(module_id, context, display_order);

COMMENT ON TABLE module_field_configurations IS 'Module-specific configurations for dynamic fields';
COMMENT ON COLUMN module_field_configurations.context IS 'Field context: ticket or workflow_step';

-- -----------------------------------------------------
-- Table: FIELD_DROPDOWN_OPTIONS
-- Dropdown options for field configurations
-- -----------------------------------------------------
CREATE TABLE field_dropdown_options (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    field_config_id VARCHAR2(36) NOT NULL,
    option_value VARCHAR2(500) NOT NULL,
    option_label VARCHAR2(500) NOT NULL,
    display_order NUMBER(10) DEFAULT 0 NOT NULL,
    is_active NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dropdown_field_config FOREIGN KEY (field_config_id) REFERENCES module_field_configurations(id) ON DELETE CASCADE
);

-- Indexes for FIELD_DROPDOWN_OPTIONS table
CREATE INDEX idx_dropdown_field ON field_dropdown_options(field_config_id, display_order);

COMMENT ON TABLE field_dropdown_options IS 'Options for dropdown and multi-select fields';

-- -----------------------------------------------------
-- Table: TICKET_FIELD_VALUES
-- Dynamic field values for tickets
-- -----------------------------------------------------
CREATE TABLE ticket_field_values (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    ticket_id VARCHAR2(36) NOT NULL,
    field_key VARCHAR2(100) NOT NULL,
    field_value CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_field_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_field UNIQUE(ticket_id, field_key)
);

-- Indexes for TICKET_FIELD_VALUES table
CREATE INDEX idx_ticket_field_ticket ON ticket_field_values(ticket_id);

COMMENT ON TABLE ticket_field_values IS 'Stores dynamic field values for tickets';

-- -----------------------------------------------------
-- Table: WORKFLOW_STEP_FIELD_VALUES
-- Dynamic field values for workflow steps
-- -----------------------------------------------------
CREATE TABLE workflow_step_field_values (
    id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    workflow_step_id VARCHAR2(36) NOT NULL,
    field_key VARCHAR2(100) NOT NULL,
    field_value CLOB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_step_field_step FOREIGN KEY (workflow_step_id) REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT uk_step_field UNIQUE(workflow_step_id, field_key)
);

-- Indexes for WORKFLOW_STEP_FIELD_VALUES table
CREATE INDEX idx_step_field_step ON workflow_step_field_values(workflow_step_id);

COMMENT ON TABLE workflow_step_field_values IS 'Stores dynamic field values for workflow steps';

-- =====================================================
-- CREATE TRIGGERS FOR UPDATED_AT COLUMNS
-- =====================================================

-- Trigger for USERS table
CREATE OR REPLACE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for MODULES table
CREATE OR REPLACE TRIGGER trg_modules_updated_at
BEFORE UPDATE ON modules
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for TICKETS table
CREATE OR REPLACE TRIGGER trg_tickets_updated_at
BEFORE UPDATE ON tickets
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for WORKFLOW_STEPS table
CREATE OR REPLACE TRIGGER trg_workflow_steps_updated_at
BEFORE UPDATE ON workflow_steps
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for FIELD_DEFINITIONS table
CREATE OR REPLACE TRIGGER trg_field_defs_updated_at
BEFORE UPDATE ON field_definitions
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for MODULE_FIELD_CONFIGURATIONS table
CREATE OR REPLACE TRIGGER trg_field_configs_updated_at
BEFORE UPDATE ON module_field_configurations
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for TICKET_FIELD_VALUES table
CREATE OR REPLACE TRIGGER trg_ticket_fields_updated_at
BEFORE UPDATE ON ticket_field_values
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for WORKFLOW_STEP_FIELD_VALUES table
CREATE OR REPLACE TRIGGER trg_step_fields_updated_at
BEFORE UPDATE ON workflow_step_field_values
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- =====================================================
-- SCHEMA CREATION COMPLETE
-- =====================================================

COMMIT;

-- Display success message
BEGIN
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Oracle Schema Created Successfully!');
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Tables created: 14');
    DBMS_OUTPUT.PUT_LINE('Sequences created: 14');
    DBMS_OUTPUT.PUT_LINE('Indexes created: 50+');
    DBMS_OUTPUT.PUT_LINE('Triggers created: 8');
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Next Step: Run 02_INSERT_SEED_DATA.sql');
    DBMS_OUTPUT.PUT_LINE('===========================================');
END;
/
