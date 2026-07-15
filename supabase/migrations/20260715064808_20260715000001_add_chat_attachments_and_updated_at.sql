/*
  # Add Chat Attachments, Channel, and Updated At to workflow_comments

  ## Overview
  Extends the workflow_comments table to support:
  - File attachments on chat messages (path, name, type)
  - Channel tracking (in-app, email, sms, whatsapp)
  - "Edited" indicator via updated_at timestamp

  ## Changes to workflow_comments table
  1. `updated_at` (timestamptz, nullable) - set when a comment is edited
  2. `attachment_path` (text, nullable) - storage path in chat-attachments bucket
  3. `attachment_name` (text, nullable) - original filename
  4. `attachment_type` (text, nullable) - MIME type
  5. `channel` (text, default 'in-app') - which channel the message was sent via

  ## Security
  - No RLS policy changes needed; existing permissive anon/authenticated policies remain.
  - Application-level authorization enforces who can post to which conversation.
*/

-- Add updated_at column
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'workflow_comments' AND column_name = 'updated_at'
  ) THEN
    ALTER TABLE workflow_comments ADD COLUMN updated_at timestamptz;
  END IF;
END $$;

-- Add attachment_path column
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'workflow_comments' AND column_name = 'attachment_path'
  ) THEN
    ALTER TABLE workflow_comments ADD COLUMN attachment_path text;
  END IF;
END $$;

-- Add attachment_name column
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'workflow_comments' AND column_name = 'attachment_name'
  ) THEN
    ALTER TABLE workflow_comments ADD COLUMN attachment_name text;
  END IF;
END $$;

-- Add attachment_type column
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'workflow_comments' AND column_name = 'attachment_type'
  ) THEN
    ALTER TABLE workflow_comments ADD COLUMN attachment_type text;
  END IF;
END $$;

-- Add channel column with default 'in-app'
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'workflow_comments' AND column_name = 'channel'
  ) THEN
    ALTER TABLE workflow_comments ADD COLUMN channel text NOT NULL DEFAULT 'in-app';
  END IF;
END $$;
