/*
  # Fix Auth RLS Initialization Plan and always-true policies

  ## Summary
  Replaces all `auth.uid()` calls in RLS policies with `(select auth.uid())` so Postgres
  can evaluate the function once per query rather than once per row. Also fixes the
  audit_logs INSERT policy that still had an always-true WITH CHECK.

  ## Tables fixed
  - documents (insert, update, delete)
  - field_definitions (insert, update, delete)
  - field_dropdown_options (insert, update, delete)
  - file_attachments (insert, update, delete)
  - file_reference_templates (insert, update, delete)
  - finance_approvals (insert, update, delete)
  - module_field_configurations (insert, update, delete)
  - modules (insert, update, delete)
  - ticket_field_values (insert, update, delete)
  - tickets (insert, update)
  - user_activity_logs (insert)
  - user_display_preferences (insert, update, delete)
  - user_management_audit (insert)
  - users (insert, update, delete)
  - workflow_comments (insert, update, delete)
  - workflow_step_dependencies (insert, update, delete)
  - workflow_step_field_values (insert, update, delete)
  - workflow_step_file_references (insert, update, delete)
  - workflow_steps (insert, update, delete)
  - audit_logs (insert - fix always-true)
*/

-- ============================================================
-- audit_logs
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert audit logs" ON public.audit_logs;
CREATE POLICY "Authenticated users can insert audit logs"
  ON public.audit_logs FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- documents
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert documents" ON public.documents;
DROP POLICY IF EXISTS "Authenticated users can update documents" ON public.documents;
DROP POLICY IF EXISTS "Authenticated users can delete documents" ON public.documents;

CREATE POLICY "Authenticated users can insert documents"
  ON public.documents FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update documents"
  ON public.documents FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete documents"
  ON public.documents FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- field_definitions
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert field definitions" ON public.field_definitions;
DROP POLICY IF EXISTS "Authenticated users can update field definitions" ON public.field_definitions;
DROP POLICY IF EXISTS "Authenticated users can delete field definitions" ON public.field_definitions;

CREATE POLICY "Authenticated users can insert field definitions"
  ON public.field_definitions FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update field definitions"
  ON public.field_definitions FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete field definitions"
  ON public.field_definitions FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- field_dropdown_options
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert dropdown options" ON public.field_dropdown_options;
DROP POLICY IF EXISTS "Authenticated users can update dropdown options" ON public.field_dropdown_options;
DROP POLICY IF EXISTS "Authenticated users can delete dropdown options" ON public.field_dropdown_options;

CREATE POLICY "Authenticated users can insert dropdown options"
  ON public.field_dropdown_options FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update dropdown options"
  ON public.field_dropdown_options FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete dropdown options"
  ON public.field_dropdown_options FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- file_attachments
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert file attachments" ON public.file_attachments;
DROP POLICY IF EXISTS "Authenticated users can update file attachments" ON public.file_attachments;
DROP POLICY IF EXISTS "Authenticated users can delete file attachments" ON public.file_attachments;

CREATE POLICY "Authenticated users can insert file attachments"
  ON public.file_attachments FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update file attachments"
  ON public.file_attachments FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete file attachments"
  ON public.file_attachments FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- file_reference_templates
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert templates" ON public.file_reference_templates;
DROP POLICY IF EXISTS "Authenticated users can update templates" ON public.file_reference_templates;
DROP POLICY IF EXISTS "Authenticated users can delete templates" ON public.file_reference_templates;

CREATE POLICY "Authenticated users can insert templates"
  ON public.file_reference_templates FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update templates"
  ON public.file_reference_templates FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete templates"
  ON public.file_reference_templates FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- finance_approvals
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert finance approvals" ON public.finance_approvals;
DROP POLICY IF EXISTS "Authenticated users can update finance approvals" ON public.finance_approvals;
DROP POLICY IF EXISTS "Authenticated users can delete finance approvals" ON public.finance_approvals;

CREATE POLICY "Authenticated users can insert finance approvals"
  ON public.finance_approvals FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update finance approvals"
  ON public.finance_approvals FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete finance approvals"
  ON public.finance_approvals FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- module_field_configurations
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert module field configurations" ON public.module_field_configurations;
DROP POLICY IF EXISTS "Authenticated users can update module field configurations" ON public.module_field_configurations;
DROP POLICY IF EXISTS "Authenticated users can delete module field configurations" ON public.module_field_configurations;

CREATE POLICY "Authenticated users can insert module field configurations"
  ON public.module_field_configurations FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update module field configurations"
  ON public.module_field_configurations FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete module field configurations"
  ON public.module_field_configurations FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- modules
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert modules" ON public.modules;
DROP POLICY IF EXISTS "Authenticated users can update modules" ON public.modules;
DROP POLICY IF EXISTS "Authenticated users can delete modules" ON public.modules;

