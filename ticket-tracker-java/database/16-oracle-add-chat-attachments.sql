-- ============================================================
-- Migration: Add Chat Attachments, Channel, and Updated At
-- to workflow_comments table
-- ============================================================
--
-- This migration adds support for:
-- 1. File attachments on chat messages (path, name, type)
-- 2. Channel tracking (in-app, email, sms, whatsapp)
-- 3. "Edited" indicator via updated_at timestamp
--
-- All columns are nullable (except channel which defaults to 'in-app')
-- to ensure backward compatibility with existing rows.
-- ============================================================

-- Add updated_at column (already added by migration 09, but included here
-- for the java-ticket-tracker schema path which may not have run it)
ALTER TABLE workflow_comments ADD updated_at TIMESTAMP NULL;

-- Add attachment columns
ALTER TABLE workflow_comments ADD attachment_path VARCHAR2(500) NULL;
ALTER TABLE workflow_comments ADD attachment_name VARCHAR2(255) NULL;
ALTER TABLE workflow_comments ADD attachment_type VARCHAR2(100) NULL;

-- Add channel column with default 'in-app'
ALTER TABLE workflow_comments ADD channel VARCHAR2(20) DEFAULT 'in-app' NOT NULL;

-- Add comment to the table
COMMENT ON COLUMN workflow_comments.updated_at IS 'Timestamp when the comment was last edited';
COMMENT ON COLUMN workflow_comments.attachment_path IS 'Storage path for chat attachment file';
COMMENT ON COLUMN workflow_comments.attachment_name IS 'Original filename of the attachment';
COMMENT ON COLUMN workflow_comments.attachment_type IS 'MIME type of the attachment';
COMMENT ON COLUMN workflow_comments.channel IS 'Channel used to send the message (in-app, email, sms, whatsapp)';
