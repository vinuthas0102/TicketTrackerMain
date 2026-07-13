/*
  # Add missing FK index on documents and drop unused indexes

  ## Summary
  1. Adds the missing covering index for documents.uploaded_by FK.
  2. Drops all indexes flagged as unused since the last migration batch.
     These were just added and never queried, so they add write overhead with no benefit.

  ## New Indexes
  - idx_documents_uploaded_by on documents(uploaded_by)

  ## Dropped Unused Indexes
  - idx_progress_docs_audit_log_id
  - idx_progress_docs_deleted_by
  - idx_progress_docs_ticket_id
  - idx_progress_docs_uploaded_by
  - idx_users_created_by
  - idx_users_updated_by
  - idx_workflow_comments_created_by
  - idx_user_management_audit_performed_by
  - idx_file_attachments_uploaded_by
  - idx_audit_logs_performed_by
  - idx_file_reference_templates_uploaded_by
  - idx_workflow_step_dependencies_created_by
  - idx_workflow_step_file_refs_document_id
  - idx_workflow_step_file_refs_template_id
  - idx_workflow_step_file_refs_uploaded_by
  - idx_tickets_assigned_to
  - idx_tickets_created_by
  - idx_tickets_finance_officer_id
  - idx_finance_approvals_submitted_by
  - idx_finance_approvals_finance_officer_id
  - idx_workflow_steps_created_by
*/

CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by
  ON public.documents USING btree (uploaded_by);

DROP INDEX IF EXISTS public.idx_progress_docs_audit_log_id;
DROP INDEX IF EXISTS public.idx_progress_docs_deleted_by;
DROP INDEX IF EXISTS public.idx_progress_docs_ticket_id;
DROP INDEX IF EXISTS public.idx_progress_docs_uploaded_by;
DROP INDEX IF EXISTS public.idx_users_created_by;
DROP INDEX IF EXISTS public.idx_users_updated_by;
DROP INDEX IF EXISTS public.idx_workflow_comments_created_by;
DROP INDEX IF EXISTS public.idx_user_management_audit_performed_by;
DROP INDEX IF EXISTS public.idx_file_attachments_uploaded_by;
DROP INDEX IF EXISTS public.idx_audit_logs_performed_by;
DROP INDEX IF EXISTS public.idx_file_reference_templates_uploaded_by;
DROP INDEX IF EXISTS public.idx_workflow_step_dependencies_created_by;
DROP INDEX IF EXISTS public.idx_workflow_step_file_refs_document_id;
DROP INDEX IF EXISTS public.idx_workflow_step_file_refs_template_id;
DROP INDEX IF EXISTS public.idx_workflow_step_file_refs_uploaded_by;
DROP INDEX IF EXISTS public.idx_tickets_assigned_to;
DROP INDEX IF EXISTS public.idx_tickets_created_by;
DROP INDEX IF EXISTS public.idx_tickets_finance_officer_id;
DROP INDEX IF EXISTS public.idx_finance_approvals_submitted_by;
DROP INDEX IF EXISTS public.idx_finance_approvals_finance_officer_id;
DROP INDEX IF EXISTS public.idx_workflow_steps_created_by;
