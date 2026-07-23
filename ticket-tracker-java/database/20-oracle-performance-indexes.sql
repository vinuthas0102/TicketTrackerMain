-- Performance Optimization Indexes
-- Phase 5: Add indexes for frequently queried columns

-- Tickets table indexes
CREATE INDEX idx_tickets_created_by ON tickets(created_by);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_department ON tickets(department);
CREATE INDEX idx_tickets_module_id ON tickets(module_id);

-- Workflow steps table indexes
CREATE INDEX idx_workflow_steps_ticket_id ON workflow_steps(ticket_id);
CREATE INDEX idx_workflow_steps_status ON workflow_steps(status);
CREATE INDEX idx_workflow_steps_assigned_to_user ON workflow_steps(assigned_to_user);
CREATE INDEX idx_workflow_steps_assigned_to_group ON workflow_steps(assigned_to_group);

-- Documents table indexes
CREATE INDEX idx_documents_ticket_id ON documents(ticket_id);
CREATE INDEX idx_documents_step_id ON documents(workflow_step_id);

-- Audit logs table indexes
CREATE INDEX idx_audit_logs_entity_type_id ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Users table indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_department ON users(department);

-- Workflow step dependencies indexes
CREATE INDEX idx_workflow_step_deps_step_id ON workflow_step_dependencies(step_id);
CREATE INDEX idx_workflow_step_deps_dep_step_id ON workflow_step_dependencies(dep_step_id);

-- Workflow step file references indexes
CREATE INDEX idx_workflow_step_file_refs_step_id ON workflow_step_file_references(workflow_step_id);

-- Workflow step progress documents indexes
CREATE INDEX idx_workflow_step_progress_docs_step_id ON workflow_step_progress_documents(workflow_step_id);

-- Finance approval indexes
CREATE INDEX idx_finance_approval_ticket_id ON finance_approvals(ticket_id);

-- File reference templates indexes
CREATE INDEX idx_file_ref_templates_module_id ON file_reference_templates(module_id);

-- Master data indexes
CREATE INDEX idx_master_data_type ON master_data(data_type);
