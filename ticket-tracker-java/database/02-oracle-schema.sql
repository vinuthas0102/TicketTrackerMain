/*
  ==================================================================================
  Oracle Database Schema Creation for Ticket Tracking System
  ==================================================================================

  ## Overview
  Complete database schema converted from PostgreSQL to Oracle 19c.
  This creates all tables with proper data types, constraints, and defaults.

  ## Tables Created
  1. users - User accounts with role-based access control
  2. modules - Workflow modules/categories
  3. tickets - Main workflow instances
  4. workflow_steps - Hierarchical workflow steps
  5. workflow_comments - Comments on steps
  6. documents - File attachments
  7. file_attachments - Legacy file attachments
  8. audit_logs - Comprehensive audit trail
  9. field_definitions - Dynamic field types
  10. module_field_configurations - Module-specific fields
  11. field_dropdown_options - Dropdown options
  12. ticket_field_values - Dynamic ticket field values
  13. workflow_step_field_values - Dynamic step field values
  14. workflow_step_dependencies - Step dependency relationships
  15. workflow_step_progress_documents - Progress tracking documents
  16. file_reference_templates - File upload templates
  17. workflow_step_file_references - File reference tracking
  18. finance_approvals - Finance approval workflow
  19. user_display_preferences - User UI preferences

  ## Data Type Mappings from PostgreSQL
  - uuid → RAW(16) with SYS_GUID()
  - text → VARCHAR2(4000) or CLOB for long text
  - timestamptz → TIMESTAMP
  - boolean → NUMBER(1) with CHECK constraint (0=false, 1=true)
  - jsonb → CLOB with IS JSON CHECK constraint
  - text[] (arrays) → CLOB (comma-separated or JSON)
  - SERIAL → NUMBER with SEQUENCE

  ## Important Notes
  - All tables will have sequences and triggers defined in separate files
  - Foreign key constraints defined in 06-oracle-constraints.sql
  - Indexes defined in 05-oracle-indexes.sql
  - RLS not supported in Oracle Standard Edition (application-level security used)

  ## Run Order
  1. 01-oracle-create-user.sql (as SYSDBA)
  2. This file (as ticket_tracker user)
  3. 03-oracle-sequences.sql
  4. 04-oracle-triggers.sql
  5. 05-oracle-indexes.sql
  6. 06-oracle-constraints.sql
  7. 07-oracle-seed-data.sql
*/

-- Set session parameters for better performance
ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';
ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF';

-- ==================================================================================
-- TABLE: USERS
-- ==================================================================================
CREATE TABLE users (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  name VARCHAR2(500) NOT NULL,
  username VARCHAR2(100) UNIQUE,
  email VARCHAR2(500) NOT NULL UNIQUE,
  role VARCHAR2(50) NOT NULL CHECK (role IN ('employee', 'eo', 'dept_officer', 'vendor', 'finance')),
  department VARCHAR2(500) NOT NULL,
  password_hash VARCHAR2(500),
  password_salt VARCHAR2(500),
  avatar VARCHAR2(1000),
  active NUMBER(1) DEFAULT 1 CHECK (active IN (0, 1)),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  last_login TIMESTAMP
);

COMMENT ON TABLE users IS 'User accounts with role-based access control';
COMMENT ON COLUMN users.username IS 'Unique username for login (defaults to email prefix)';
COMMENT ON COLUMN users.password_hash IS 'SHA-256 hashed password';
COMMENT ON COLUMN users.password_salt IS 'Random salt for password hashing';
COMMENT ON COLUMN users.last_login IS 'Timestamp of last successful login (NULL if never logged in)';
COMMENT ON COLUMN users.active IS '1=active, 0=inactive';

