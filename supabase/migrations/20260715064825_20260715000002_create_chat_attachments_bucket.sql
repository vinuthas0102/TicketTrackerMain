/*
  # Create chat-attachments Storage Bucket

  ## Overview
  Creates a private Supabase storage bucket for chat message file attachments.

  ## Bucket Configuration
  - Name: chat-attachments
  - Public: false (files accessed via signed URLs)
  - File size limit: 5MB (5,242,880 bytes)
  - Allowed MIME types: PDF, JPEG, JPG, PNG, GIF, Word (DOC/DOCX), Excel (XLS/XLSX), ZIP

  ## Folder Structure
  - `{step_id}/{timestamp}_{filename}` - one folder per workflow step

  ## Security
  - RLS policies on storage.objects allow public (anon + authenticated) CRUD
    for objects in the chat-attachments bucket, matching the existing step-documents pattern.
  - Application-level authorization controls who can upload/download.
*/

-- ================================================================
-- STEP 1: Create Storage Bucket
-- ================================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'chat-attachments',
  'chat-attachments',
  false,
  5242880,
  ARRAY[
    'application/pdf',
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/zip',
    'application/x-zip-compressed'
  ]
)
ON CONFLICT (id) DO NOTHING;

-- ================================================================
-- STEP 2: Create RLS Policies for Storage Bucket
-- ================================================================

-- Drop existing policies if they exist (idempotent)
DROP POLICY IF EXISTS "Allow uploads to chat-attachments bucket" ON storage.objects;
DROP POLICY IF EXISTS "Allow reads from chat-attachments bucket" ON storage.objects;
DROP POLICY IF EXISTS "Allow updates to chat-attachments bucket" ON storage.objects;
DROP POLICY IF EXISTS "Allow deletes from chat-attachments bucket" ON storage.objects;

-- Policy: Allow uploads to chat-attachments bucket
CREATE POLICY "Allow uploads to chat-attachments bucket"
  ON storage.objects
  FOR INSERT
  TO public
  WITH CHECK (bucket_id = 'chat-attachments');

-- Policy: Allow reads from chat-attachments bucket
CREATE POLICY "Allow reads from chat-attachments bucket"
  ON storage.objects
  FOR SELECT
  TO public
  USING (bucket_id = 'chat-attachments');

-- Policy: Allow updates to chat-attachments bucket
CREATE POLICY "Allow updates to chat-attachments bucket"
  ON storage.objects
  FOR UPDATE
  TO public
  USING (bucket_id = 'chat-attachments')
  WITH CHECK (bucket_id = 'chat-attachments');

-- Policy: Allow deletes from chat-attachments bucket
CREATE POLICY "Allow deletes from chat-attachments bucket"
  ON storage.objects
  FOR DELETE
  TO public
  USING (bucket_id = 'chat-attachments');