CREATE POLICY "Authenticated users can insert modules"
  ON public.modules FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update modules"
  ON public.modules FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete modules"
  ON public.modules FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- ticket_field_values
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert ticket field values" ON public.ticket_field_values;
DROP POLICY IF EXISTS "Authenticated users can update ticket field values" ON public.ticket_field_values;
DROP POLICY IF EXISTS "Authenticated users can delete ticket field values" ON public.ticket_field_values;

CREATE POLICY "Authenticated users can insert ticket field values"
  ON public.ticket_field_values FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update ticket field values"
  ON public.ticket_field_values FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete ticket field values"
  ON public.ticket_field_values FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- tickets
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert tickets" ON public.tickets;
DROP POLICY IF EXISTS "Authenticated users can update tickets" ON public.tickets;

CREATE POLICY "Authenticated users can insert tickets"
  ON public.tickets FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- user_activity_logs
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert activity logs" ON public.user_activity_logs;

CREATE POLICY "Authenticated users can insert activity logs"
  ON public.user_activity_logs FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- user_display_preferences
-- ============================================================
DROP POLICY IF EXISTS "Users can insert own preferences" ON public.user_display_preferences;
DROP POLICY IF EXISTS "Users can update own preferences" ON public.user_display_preferences;
DROP POLICY IF EXISTS "Users can delete own preferences" ON public.user_display_preferences;

CREATE POLICY "Users can insert own preferences"
  ON public.user_display_preferences FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) = user_id);

CREATE POLICY "Users can update own preferences"
  ON public.user_display_preferences FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) = user_id)
  WITH CHECK ((select auth.uid()) = user_id);

CREATE POLICY "Users can delete own preferences"
  ON public.user_display_preferences FOR DELETE
  TO authenticated
  USING ((select auth.uid()) = user_id);

-- ============================================================
-- user_management_audit
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert audit records" ON public.user_management_audit;

CREATE POLICY "Authenticated users can insert audit records"
  ON public.user_management_audit FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- users
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert users" ON public.users;
DROP POLICY IF EXISTS "Authenticated users can update users" ON public.users;
DROP POLICY IF EXISTS "Authenticated users can delete users" ON public.users;

CREATE POLICY "Authenticated users can insert users"
  ON public.users FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update users"
  ON public.users FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete users"
  ON public.users FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- workflow_comments
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert workflow comments" ON public.workflow_comments;
DROP POLICY IF EXISTS "Authenticated users can update workflow comments" ON public.workflow_comments;
DROP POLICY IF EXISTS "Authenticated users can delete workflow comments" ON public.workflow_comments;

CREATE POLICY "Authenticated users can insert workflow comments"
  ON public.workflow_comments FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update workflow comments"
  ON public.workflow_comments FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete workflow comments"
  ON public.workflow_comments FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- workflow_step_dependencies
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert workflow step dependencies" ON public.workflow_step_dependencies;
DROP POLICY IF EXISTS "Authenticated users can update workflow step dependencies" ON public.workflow_step_dependencies;
DROP POLICY IF EXISTS "Authenticated users can delete workflow step dependencies" ON public.workflow_step_dependencies;

CREATE POLICY "Authenticated users can insert workflow step dependencies"
  ON public.workflow_step_dependencies FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update workflow step dependencies"
  ON public.workflow_step_dependencies FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete workflow step dependencies"
  ON public.workflow_step_dependencies FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- workflow_step_field_values
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert step field values" ON public.workflow_step_field_values;
DROP POLICY IF EXISTS "Authenticated users can update step field values" ON public.workflow_step_field_values;
DROP POLICY IF EXISTS "Authenticated users can delete step field values" ON public.workflow_step_field_values;

CREATE POLICY "Authenticated users can insert step field values"
  ON public.workflow_step_field_values FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update step field values"
  ON public.workflow_step_field_values FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete step field values"
  ON public.workflow_step_field_values FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- workflow_step_file_references
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert step file references" ON public.workflow_step_file_references;
DROP POLICY IF EXISTS "Authenticated users can update step file references" ON public.workflow_step_file_references;
DROP POLICY IF EXISTS "Authenticated users can delete step file references" ON public.workflow_step_file_references;

CREATE POLICY "Authenticated users can insert step file references"
  ON public.workflow_step_file_references FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update step file references"
  ON public.workflow_step_file_references FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete step file references"
  ON public.workflow_step_file_references FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);

-- ============================================================
-- workflow_steps
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert workflow steps" ON public.workflow_steps;
DROP POLICY IF EXISTS "Authenticated users can update workflow steps" ON public.workflow_steps;
DROP POLICY IF EXISTS "Authenticated users can delete workflow steps" ON public.workflow_steps;

CREATE POLICY "Authenticated users can insert workflow steps"
  ON public.workflow_steps FOR INSERT
  TO authenticated
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can update workflow steps"
  ON public.workflow_steps FOR UPDATE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL)
  WITH CHECK ((select auth.uid()) IS NOT NULL);

CREATE POLICY "Authenticated users can delete workflow steps"
  ON public.workflow_steps FOR DELETE
  TO authenticated
  USING ((select auth.uid()) IS NOT NULL);
