-- Add audit_log_id column to documents table
-- Links step documents and completion certificates to audit log entries

ALTER TABLE documents ADD (
    audit_log_id RAW(16)
);

-- Add index for audit log lookups
CREATE INDEX idx_documents_audit_log_id ON documents(audit_log_id);

-- Add foreign key constraint
ALTER TABLE documents ADD CONSTRAINT fk_documents_audit_log
    FOREIGN KEY (audit_log_id) REFERENCES audit_logs(id) ON DELETE SET NULL;
