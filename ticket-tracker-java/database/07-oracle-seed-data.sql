/*
  Oracle Database Seed Data for Ticket Tracking System

  This script populates the database with initial data:
  1. Default modules (Maintenance, Complaints, Grievances, RTI, PEP)
  2. Mock users for development and testing
  3. Default field type definitions
  4. Default field configurations for each module
  5. Dropdown options for priority and status fields
  6. Default file upload configurations

  Run this script after 06-oracle-constraints.sql
*/

-- ==================================================================================
-- INSERT DEFAULT MODULES
-- ==================================================================================
-- Note: Using HEXTORAW() to convert UUID strings to RAW(16)

-- Maintenance Tracker
INSERT INTO modules (id, name, description, icon, color, schema_id, config, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440101', '-', '')),
  'Maintenance Tracker',
  'Track and manage maintenance requests and work orders',
  'Wrench',
  'from-blue-500 to-indigo-500',
  'maintenance',
  '{"categories": ["Electrical", "Plumbing", "HVAC", "General Maintenance", "Equipment Repair"]}',
  1
);

-- Complaints Tracker
INSERT INTO modules (id, name, description, icon, color, schema_id, config, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440102', '-', '')),
  'Complaints Tracker',
  'Manage customer complaints and resolution workflows',
  'AlertTriangle',
  'from-red-500 to-pink-500',
  'complaints',
  '{"categories": ["Service Quality", "Staff Behavior", "Facility Issues", "Process Issues", "Other"]}',
  1
);

-- Grievances Management
INSERT INTO modules (id, name, description, icon, color, schema_id, config, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440103', '-', '')),
  'Grievances Management',
  'Handle employee grievances and HR processes',
  'Users',
  'from-orange-500 to-red-500',
  'grievances',
  '{"categories": ["Workplace Issues", "Policy Concerns", "Discrimination", "Safety Issues", "Other"]}',
  1
);

-- RTI Tracker
INSERT INTO modules (id, name, description, icon, color, schema_id, config, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440104', '-', '')),
  'RTI Tracker',
  'Right to Information request tracking and management',
  'FileText',
  'from-green-500 to-teal-500',
  'rti',
  '{"categories": ["Information Request", "Appeal", "Compliance", "Documentation", "Other"]}',
  1
);

-- Project Execution Platform
INSERT INTO modules (id, name, description, icon, color, schema_id, config, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440105', '-', '')),
  'Project Execution Platform',
  'Track project milestones and deliverables',
  'Briefcase',
  'from-purple-500 to-indigo-500',
  'pep',
  '{"categories": ["Planning", "Execution", "Monitoring", "Resource Management", "Quality Control"]}',
  1
);

COMMIT;

-- ==================================================================================
-- INSERT DEFAULT USERS (with default passwords)
-- ==================================================================================
-- Note: Passwords should be hashed in production. Here using plain text for development.
-- Password hashing will be done by the application (SHA-256 + salt)

-- Administrator (EO - Executive Officer)
INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440001', '-', '')),
  'Administrator',
  'admin@company.com',
  'eo',
  'ADMINISTRATION',
  'changeme',  -- To be hashed by application
  'salt',
  1
);

-- Department Manager (DO - Department Officer)
INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440002', '-', '')),
  'Department Manager',
  'manager@company.com',
  'dept_officer',
  'IT',
  'changeme',
  'salt',
  1
);

-- John Employee
INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440003', '-', '')),
  'John Employee',
  'john@company.com',
  'employee',
  'IT',
  'changeme',
  'salt',
  1
);

-- Jane Doe
INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440004', '-', '')),
  'Jane Doe',
  'jane@company.com',
  'employee',
  'HR',
  'changeme',
  'salt',
  1
);

-- HR Manager (DO)
INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440005', '-', '')),
  'HR Manager',
  'hrmanager@company.com',
  'dept_officer',
  'HR',
  'changeme',
  'salt',
  1
);

-- Vendor Users
INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440021', '-', '')),
  'ABC Construction',
  'abc.construction@vendor.com',
  'vendor',
  'VENDOR',
  'vendor123',
  'salt',
  1
);

INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440022', '-', '')),
  'XYZ Suppliers',
  'xyz.suppliers@vendor.com',
  'vendor',
  'VENDOR',
  'vendor123',
  'salt',
  1
);

INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440023', '-', '')),
  'Global Services',
  'global.services@vendor.com',
  'vendor',
  'VENDOR',
  'vendor123',
  'salt',
  1
);

-- Finance Officer
INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
VALUES (
  HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440030', '-', '')),
  'Finance Officer',
  'finance.officer@company.com',
  'finance',
  'FINANCE',
  'finance123',
  'salt',
  1
);

COMMIT;

-- ==================================================================================
-- INSERT DEFAULT FIELD TYPE DEFINITIONS
-- ==================================================================================
INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('text', 'text_field', 'Text Field', 'Single line text input', 'Type', '{"minLength": 0, "maxLength": 255}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('textarea', 'textarea_field', 'Text Area', 'Multi-line text input', 'FileText', '{"minLength": 0, "maxLength": 5000}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('number', 'number_field', 'Number Field', 'Numeric input', 'Hash', '{"min": null, "max": null}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('date', 'date_field', 'Date Field', 'Date picker', 'Calendar', '{}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('dropdown', 'dropdown_field', 'Dropdown', 'Single selection dropdown', 'ChevronDown', '{"required": false}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('multi_select', 'multi_select_field', 'Multi Select', 'Multiple selection field', 'List', '{"minSelections": 0, "maxSelections": null}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('checkbox', 'checkbox_field', 'Checkbox', 'Boolean checkbox', 'CheckSquare', '{}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('file_upload', 'file_upload_field', 'File Upload', 'File attachment field', 'Upload', '{"maxSize": 5242880, "allowedTypes": [".pdf", ".doc", ".docx", ".jpg", ".png", ".xlsx"]}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules)
VALUES ('alphanumeric', 'alphanumeric_field', 'Alphanumeric Field', 'Text with alphanumeric validation', 'Type', '{"pattern": "^[a-zA-Z0-9]+$", "minLength": 0, "maxLength": 255}');

COMMIT;

-- ==================================================================================
-- PROCEDURE: Create Default Ticket Fields for a Module
-- ==================================================================================
CREATE OR REPLACE PROCEDURE create_default_ticket_fields(p_module_id RAW) IS
BEGIN
  -- Title
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'title', 'text', 'Title', 'ticket', 1, 1, 1, 0,
    'Enter ticket title', 'Brief description of the ticket',
    '{"minLength": 3, "maxLength": 255}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Description
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'description', 'textarea', 'Description', 'ticket', 2, 1, 1, 0,
    'Enter detailed description', 'Provide comprehensive details about the ticket',
    '{"minLength": 10, "maxLength": 5000}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Priority
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'priority', 'dropdown', 'Priority', 'ticket', 3, 1, 1, 0,
    'Select priority', 'Ticket priority level',
    '{}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Category
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'category', 'dropdown', 'Category', 'ticket', 4, 1, 1, 0,
    'Select category', 'Ticket category or type',
    '{}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Department
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'department', 'dropdown', 'Department', 'ticket', 5, 1, 1, 0,
    'Select department', 'Department responsible for this ticket',
    '{}',
    '{"EO": true, "DO": false, "EMPLOYEE": false}'
  );

  -- Assigned To
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'assigned_to', 'dropdown', 'Assigned To', 'ticket', 6, 0, 1, 0,
    'Select assignee', 'Person responsible for this ticket',
    '{}',
    '{"EO": true, "DO": true, "EMPLOYEE": false}'
  );

  -- Est Completion Date
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'due_date', 'date', 'Est Completion Date', 'ticket', 7, 0, 1, 0,
    'Select date', 'Expected completion date',
    '{"minDate": "today"}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Attachments
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'attachments', 'file_upload', 'Attachments', 'ticket', 8, 0, 1, 0,
    'Upload files', 'Attach relevant documents',
    '{"maxSize": 5242880, "allowedTypes": [".pdf", ".doc", ".docx", ".txt", ".jpg", ".jpeg", ".png", ".gif", ".xlsx", ".xls"], "maxFiles": 10}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  COMMIT;
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    NULL; -- Ignore duplicates
END;
/

-- ==================================================================================
-- PROCEDURE: Create Default Workflow Step Fields for a Module
-- ==================================================================================
CREATE OR REPLACE PROCEDURE create_default_workflow_step_fields(p_module_id RAW) IS
BEGIN
  -- Title
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'title', 'text', 'Title', 'workflow_step', 1, 1, 1, 0,
    'Enter step title', 'Brief description of the workflow step',
    '{"minLength": 3, "maxLength": 255}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Description
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'description', 'textarea', 'Description', 'workflow_step', 2, 0, 1, 0,
    'Enter step description', 'Detailed description of the workflow step',
    '{"minLength": 0, "maxLength": 2000}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Status
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'status', 'dropdown', 'Status', 'workflow_step', 3, 1, 1, 0,
    'Select status', 'Current status of the workflow step',
    '{}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  -- Assigned To
  INSERT INTO module_field_configurations (
    module_id, field_key, field_type, label, context, display_order,
    is_required, is_visible, is_system_field, placeholder, help_text,
    validation_rules, role_visibility
  ) VALUES (
    p_module_id, 'assigned_to', 'dropdown', 'Assigned To', 'workflow_step', 4, 0, 1, 0,
    'Select assignee', 'Person responsible for this step',
    '{}',
    '{"EO": true, "DO": true, "EMPLOYEE": true}'
  );

  COMMIT;
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    NULL; -- Ignore duplicates
END;
/

-- ==================================================================================
-- EXECUTE: Create field configurations for all active modules
-- ==================================================================================
DECLARE
  CURSOR module_cursor IS SELECT id FROM modules WHERE active = 1;
BEGIN
  FOR module_rec IN module_cursor LOOP
    create_default_ticket_fields(module_rec.id);
    create_default_workflow_step_fields(module_rec.id);
  END LOOP;
  COMMIT;
END;
/

-- ==================================================================================
-- INSERT: Priority dropdown options for all modules
-- ==================================================================================
INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active)
SELECT mfc.id, 'LOW', 'Low', 1, 1
FROM module_field_configurations mfc
WHERE mfc.field_key = 'priority' AND mfc.context = 'ticket';

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active)
SELECT mfc.id, 'MEDIUM', 'Medium', 2, 1
FROM module_field_configurations mfc
WHERE mfc.field_key = 'priority' AND mfc.context = 'ticket';

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active)
SELECT mfc.id, 'HIGH', 'High', 3, 1
FROM module_field_configurations mfc
WHERE mfc.field_key = 'priority' AND mfc.context = 'ticket';

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active)
SELECT mfc.id, 'CRITICAL', 'Critical', 4, 1
FROM module_field_configurations mfc
WHERE mfc.field_key = 'priority' AND mfc.context = 'ticket';