-- ==================================================================================
-- TABLE: MODULES
-- ==================================================================================
CREATE TABLE modules (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  name VARCHAR2(500) NOT NULL,
  description CLOB,
  icon VARCHAR2(100) DEFAULT 'FileText',
  color VARCHAR2(100) DEFAULT 'from-blue-500 to-indigo-500',
  schema_id VARCHAR2(200) NOT NULL,
  config CLOB CHECK (config IS JSON),
  active NUMBER(1) DEFAULT 1 CHECK (active IN (0, 1)),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE modules IS 'Workflow modules/categories for organizing tickets';
COMMENT ON COLUMN modules.config IS 'JSON configuration for module settings';

-- ==================================================================================
-- TABLE: TICKETS
-- ==================================================================================
CREATE TABLE tickets (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  ticket_number VARCHAR2(100) NOT NULL UNIQUE,
  module_id RAW(16) NOT NULL,
  title VARCHAR2(1000) NOT NULL,
  description CLOB NOT NULL,
  status VARCHAR2(50) DEFAULT 'open',
  priority VARCHAR2(50),
  created_by RAW(16) NOT NULL,
  assigned_to RAW(16),
  due_date TIMESTAMP,
  data CLOB CHECK (data IS JSON),
  property_id VARCHAR2(200) DEFAULT 'PROP001' NOT NULL,
  property_location VARCHAR2(500) DEFAULT 'Location01' NOT NULL,
  completion_documents_required NUMBER(1) DEFAULT 1 CHECK (completion_documents_required IN (0, 1)),
  finance_officer_id RAW(16),
  finance_submission_count NUMBER(10) DEFAULT 0,
  latest_finance_status VARCHAR2(50),
  requires_finance_approval NUMBER(1) DEFAULT 1 CHECK (requires_finance_approval IN (0, 1)),
  start_date TIMESTAMP,
  request_type VARCHAR2(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE tickets IS 'Main workflow instances/tickets';
COMMENT ON COLUMN tickets.request_type IS 'Type of request for the ticket (e.g. corrective, preventive, inspection)';
COMMENT ON COLUMN tickets.data IS 'JSON data for dynamic fields';
COMMENT ON COLUMN tickets.finance_submission_count IS 'Number of times submitted to finance';
COMMENT ON COLUMN tickets.start_date IS 'When work actually started on the ticket';

-- ==================================================================================
-- TABLE: WORKFLOW_STEPS
-- ==================================================================================
CREATE TABLE workflow_steps (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  ticket_id RAW(16) NOT NULL,
  step_number VARCHAR2(100) NOT NULL,
  title VARCHAR2(1000) NOT NULL,
  description CLOB,
  status VARCHAR2(50) DEFAULT 'pending',
  assigned_to RAW(16),
  parent_step_id RAW(16),
  level_1 NUMBER(10),
  level_2 NUMBER(10),
  level_3 NUMBER(10),
  dependencies CLOB,
  is_parallel NUMBER(1) DEFAULT 0 CHECK (is_parallel IN (0, 1)),
  mandatory_documents CLOB,
  optional_documents CLOB,
  completion_certificate_required NUMBER(1) DEFAULT 0 CHECK (completion_certificate_required IN (0, 1)),
  due_date TIMESTAMP,
  data CLOB CHECK (data IS JSON),
  progress NUMBER(5, 2) DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
  dependency_mode VARCHAR2(20) DEFAULT 'all' CHECK (dependency_mode IN ('all', 'any_one')),
  is_dependency_locked NUMBER(1) DEFAULT 0 CHECK (is_dependency_locked IN (0, 1)),
  created_by RAW(16),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  completed_at TIMESTAMP,
  start_date TIMESTAMP,
  step_type VARCHAR2(500),
  CONSTRAINT chk_workflow_steps_no_self_parent CHECK (parent_step_id IS NULL OR parent_step_id != id)
);

COMMENT ON TABLE workflow_steps IS 'Hierarchical workflow steps for tickets';
COMMENT ON COLUMN workflow_steps.step_type IS 'Type/category of the workflow step (e.g. approval, review, inspection)';
COMMENT ON COLUMN workflow_steps.progress IS 'Progress percentage (0-100)';
COMMENT ON COLUMN workflow_steps.dependency_mode IS 'all=all dependencies must complete, any_one=any one dependency';
COMMENT ON COLUMN workflow_steps.dependencies IS 'Comma-separated or JSON array of dependent step IDs';
COMMENT ON COLUMN workflow_steps.mandatory_documents IS 'Comma-separated or JSON array of required document names';

-- ==================================================================================
-- TABLE: WORKFLOW_COMMENTS
-- ==================================================================================
CREATE TABLE workflow_comments (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  step_id RAW(16) NOT NULL,
  content CLOB NOT NULL,
  created_by RAW(16) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE workflow_comments IS 'Comments on workflow steps';

-- ==================================================================================
-- TABLE: DOCUMENTS
-- ==================================================================================
CREATE TABLE documents (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  ticket_id RAW(16),
  step_id RAW(16),
  name VARCHAR2(1000) NOT NULL,
  type VARCHAR2(200) NOT NULL,
  size NUMBER(20) NOT NULL,
  url VARCHAR2(2000),
  storage_path VARCHAR2(2000) NOT NULL,
  uploaded_by RAW(16) NOT NULL,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  is_mandatory NUMBER(1) DEFAULT 0 CHECK (is_mandatory IN (0, 1)),
  is_completion_certificate NUMBER(1) DEFAULT 0 CHECK (is_completion_certificate IN (0, 1)),
  CONSTRAINT chk_documents_reference CHECK (ticket_id IS NOT NULL OR step_id IS NOT NULL)
);

COMMENT ON TABLE documents IS 'File attachments for tickets and workflow steps';
COMMENT ON COLUMN documents.size IS 'File size in bytes';

-- ==================================================================================
-- TABLE: FILE_ATTACHMENTS
-- ==================================================================================
CREATE TABLE file_attachments (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  ticket_id RAW(16),
  step_id RAW(16),
  file_name VARCHAR2(1000) NOT NULL,
  file_size NUMBER(20) NOT NULL,
  file_type VARCHAR2(200) NOT NULL,
  file_url VARCHAR2(2000) NOT NULL,
  uploaded_by RAW(16) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE file_attachments IS 'Legacy file attachments table';

-- ==================================================================================
-- TABLE: AUDIT_LOGS
-- ==================================================================================
CREATE TABLE audit_logs (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  ticket_id RAW(16) NOT NULL,
  step_id RAW(16),
  performed_by RAW(16) NOT NULL,
  action VARCHAR2(500) NOT NULL,
  old_data CLOB,
  new_data CLOB,
  description CLOB,
  action_category VARCHAR2(50) DEFAULT 'ticket_action' CHECK (action_category IN ('ticket_action', 'workflow_action', 'document_action', 'status_change', 'assignment_change', 'progress_update', 'finance_action')),
  metadata CLOB CHECK (metadata IS JSON),
  performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all system actions';
COMMENT ON COLUMN audit_logs.action_category IS 'Category of action for filtering';
COMMENT ON COLUMN audit_logs.metadata IS 'Additional JSON context data';

-- ==================================================================================
-- TABLE: FIELD_DEFINITIONS
-- ==================================================================================
CREATE TABLE field_definitions (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  field_type VARCHAR2(50) NOT NULL CHECK (field_type IN ('text', 'number', 'date', 'dropdown', 'multi_select', 'checkbox', 'file_upload', 'textarea', 'alphanumeric')),
  field_key VARCHAR2(200) NOT NULL UNIQUE,
  label VARCHAR2(500) NOT NULL,
  description CLOB,
  icon VARCHAR2(100),
  default_validation_rules CLOB CHECK (default_validation_rules IS JSON),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE field_definitions IS 'Global field type definitions';

-- ==================================================================================
-- TABLE: MODULE_FIELD_CONFIGURATIONS
-- ==================================================================================
CREATE TABLE module_field_configurations (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  module_id RAW(16) NOT NULL,
  field_key VARCHAR2(200) NOT NULL,
  field_type VARCHAR2(50) NOT NULL CHECK (field_type IN ('text', 'number', 'date', 'dropdown', 'multi_select', 'checkbox', 'file_upload', 'textarea', 'alphanumeric')),
  label VARCHAR2(500) NOT NULL,
  context VARCHAR2(50) NOT NULL CHECK (context IN ('ticket', 'workflow_step')),
  display_order NUMBER(10) DEFAULT 0 NOT NULL,
  is_required NUMBER(1) DEFAULT 0 CHECK (is_required IN (0, 1)),
  is_visible NUMBER(1) DEFAULT 1 CHECK (is_visible IN (0, 1)),
  is_system_field NUMBER(1) DEFAULT 0 CHECK (is_system_field IN (0, 1)),
  default_value VARCHAR2(4000),
  validation_rules CLOB CHECK (validation_rules IS JSON),
  role_visibility CLOB CHECK (role_visibility IS JSON),
  conditional_visibility CLOB CHECK (conditional_visibility IS JSON),
  placeholder VARCHAR2(500),
  help_text VARCHAR2(2000),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_module_field_config UNIQUE (module_id, field_key, context)
);

COMMENT ON TABLE module_field_configurations IS 'Module-specific field configurations';
COMMENT ON COLUMN module_field_configurations.role_visibility IS 'JSON defining which roles can see this field';

-- ==================================================================================
-- TABLE: FIELD_DROPDOWN_OPTIONS
-- ==================================================================================
CREATE TABLE field_dropdown_options (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  field_config_id RAW(16) NOT NULL,
  option_value VARCHAR2(500) NOT NULL,
  option_label VARCHAR2(500) NOT NULL,
  display_order NUMBER(10) DEFAULT 0 NOT NULL,
  is_active NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE field_dropdown_options IS 'Dropdown options for field configurations';

-- ==================================================================================
-- TABLE: TICKET_FIELD_VALUES
-- ==================================================================================
CREATE TABLE ticket_field_values (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  ticket_id RAW(16) NOT NULL,
  field_key VARCHAR2(200) NOT NULL,
  field_value CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_ticket_field_value UNIQUE (ticket_id, field_key)
);

COMMENT ON TABLE ticket_field_values IS 'Dynamic field values for tickets';

-- ==================================================================================
-- TABLE: WORKFLOW_STEP_FIELD_VALUES
-- ==================================================================================
CREATE TABLE workflow_step_field_values (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  workflow_step_id RAW(16) NOT NULL,
  field_key VARCHAR2(200) NOT NULL,
  field_value CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_step_field_value UNIQUE (workflow_step_id, field_key)
);

COMMENT ON TABLE workflow_step_field_values IS 'Dynamic field values for workflow steps';

-- ==================================================================================
-- TABLE: WORKFLOW_STEP_DEPENDENCIES
-- ==================================================================================
CREATE TABLE workflow_step_dependencies (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  step_id RAW(16) NOT NULL,
  depends_on_step_id RAW(16) NOT NULL,
  created_by RAW(16) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  is_active NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),
  CONSTRAINT chk_no_self_dependency CHECK (step_id != depends_on_step_id),
  CONSTRAINT uq_dependency UNIQUE (step_id, depends_on_step_id)
);

COMMENT ON TABLE workflow_step_dependencies IS 'Step dependency relationships for serial execution';

-- ==================================================================================
-- TABLE: WORKFLOW_STEP_PROGRESS_DOCUMENTS
-- ==================================================================================
CREATE TABLE workflow_step_progress_documents (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  step_id RAW(16) NOT NULL,
  ticket_id RAW(16) NOT NULL,
  audit_log_id RAW(16),
  file_name VARCHAR2(1000) NOT NULL,
  file_path VARCHAR2(2000) NOT NULL,
  file_size NUMBER(20) NOT NULL,
  file_type VARCHAR2(200) NOT NULL,
  uploaded_by RAW(16) NOT NULL,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  deleted_at TIMESTAMP,
  deleted_by RAW(16),
  delete_reason VARCHAR2(2000),
  is_deleted NUMBER(1) DEFAULT 0 CHECK (is_deleted IN (0, 1)),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE workflow_step_progress_documents IS 'Progress tracking documents for workflow steps';
COMMENT ON COLUMN workflow_step_progress_documents.is_deleted IS 'Soft delete flag';

-- ==================================================================================
-- TABLE: FILE_REFERENCE_TEMPLATES
-- ==================================================================================
CREATE TABLE file_reference_templates (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  template_name VARCHAR2(500) NOT NULL UNIQUE,
  description VARCHAR2(2000),
  json_content CLOB NOT NULL CHECK (json_content IS JSON),
  uploaded_by RAW(16) NOT NULL,
  is_active NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE file_reference_templates IS 'JSON-based file reference templates for workflow steps';
COMMENT ON COLUMN file_reference_templates.json_content IS 'JSON with fileReferences, taskTitle, description, mandatoryFlags';

-- ==================================================================================
-- TABLE: WORKFLOW_STEP_FILE_REFERENCES
-- ==================================================================================
CREATE TABLE workflow_step_file_references (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  step_id RAW(16) NOT NULL,
  template_id RAW(16) NOT NULL,
  reference_name VARCHAR2(1000) NOT NULL,
  is_mandatory NUMBER(1) DEFAULT 0 CHECK (is_mandatory IN (0, 1)),
  document_id RAW(16),
  uploaded_by RAW(16),
  uploaded_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_step_reference UNIQUE (step_id, reference_name)
);

COMMENT ON TABLE workflow_step_file_references IS 'Links file references to workflow steps and tracks uploads';

-- ==================================================================================
-- TABLE: FINANCE_APPROVALS
-- ==================================================================================
CREATE TABLE finance_approvals (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  ticket_id RAW(16) NOT NULL,
  tentative_cost NUMBER(15, 2) NOT NULL CHECK (tentative_cost >= 0),
  cost_deducted_from VARCHAR2(100) NOT NULL CHECK (cost_deducted_from IN ('Current Tenant/Employee', 'Vacating Tenant/Employee', 'Borne by Management')),
  remarks CLOB NOT NULL,
  finance_officer_id RAW(16) NOT NULL,
  status VARCHAR2(50) DEFAULT 'pending' NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
  rejection_reason VARCHAR2(2000),
  submitted_by RAW(16) NOT NULL,
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  decided_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  approval_remarks VARCHAR2(2000),
  approval_document_file_name VARCHAR2(500),
  approval_document_file_path VARCHAR2(1000),
  approval_document_file_size NUMBER(10),
  approval_document_file_type VARCHAR2(100),
  approval_document_uploaded_at TIMESTAMP
);

COMMENT ON TABLE finance_approvals IS 'Finance approval requests for ticket costs';
COMMENT ON COLUMN finance_approvals.tentative_cost IS 'Estimated cost in Rs (Indian Rupees)';
COMMENT ON COLUMN finance_approvals.approval_remarks IS 'Remarks provided by the finance officer when approving the request';
COMMENT ON COLUMN finance_approvals.approval_document_file_name IS 'Original file name of the uploaded approval document';
COMMENT ON COLUMN finance_approvals.approval_document_file_path IS 'Storage path where the approval document is stored';
COMMENT ON COLUMN finance_approvals.approval_document_file_size IS 'Size of the approval document in bytes';
COMMENT ON COLUMN finance_approvals.approval_document_file_type IS 'MIME type of the approval document';
COMMENT ON COLUMN finance_approvals.approval_document_uploaded_at IS 'Timestamp when the approval document was uploaded';

-- ==================================================================================
-- TABLE: USER_DISPLAY_PREFERENCES
-- ==================================================================================
CREATE TABLE user_display_preferences (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  user_id RAW(16) NOT NULL UNIQUE,
  preferred_layout VARCHAR2(50) DEFAULT 'grid' CHECK (preferred_layout IN ('grid', 'list', 'compact')),
  items_per_page NUMBER(10) DEFAULT 20,
  show_completed NUMBER(1) DEFAULT 1 CHECK (show_completed IN (0, 1)),
  default_sort_field VARCHAR2(100) DEFAULT 'created_at',
  default_sort_order VARCHAR2(10) DEFAULT 'desc' CHECK (default_sort_order IN ('asc', 'desc')),
  theme VARCHAR2(20) DEFAULT 'light' CHECK (theme IN ('light', 'dark', 'auto')),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE user_display_preferences IS 'User interface display preferences';

-- ==================================================================================
-- TABLE: FILE_UPLOAD_CONFIG (Database-Driven File Upload Configuration)
-- ==================================================================================
CREATE TABLE file_upload_config (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  field_key VARCHAR2(200) NOT NULL,
  module_id RAW(16),
  max_file_size_bytes NUMBER(20) DEFAULT 10485760 NOT NULL,
  allowed_extensions VARCHAR2(1000) DEFAULT 'pdf,doc,docx,jpg,jpeg,png' NOT NULL,
  storage_base_path VARCHAR2(2000) DEFAULT '/uploads',
  is_mandatory NUMBER(1) DEFAULT 0 CHECK (is_mandatory IN (0, 1)),
  description VARCHAR2(2000),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_file_upload_config UNIQUE (field_key, module_id)
);

COMMENT ON TABLE file_upload_config IS 'Database-driven file upload configuration per field';
COMMENT ON COLUMN file_upload_config.max_file_size_bytes IS 'Maximum file size in bytes (default 10MB)';
COMMENT ON COLUMN file_upload_config.allowed_extensions IS 'Comma-separated list of allowed file extensions';

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Schema creation completed successfully!' FROM DUAL;
SELECT 'Total tables created: 20' FROM DUAL;
SELECT 'Next step: Run 03-oracle-sequences.sql' FROM DUAL;
