/*
  # Add missing indexes for foreign key columns

  ## Summary
  Adds covering indexes for all foreign key columns that currently lack them.
  This improves join and lookup performance for referential integrity checks
  and cascading operations.

  ## New Indexes
  1. audit_logs.performed_by -> users.id
  2. file_attachments.uploaded_by -> users.id
  3. file_reference_templates.uploaded_by (fk_uploaded_by) -> users.id
  4. finance_approvals.submitted_by -> users.id
  5. tickets.assigned_to -> users.id
  6. tickets.created_by -> users.id
  7. tickets.finance_officer_id -> users.id
  8. user_management_audit.performed_by -> users.id
  9. users.created_by -> users.id
  10. users.updated_by -> users.id
  11. workflow_comments.created_by -> users.id
  12. workflow_step_dependencies.created_by -> users.id
  13. workflow_step_file_references.document_id -> documents.id
  14. workflow_step_file_references.template_id -> file_reference_templates.id
  15. workflow_step_file_references.uploaded_by -> users.id
  16. workflow_step_progress_documents.audit_log_id -> audit_logs.id
  17. workflow_step_progress_documents.deleted_by -> users.id
  18. workflow_step_progress_documents.ticket_id -> tickets.id
  19. workflow_step_progress_documents.uploaded_by -> users.id
  20. workflow_steps.created_by -> users.id
*/

CREATE INDEX IF NOT EXISTS idx_audit_logs_performed_by
  ON public.audit_logs USING btree (performed_by);

CREATE INDEX IF NOT EXISTS idx_file_attachments_uploaded_by
  ON public.file_attachments USING btree (uploaded_by);

CREATE INDEX IF NOT EXISTS idx_file_reference_templates_uploaded_by
  ON public.file_reference_templates USING btree (uploaded_by);

CREATE INDEX IF NOT EXISTS idx_finance_approvals_submitted_by
  ON public.finance_approvals USING btree (submitted_by);

CREATE INDEX IF NOT EXISTS idx_tickets_assigned_to
  ON public.tickets USING btree (assigned_to);

CREATE INDEX IF NOT EXISTS idx_tickets_created_by
  ON public.tickets USING btree (created_by);

CREATE INDEX IF NOT EXISTS idx_tickets_finance_officer_id
  ON public.tickets USING btree (finance_officer_id);

CREATE INDEX IF NOT EXISTS idx_user_management_audit_performed_by
  ON public.user_management_audit USING btree (performed_by);

CREATE INDEX IF NOT EXISTS idx_users_created_by
  ON public.users USING btree (created_by);

CREATE INDEX IF NOT EXISTS idx_users_updated_by
  ON public.users USING btree (updated_by);

CREATE INDEX IF NOT EXISTS idx_workflow_comments_created_by
  ON public.workflow_comments USING btree (created_by);

CREATE INDEX IF NOT EXISTS idx_workflow_step_dependencies_created_by
  ON public.workflow_step_dependencies USING btree (created_by);

CREATE INDEX IF NOT EXISTS idx_workflow_step_file_refs_document_id
  ON public.workflow_step_file_references USING btree (document_id);

CREATE INDEX IF NOT EXISTS idx_workflow_step_file_refs_template_id
  ON public.workflow_step_file_references USING btree (template_id);

CREATE INDEX IF NOT EXISTS idx_workflow_step_file_refs_uploaded_by
  ON public.workflow_step_file_references USING btree (uploaded_by);

CREATE INDEX IF NOT EXISTS idx_progress_docs_audit_log_id
  ON public.workflow_step_progress_documents USING btree (audit_log_id);

CREATE INDEX IF NOT EXISTS idx_progress_docs_deleted_by
  ON public.workflow_step_progress_documents USING btree (deleted_by);

CREATE INDEX IF NOT EXISTS idx_progress_docs_ticket_id
  ON public.workflow_step_progress_documents USING btree (ticket_id);

CREATE INDEX IF NOT EXISTS idx_progress_docs_uploaded_by
  ON public.workflow_step_progress_documents USING btree (uploaded_by);

CREATE INDEX IF NOT EXISTS idx_workflow_steps_created_by
  ON public.workflow_steps USING btree (created_by);
