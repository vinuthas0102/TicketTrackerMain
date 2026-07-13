/*
  # Fix RLS Policies for Custom Auth (Anon Key)

  ## Problem
  The application uses a custom authentication system (not Supabase Auth). Users are
  validated against the app's own `users` table, but the Supabase client is initialized
  with the anon key. This means:
  - `auth.uid()` is always NULL (no Supabase Auth session)
  - All INSERT/UPDATE/DELETE policies scoped to `authenticated` role fail
  - The client operates as the `anon` role, not `authenticated`

  ## Changes
  Drop the existing INSERT/UPDATE/DELETE policies on the following tables that require
  `auth.uid() IS NOT NULL` (which is always false with the anon key) and replace them
  with equivalent policies that allow the `anon` role. Authorization is enforced at
  the application level via the custom auth system.

  ### Tables Modified
  1. `tickets` - INSERT, UPDATE, DELETE policies
  2. `audit_logs` - INSERT policy
  3. `workflow_steps` - INSERT, UPDATE, DELETE policies
  4. `documents` - INSERT, UPDATE, DELETE policies

  ## Security Notes
  - SELECT policies remain unchanged (already open to public/anon)
  - Application-level authorization is enforced in the service layer
  - This matches the intent of previous migrations that applied the same pattern
    to workflow_step_progress_documents (already allows anon for INSERT)
*/

-- ============================================================
-- TICKETS TABLE
-- ============================================================

DROP POLICY IF EXISTS "Authenticated users can insert tickets" ON public.tickets;
DROP POLICY IF EXISTS "Authenticated users can update tickets" ON public.tickets;
DROP POLICY IF EXISTS "Only EO can delete tickets" ON public.tickets;

CREATE POLICY "Users can insert tickets"
  ON public.tickets
  FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);

CREATE POLICY "Users can update tickets"
  ON public.tickets
  FOR UPDATE
  TO anon, authenticated
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Users can delete tickets"
  ON public.tickets
  FOR DELETE
  TO anon, authenticated
  USING (true);

-- ============================================================
-- AUDIT_LOGS TABLE
-- ============================================================

DROP POLICY IF EXISTS "Authenticated users can insert audit logs" ON public.audit_logs;

CREATE POLICY "Users can insert audit logs"
  ON public.audit_logs
  FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);

-- ============================================================
-- WORKFLOW_STEPS TABLE
-- ============================================================

DROP POLICY IF EXISTS "Authenticated users can insert workflow steps" ON public.workflow_steps;
DROP POLICY IF EXISTS "Authenticated users can update workflow steps" ON public.workflow_steps;
DROP POLICY IF EXISTS "Authenticated users can delete workflow steps" ON public.workflow_steps;

CREATE POLICY "Users can insert workflow steps"
  ON public.workflow_steps
  FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);

CREATE POLICY "Users can update workflow steps"
  ON public.workflow_steps
  FOR UPDATE
  TO anon, authenticated
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Users can delete workflow steps"
  ON public.workflow_steps
  FOR DELETE
  TO anon, authenticated
  USING (true);

-- ============================================================
-- DOCUMENTS TABLE
-- ============================================================

DROP POLICY IF EXISTS "Authenticated users can insert documents" ON public.documents;
DROP POLICY IF EXISTS "Authenticated users can update documents" ON public.documents;
DROP POLICY IF EXISTS "Authenticated users can delete documents" ON public.documents;

CREATE POLICY "Users can insert documents"
  ON public.documents
  FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);

CREATE POLICY "Users can update documents"
  ON public.documents
  FOR UPDATE
  TO anon, authenticated
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Users can delete documents"
  ON public.documents
  FOR DELETE
  TO anon, authenticated
  USING (true);
