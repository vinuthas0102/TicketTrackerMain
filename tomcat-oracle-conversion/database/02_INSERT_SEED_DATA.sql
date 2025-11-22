-- =====================================================
-- TICKET TRACKER - ORACLE SEED DATA
-- =====================================================
--
-- This script inserts sample data for development and testing
--
-- Includes:
-- 1. Default modules (Maintenance, Complaints, Grievances, RTI, PEP)
-- 2. Sample users (Admin, Managers, Employees, Vendors)
-- 3. Field type definitions
-- 4. Module field configurations
-- 5. Dropdown options
--
-- Compatible with: Oracle Database 19c and higher
--
-- =====================================================

-- =====================================================
-- INSERT SAMPLE USERS
-- =====================================================

-- Clear existing data (optional - comment out if preserving data)
DELETE FROM workflow_step_field_values;
DELETE FROM ticket_field_values;
DELETE FROM field_dropdown_options;
DELETE FROM module_field_configurations;
DELETE FROM field_definitions;
DELETE FROM audit_logs;
DELETE FROM workflow_step_dependencies;
DELETE FROM documents;
DELETE FROM file_attachments;
DELETE FROM workflow_comments;
DELETE FROM workflow_steps;
DELETE FROM tickets;
DELETE FROM modules;
DELETE FROM users;

COMMIT;

-- Insert sample users with fixed IDs for consistency
INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440001', 'Administrator', 'admin@company.com', 'eo', 'ADMINISTRATION', 1);

INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440002', 'Department Manager IT', 'manager@company.com', 'dept_officer', 'IT', 1);

INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440003', 'John Employee', 'john@company.com', 'employee', 'IT', 1);

INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440004', 'Jane Doe', 'jane@company.com', 'employee', 'HR', 1);

INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440005', 'HR Manager', 'hrmanager@company.com', 'dept_officer', 'HR', 1);

INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440006', 'DO IT', 'do.it@company.com', 'dept_officer', 'IT', 1);

INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440007', 'DO Finance', 'do.finance@company.com', 'dept_officer', 'FINANCE', 1);

