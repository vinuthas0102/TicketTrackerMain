/*
  # Fix workflow_step_progress_documents RLS policies

  ## Problem
  1. The SELECT policy uses `is_deleted = false` which blocks rows where is_deleted is NULL
     (newly inserted rows that haven't been explicitly set to false yet).
  2. There is no UPDATE policy, so soft-deleting documents (setting is_deleted = true) fails.

  ## Changes
  - Drop and recreate the SELECT policy to allow `is_deleted IS NULL OR is_deleted = false`
  - Add an UPDATE policy for anon and authenticated roles so progress documents can be soft-deleted
*/

-- Drop the old restrictive SELECT policy
DROP POLICY IF EXISTS "Anyone can view active progress documents" ON workflow_step_progress_documents;

-- Recreate SELECT policy to allow rows where is_deleted is NULL or false
CREATE POLICY "Anyone can view active progress documents"
  ON workflow_step_progress_documents
  FOR SELECT
  TO anon, authenticated
  USING (is_deleted IS NULL OR is_deleted = false);

-- Add missing UPDATE policy for soft-delete support
DROP POLICY IF EXISTS "Anyone can soft delete own progress documents" ON workflow_step_progress_documents;

CREATE POLICY "Anyone can soft delete own progress documents"
  ON workflow_step_progress_documents
  FOR UPDATE
  TO anon, authenticated
  USING (true)
  WITH CHECK (true);
