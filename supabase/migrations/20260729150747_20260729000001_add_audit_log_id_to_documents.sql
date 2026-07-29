/*
# Link step documents to audit log entries

1. Purpose
   - Step documents uploaded for completed tasks are stored in the `documents` table,
     but unlike `workflow_step_progress_documents` they have no link to the audit trail.
   - This migration adds an `audit_log_id` column to `documents` so that every uploaded
     document (step documents, completion certificates, regular attachments) can be
     associated with the audit log entry that records who uploaded it, when, and to which
     ticket/step.

2. Changes
   - `documents.audit_log_id` (uuid, nullable, REFERENCES audit_logs(id) ON DELETE SET NULL)
     — links a document to the audit log entry that describes its upload.
   - Index `idx_documents_audit_log_id` for efficient audit-trail joins.

3. Security
   - No RLS policy changes. The `documents` table already has RLS enabled and policies in place.
   - The new column is nullable, so existing rows are unaffected.

4. Notes
   - This mirrors the existing `workflow_step_progress_documents.audit_log_id` column.
   - The column is nullable because not every document needs an audit log link (e.g., legacy rows).
*/

ALTER TABLE documents
ADD COLUMN IF NOT EXISTS audit_log_id uuid REFERENCES audit_logs(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_documents_audit_log_id ON documents(audit_log_id);