INSERT INTO users (id, name, email, role, department, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440008', 'ABC Construction', 'abc.construction@vendor.com', 'vendor', 'EXTERNAL', 1);

COMMIT;

-- =====================================================
-- INSERT DEFAULT MODULES
-- =====================================================

INSERT INTO modules (id, name, description, icon, color, schema_id, config, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440101',
   'Maintenance Tracker',
   'Track and manage maintenance requests and work orders',
   'Wrench',
   'from-blue-500 to-indigo-500',
   'maintenance',
   '{"categories": ["Electrical", "Plumbing", "HVAC", "General Maintenance", "Equipment Repair"]}',
   1);

INSERT INTO modules (id, name, description, icon, color, schema_id, config, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440102',
   'Complaints Tracker',
   'Manage customer complaints and resolution workflows',
   'AlertTriangle',
   'from-red-500 to-pink-500',
   'complaints',
   '{"categories": ["Service Quality", "Staff Behavior", "Facility Issues", "Process Issues", "Other"]}',
   1);

INSERT INTO modules (id, name, description, icon, color, schema_id, config, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440103',
   'Grievances Management',
   'Handle employee grievances and HR processes',
   'Users',
   'from-orange-500 to-red-500',
   'grievances',
   '{"categories": ["Workplace Issues", "Policy Concerns", "Discrimination", "Safety Issues", "Other"]}',
   1);

INSERT INTO modules (id, name, description, icon, color, schema_id, config, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440104',
   'RTI Tracker',
   'Right to Information request tracking and management',
   'FileText',
   'from-green-500 to-teal-500',
   'rti',
   '{"categories": ["Information Request", "Appeal", "Compliance", "Documentation", "Other"]}',
   1);

INSERT INTO modules (id, name, description, icon, color, schema_id, config, active) VALUES
  ('550e8400-e29b-41d4-a716-446655440105',
   'Project Execution Platform',
   'Track project milestones and deliverables',
   'Briefcase',
   'from-purple-500 to-indigo-500',
   'pep',
   '{"categories": ["Planning", "Execution", "Monitoring", "Resource Management", "Quality Control"]}',
   1);

COMMIT;

-- =====================================================
-- INSERT FIELD DEFINITIONS
-- =====================================================

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('text', 'text_field', 'Text Field', 'Single line text input', 'Type', '{"minLength": 0, "maxLength": 255}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('textarea', 'textarea_field', 'Text Area', 'Multi-line text input', 'FileText', '{"minLength": 0, "maxLength": 5000}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('number', 'number_field', 'Number Field', 'Numeric input', 'Hash', '{"min": null, "max": null}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('date', 'date_field', 'Date Field', 'Date picker', 'Calendar', '{}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('dropdown', 'dropdown_field', 'Dropdown', 'Single selection dropdown', 'ChevronDown', '{"required": false}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('multi_select', 'multi_select_field', 'Multi Select', 'Multiple selection field', 'List', '{"minSelections": 0, "maxSelections": null}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('checkbox', 'checkbox_field', 'Checkbox', 'Boolean checkbox', 'CheckSquare', '{}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('file_upload', 'file_upload_field', 'File Upload', 'File attachment field', 'Upload', '{"maxSize": 5242880, "allowedTypes": [".pdf", ".doc", ".docx", ".jpg", ".png", ".xlsx"]}');

INSERT INTO field_definitions (field_type, field_key, label, description, icon, default_validation_rules) VALUES
  ('alphanumeric', 'alphanumeric_field', 'Alphanumeric Field', 'Text with alphanumeric validation', 'Type', '{"pattern": "^[a-zA-Z0-9]+$", "minLength": 0, "maxLength": 255}');

COMMIT;

-- =====================================================
-- INSERT MODULE FIELD CONFIGURATIONS (MAINTENANCE MODULE)
-- =====================================================

-- Ticket fields for Maintenance module
INSERT INTO module_field_configurations (id, module_id, field_key, field_type, label, context, display_order, is_required, is_visible, is_system_field, placeholder, help_text, validation_rules, role_visibility) VALUES
  ('mfc-maint-ticket-title', '550e8400-e29b-41d4-a716-446655440101', 'title', 'text', 'Title', 'ticket', 1, 1, 1, 0, 'Enter ticket title', 'Brief description of the ticket', '{"minLength": 3, "maxLength": 255}', '{"EO": true, "DO": true, "EMPLOYEE": true}');

INSERT INTO module_field_configurations (id, module_id, field_key, field_type, label, context, display_order, is_required, is_visible, is_system_field, placeholder, help_text, validation_rules, role_visibility) VALUES
  ('mfc-maint-ticket-desc', '550e8400-e29b-41d4-a716-446655440101', 'description', 'textarea', 'Description', 'ticket', 2, 1, 1, 0, 'Enter detailed description', 'Provide comprehensive details', '{"minLength": 10, "maxLength": 5000}', '{"EO": true, "DO": true, "EMPLOYEE": true}');

INSERT INTO module_field_configurations (id, module_id, field_key, field_type, label, context, display_order, is_required, is_visible, is_system_field, placeholder, help_text, validation_rules, role_visibility) VALUES
  ('mfc-maint-ticket-priority', '550e8400-e29b-41d4-a716-446655440101', 'priority', 'dropdown', 'Priority', 'ticket', 3, 1, 1, 0, 'Select priority', 'Ticket priority level', '{}', '{"EO": true, "DO": true, "EMPLOYEE": true}');

INSERT INTO module_field_configurations (id, module_id, field_key, field_type, label, context, display_order, is_required, is_visible, is_system_field, placeholder, help_text, validation_rules, role_visibility) VALUES
  ('mfc-maint-ticket-category', '550e8400-e29b-41d4-a716-446655440101', 'category', 'dropdown', 'Category', 'ticket', 4, 1, 1, 0, 'Select category', 'Maintenance category', '{}', '{"EO": true, "DO": true, "EMPLOYEE": true}');

-- Workflow step fields for Maintenance module
INSERT INTO module_field_configurations (id, module_id, field_key, field_type, label, context, display_order, is_required, is_visible, is_system_field, placeholder, help_text, validation_rules, role_visibility) VALUES
  ('mfc-maint-step-title', '550e8400-e29b-41d4-a716-446655440101', 'title', 'text', 'Title', 'workflow_step', 1, 1, 1, 0, 'Enter step title', 'Brief description of the step', '{"minLength": 3, "maxLength": 255}', '{"EO": true, "DO": true, "EMPLOYEE": true}');

INSERT INTO module_field_configurations (id, module_id, field_key, field_type, label, context, display_order, is_required, is_visible, is_system_field, placeholder, help_text, validation_rules, role_visibility) VALUES
  ('mfc-maint-step-desc', '550e8400-e29b-41d4-a716-446655440101', 'description', 'textarea', 'Description', 'workflow_step', 2, 0, 1, 0, 'Enter step description', 'Detailed description', '{"minLength": 0, "maxLength": 2000}', '{"EO": true, "DO": true, "EMPLOYEE": true}');

COMMIT;

-- =====================================================
-- INSERT DROPDOWN OPTIONS
-- =====================================================

-- Priority options for Maintenance module
INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-priority', 'LOW', 'Low', 1, 1);

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-priority', 'MEDIUM', 'Medium', 2, 1);

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-priority', 'HIGH', 'High', 3, 1);

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-priority', 'CRITICAL', 'Critical', 4, 1);

-- Category options for Maintenance module
INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-category', 'ELECTRICAL', 'Electrical', 1, 1);

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-category', 'PLUMBING', 'Plumbing', 2, 1);

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-category', 'HVAC', 'HVAC', 3, 1);

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-category', 'GENERAL', 'General Maintenance', 4, 1);

INSERT INTO field_dropdown_options (field_config_id, option_value, option_label, display_order, is_active) VALUES
  ('mfc-maint-ticket-category', 'EQUIPMENT', 'Equipment Repair', 5, 1);

COMMIT;

-- =====================================================
-- INSERT SAMPLE TICKETS (Optional - for testing)
-- =====================================================

-- Sample Maintenance Ticket
INSERT INTO tickets (id, ticket_number, module_id, title, description, status, priority, created_by, assigned_to, property_id, property_location, completion_documents_required) VALUES
  ('ticket-sample-001',
   'MAINT-2025-001',
   '550e8400-e29b-41d4-a716-446655440101',
   'Fix Air Conditioning in Building A',
   'The air conditioning unit in Building A, Floor 3 is not working properly. Temperature is too high and affecting employee productivity.',
   'ACTIVE',
   'HIGH',
   '550e8400-e29b-41d4-a716-446655440003',
   '550e8400-e29b-41d4-a716-446655440002',
   'PROP-A-001',
   'Building A, Floor 3',
   1);

-- Sample Workflow Step
INSERT INTO workflow_steps (id, ticket_id, step_number, title, description, status, assigned_to, created_by, level_1, progress) VALUES
  ('step-sample-001',
   'ticket-sample-001',
   '1',
   'Diagnose AC Unit',
   'Inspect the AC unit and identify the root cause of the malfunction',
   'WIP',
   '550e8400-e29b-41d4-a716-446655440003',
   '550e8400-e29b-41d4-a716-446655440002',
   1,
   30);

-- Sample Audit Log
INSERT INTO audit_logs (ticket_id, step_id, performed_by, action, action_category, description, new_data) VALUES
  ('ticket-sample-001',
   'step-sample-001',
   '550e8400-e29b-41d4-a716-446655440002',
   'Step Progress Updated',
   'workflow_action',
   'Updated workflow step progress to 30%',
   '30%');

COMMIT;

-- =====================================================
-- SEED DATA INSERTION COMPLETE
-- =====================================================

-- Display summary
BEGIN
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Seed Data Inserted Successfully!');
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Users created: 8');
    DBMS_OUTPUT.PUT_LINE('Modules created: 5');
    DBMS_OUTPUT.PUT_LINE('Field definitions: 9');
    DBMS_OUTPUT.PUT_LINE('Sample tickets: 1');
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Test Login Credentials:');
    DBMS_OUTPUT.PUT_LINE('  Admin: admin@company.com / admin');
    DBMS_OUTPUT.PUT_LINE('  Manager: manager@company.com / manager');
    DBMS_OUTPUT.PUT_LINE('  Employee: john@company.com / user');
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Next Step: Configure Spring Boot backend');
    DBMS_OUTPUT.PUT_LINE('===========================================');
END;
/