COMMIT;

-- ==================================================================================
-- INSERT: Workflow step status dropdown options for all modules
-- ==================================================================================
INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active)
SELECT mfc.id, 'PENDING', 'Pending', 1, 1
FROM module_field_configurations mfc
WHERE mfc.field_key = 'status' AND mfc.context = 'workflow_step';

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active)
SELECT mfc.id, 'IN_PROGRESS', 'In Progress', 2, 1
FROM module_field_configurations mfc
WHERE mfc.field_key = 'status' AND mfc.context = 'workflow_step';

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active)
SELECT mfc.id, 'COMPLETED', 'Completed', 3, 1
FROM module_field_configurations mfc
WHERE mfc.field_key = 'status' AND mfc.context = 'workflow_step';

COMMIT;

-- ==================================================================================
-- INSERT: Default file upload configurations
-- ==================================================================================
INSERT INTO file_upload_config (field_key, module_id, max_file_size_bytes, allowed_extensions, storage_base_path, is_mandatory, description)
SELECT 'attachments', m.id, 10485760, 'pdf,doc,docx,txt,jpg,jpeg,png,gif,xlsx,xls', '/uploads/ticket-attachments', 0, 'Ticket attachments'
FROM modules m;

INSERT INTO file_upload_config (field_key, module_id, max_file_size_bytes, allowed_extensions, storage_base_path, is_mandatory, description)
SELECT 'step_documents', m.id, 10485760, 'pdf,doc,docx,jpg,jpeg,png', '/uploads/step-documents', 0, 'Workflow step documents'
FROM modules m;

COMMIT;

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Seed data inserted successfully!' FROM DUAL;
SELECT 'Total modules: ' || COUNT(*) AS module_count FROM modules;
SELECT 'Total users: ' || COUNT(*) AS user_count FROM users;
SELECT 'Total field definitions: ' || COUNT(*) AS field_count FROM field_definitions;
SELECT 'Setup complete! Ready for application deployment.' FROM DUAL;
