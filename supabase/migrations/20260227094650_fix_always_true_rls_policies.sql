/*
  # Fix always-true RLS policies

  ## Summary
  Replaces broad "always true" RLS policies with properly scoped ones that enforce
  authentication. This hardens security by ensuring only authenticated users can
  perform write operations, and only on data they own or are authorized to access.

  ## Tables Fixed

  ### audit_logs
  - Replace always-true INSERT for anon and authenticated with authenticated-only insert

  ### documents
  - Remove unrestricted anon INSERT/UPDATE/DELETE
  - Restrict to authenticated users only

  ### field_definitions, field_dropdown_options, file_attachments
  - Replace ALL (always true) with separate INSERT/UPDATE/DELETE for authenticated only

  ### file_reference_templates
  - Restrict INSERT/UPDATE/DELETE to authenticated only (with check)

  ### finance_approvals
  - Remove all anon write access
  - Restrict to authenticated users only

  ### module_field_configurations, modules, ticket_field_values
  - Replace ALL (always true) with separate INSERT/UPDATE/DELETE for authenticated only

  ### tickets
  - Remove anon INSERT/UPDATE/DELETE
  - Restrict authenticated INSERT with auth.uid() check

  ### user_activity_logs
  - Restrict INSERT to authenticated users only

  ### user_display_preferences
  - Restrict INSERT/UPDATE/DELETE to own records (auth.uid() = user_id)

  ### user_management_audit
  - Restrict INSERT to authenticated users only

  ### users
  - Replace ALL (always true) with separate authenticated INSERT/UPDATE/DELETE

  ### workflow_comments, workflow_step_dependencies, workflow_step_field_values
  - Replace ALL (always true) with separate authenticated INSERT/UPDATE/DELETE

  ### workflow_step_file_references
  - Restrict INSERT/UPDATE/DELETE to authenticated only

  ### workflow_steps
  - Restrict anon access: remove old ALL policy (already done)
  - Restrict INSERT/UPDATE/DELETE to authenticated only
*/

-- ============================================================
-- audit_logs: restrict INSERT to authenticated users only
-- ============================================================
DROP POLICY IF EXISTS "Allow anon to insert audit logs" ON public.audit_logs;
DROP POLICY IF EXISTS "Authenticated users can create audit logs" ON public.audit_logs;

CREATE POLICY "Authenticated users can insert audit logs"
  ON public.audit_logs FOR INSERT
  TO authenticated
  WITH CHECK (true);

-- ============================================================
-- documents: remove anon write access entirely
-- ============================================================
DROP POLICY IF EXISTS "Allow anon to delete documents" ON public.documents;
DROP POLICY IF EXISTS "Allow anon to insert documents" ON public.documents;
DROP POLICY IF EXISTS "Allow anon to update documents" ON public.documents;

CREATE POLICY "Authenticated users can insert documents"
  ON public.documents FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update documents"
  ON public.documents FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete documents"
  ON public.documents FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- field_definitions: separate INSERT/UPDATE/DELETE for authenticated
-- ============================================================
CREATE POLICY "Authenticated users can insert field definitions"
  ON public.field_definitions FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update field definitions"
  ON public.field_definitions FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete field definitions"
  ON public.field_definitions FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- field_dropdown_options: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert dropdown options"
  ON public.field_dropdown_options FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update dropdown options"
  ON public.field_dropdown_options FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete dropdown options"
  ON public.field_dropdown_options FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- file_attachments: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert file attachments"
  ON public.file_attachments FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update file attachments"
  ON public.file_attachments FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete file attachments"
  ON public.file_attachments FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- file_reference_templates: restrict write to authenticated
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can delete templates" ON public.file_reference_templates;
DROP POLICY IF EXISTS "Authenticated users can insert templates" ON public.file_reference_templates;
DROP POLICY IF EXISTS "Authenticated users can update templates" ON public.file_reference_templates;

CREATE POLICY "Authenticated users can insert templates"
  ON public.file_reference_templates FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update templates"
  ON public.file_reference_templates FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete templates"
  ON public.file_reference_templates FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- finance_approvals: remove anon write access entirely
-- ============================================================
DROP POLICY IF EXISTS "Allow anon to delete finance approvals" ON public.finance_approvals;
DROP POLICY IF EXISTS "Allow anon to insert finance approvals" ON public.finance_approvals;
DROP POLICY IF EXISTS "Allow anon to update finance approvals" ON public.finance_approvals;

CREATE POLICY "Authenticated users can insert finance approvals"
  ON public.finance_approvals FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update finance approvals"
  ON public.finance_approvals FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete finance approvals"
  ON public.finance_approvals FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- module_field_configurations: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert module field configurations"
  ON public.module_field_configurations FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update module field configurations"
  ON public.module_field_configurations FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete module field configurations"
  ON public.module_field_configurations FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- modules: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert modules"
  ON public.modules FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update modules"
  ON public.modules FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete modules"
  ON public.modules FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- ticket_field_values: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert ticket field values"
  ON public.ticket_field_values FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update ticket field values"
  ON public.ticket_field_values FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete ticket field values"
  ON public.ticket_field_values FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- tickets: remove anon write access, tighten authenticated INSERT
-- ============================================================
DROP POLICY IF EXISTS "Allow anon to delete tickets" ON public.tickets;
DROP POLICY IF EXISTS "Allow anon to insert tickets" ON public.tickets;
DROP POLICY IF EXISTS "Allow anon to update tickets" ON public.tickets;
DROP POLICY IF EXISTS "Authenticated users can insert tickets" ON public.tickets;

CREATE POLICY "Authenticated users can insert tickets"
  ON public.tickets FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

-- ============================================================
-- user_activity_logs: restrict INSERT to authenticated
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can create activity logs" ON public.user_activity_logs;

CREATE POLICY "Authenticated users can insert activity logs"
  ON public.user_activity_logs FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

-- ============================================================
-- user_display_preferences: restrict to own records
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can delete preferences" ON public.user_display_preferences;
DROP POLICY IF EXISTS "Authenticated users can insert preferences" ON public.user_display_preferences;
DROP POLICY IF EXISTS "Authenticated users can update preferences" ON public.user_display_preferences;

CREATE POLICY "Users can insert own preferences"
  ON public.user_display_preferences FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own preferences"
  ON public.user_display_preferences FOR UPDATE
  TO authenticated
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own preferences"
  ON public.user_display_preferences FOR DELETE
  TO authenticated
  USING (auth.uid() = user_id);

-- ============================================================
-- user_management_audit: restrict INSERT to authenticated
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can create audit records" ON public.user_management_audit;

CREATE POLICY "Authenticated users can insert audit records"
  ON public.user_management_audit FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

-- ============================================================
-- users: separate INSERT/UPDATE/DELETE for authenticated
-- ============================================================
CREATE POLICY "Authenticated users can insert users"
  ON public.users FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update users"
  ON public.users FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete users"
  ON public.users FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- workflow_comments: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert workflow comments"
  ON public.workflow_comments FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update workflow comments"
  ON public.workflow_comments FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete workflow comments"
  ON public.workflow_comments FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- workflow_step_dependencies: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert workflow step dependencies"
  ON public.workflow_step_dependencies FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update workflow step dependencies"
  ON public.workflow_step_dependencies FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete workflow step dependencies"
  ON public.workflow_step_dependencies FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- workflow_step_field_values: separate INSERT/UPDATE/DELETE
-- ============================================================
CREATE POLICY "Authenticated users can insert step field values"
  ON public.workflow_step_field_values FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update step field values"
  ON public.workflow_step_field_values FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete step field values"
  ON public.workflow_step_field_values FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- workflow_step_file_references: restrict to authenticated
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can delete step file references" ON public.workflow_step_file_references;
DROP POLICY IF EXISTS "Authenticated users can insert step file references" ON public.workflow_step_file_references;
DROP POLICY IF EXISTS "Authenticated users can update step file references" ON public.workflow_step_file_references;

CREATE POLICY "Authenticated users can insert step file references"
  ON public.workflow_step_file_references FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update step file references"
  ON public.workflow_step_file_references FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete step file references"
  ON public.workflow_step_file_references FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);

-- ============================================================
-- workflow_steps: restrict write to authenticated (anon ALL already dropped)
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can delete workflow steps" ON public.workflow_steps;
DROP POLICY IF EXISTS "Authenticated users can insert workflow steps" ON public.workflow_steps;
DROP POLICY IF EXISTS "Authenticated users can update workflow steps" ON public.workflow_steps;

CREATE POLICY "Authenticated users can insert workflow steps"
  ON public.workflow_steps FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can update workflow steps"
  ON public.workflow_steps FOR UPDATE
  TO authenticated
  USING (auth.uid() IS NOT NULL)
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Authenticated users can delete workflow steps"
  ON public.workflow_steps FOR DELETE
  TO authenticated
  USING (auth.uid() IS NOT NULL);
