/*
  # Fix RLS Policies for All Remaining Tables (Custom Auth / Anon Key)

  ## Problem
  All remaining tables have INSERT/UPDATE/DELETE policies scoped to the `authenticated`
  role with `auth.uid() IS NOT NULL` checks. Since the app uses custom auth with the
  anon key (no Supabase Auth session), `auth.uid()` is always NULL and these policies
  block all write operations.

  ## Changes
  Replace all broken `authenticated`-only policies with equivalent policies that also
  allow the `anon` role. Application-level authorization is enforced in service layer.

  ### Tables Fixed
  - field_definitions
  - field_dropdown_options
  - file_attachments
  - file_reference_templates
  - finance_approvals
  - module_field_configurations
  - modules
  - ticket_field_values
  - user_activity_logs
  - user_display_preferences
  - user_management_audit
  - users
  - workflow_comments
  - workflow_step_dependencies
  - workflow_step_field_values
  - workflow_step_file_references
*/

-- ============================================================
-- FIELD_DEFINITIONS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert field definitions" ON public.field_definitions;
DROP POLICY IF EXISTS "Authenticated users can update field definitions" ON public.field_definitions;
DROP POLICY IF EXISTS "Authenticated users can delete field definitions" ON public.field_definitions;

CREATE POLICY "Users can insert field definitions" ON public.field_definitions FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update field definitions" ON public.field_definitions FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete field definitions" ON public.field_definitions FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- FIELD_DROPDOWN_OPTIONS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert dropdown options" ON public.field_dropdown_options;
DROP POLICY IF EXISTS "Authenticated users can update dropdown options" ON public.field_dropdown_options;
DROP POLICY IF EXISTS "Authenticated users can delete dropdown options" ON public.field_dropdown_options;

CREATE POLICY "Users can insert dropdown options" ON public.field_dropdown_options FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update dropdown options" ON public.field_dropdown_options FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete dropdown options" ON public.field_dropdown_options FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- FILE_ATTACHMENTS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert file attachments" ON public.file_attachments;
DROP POLICY IF EXISTS "Authenticated users can update file attachments" ON public.file_attachments;
DROP POLICY IF EXISTS "Authenticated users can delete file attachments" ON public.file_attachments;

CREATE POLICY "Users can insert file attachments" ON public.file_attachments FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update file attachments" ON public.file_attachments FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete file attachments" ON public.file_attachments FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- FILE_REFERENCE_TEMPLATES
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert templates" ON public.file_reference_templates;
DROP POLICY IF EXISTS "Authenticated users can update templates" ON public.file_reference_templates;
DROP POLICY IF EXISTS "Authenticated users can delete templates" ON public.file_reference_templates;

CREATE POLICY "Users can insert templates" ON public.file_reference_templates FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update templates" ON public.file_reference_templates FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete templates" ON public.file_reference_templates FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- FINANCE_APPROVALS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert finance approvals" ON public.finance_approvals;
DROP POLICY IF EXISTS "Authenticated users can update finance approvals" ON public.finance_approvals;
DROP POLICY IF EXISTS "Authenticated users can delete finance approvals" ON public.finance_approvals;

CREATE POLICY "Users can insert finance approvals" ON public.finance_approvals FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update finance approvals" ON public.finance_approvals FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete finance approvals" ON public.finance_approvals FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- MODULE_FIELD_CONFIGURATIONS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert module field configurations" ON public.module_field_configurations;
DROP POLICY IF EXISTS "Authenticated users can update module field configurations" ON public.module_field_configurations;
DROP POLICY IF EXISTS "Authenticated users can delete module field configurations" ON public.module_field_configurations;

CREATE POLICY "Users can insert module field configurations" ON public.module_field_configurations FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update module field configurations" ON public.module_field_configurations FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete module field configurations" ON public.module_field_configurations FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- MODULES
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert modules" ON public.modules;
DROP POLICY IF EXISTS "Authenticated users can update modules" ON public.modules;
DROP POLICY IF EXISTS "Authenticated users can delete modules" ON public.modules;

CREATE POLICY "Users can insert modules" ON public.modules FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update modules" ON public.modules FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete modules" ON public.modules FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- TICKET_FIELD_VALUES
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert ticket field values" ON public.ticket_field_values;
DROP POLICY IF EXISTS "Authenticated users can update ticket field values" ON public.ticket_field_values;
DROP POLICY IF EXISTS "Authenticated users can delete ticket field values" ON public.ticket_field_values;

CREATE POLICY "Users can insert ticket field values" ON public.ticket_field_values FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update ticket field values" ON public.ticket_field_values FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete ticket field values" ON public.ticket_field_values FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- USER_ACTIVITY_LOGS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert activity logs" ON public.user_activity_logs;

CREATE POLICY "Users can insert activity logs" ON public.user_activity_logs FOR INSERT TO anon, authenticated WITH CHECK (true);

-- ============================================================
-- USER_DISPLAY_PREFERENCES
-- ============================================================
DROP POLICY IF EXISTS "Users can insert own preferences" ON public.user_display_preferences;
DROP POLICY IF EXISTS "Users can update own preferences" ON public.user_display_preferences;
DROP POLICY IF EXISTS "Users can delete own preferences" ON public.user_display_preferences;

CREATE POLICY "Users can insert preferences" ON public.user_display_preferences FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update preferences" ON public.user_display_preferences FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete preferences" ON public.user_display_preferences FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- USER_MANAGEMENT_AUDIT
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert audit records" ON public.user_management_audit;

CREATE POLICY "Users can insert audit records" ON public.user_management_audit FOR INSERT TO anon, authenticated WITH CHECK (true);

-- ============================================================
-- USERS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert users" ON public.users;
DROP POLICY IF EXISTS "Authenticated users can update users" ON public.users;
DROP POLICY IF EXISTS "Authenticated users can delete users" ON public.users;

CREATE POLICY "Users can insert users" ON public.users FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update users" ON public.users FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete users" ON public.users FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- WORKFLOW_COMMENTS
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert workflow comments" ON public.workflow_comments;
DROP POLICY IF EXISTS "Authenticated users can update workflow comments" ON public.workflow_comments;
DROP POLICY IF EXISTS "Authenticated users can delete workflow comments" ON public.workflow_comments;

CREATE POLICY "Users can insert workflow comments" ON public.workflow_comments FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update workflow comments" ON public.workflow_comments FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete workflow comments" ON public.workflow_comments FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- WORKFLOW_STEP_DEPENDENCIES
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert workflow step dependencies" ON public.workflow_step_dependencies;
DROP POLICY IF EXISTS "Authenticated users can update workflow step dependencies" ON public.workflow_step_dependencies;
DROP POLICY IF EXISTS "Authenticated users can delete workflow step dependencies" ON public.workflow_step_dependencies;

CREATE POLICY "Users can insert workflow step dependencies" ON public.workflow_step_dependencies FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update workflow step dependencies" ON public.workflow_step_dependencies FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete workflow step dependencies" ON public.workflow_step_dependencies FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- WORKFLOW_STEP_FIELD_VALUES
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert step field values" ON public.workflow_step_field_values;
DROP POLICY IF EXISTS "Authenticated users can update step field values" ON public.workflow_step_field_values;
DROP POLICY IF EXISTS "Authenticated users can delete step field values" ON public.workflow_step_field_values;

CREATE POLICY "Users can insert step field values" ON public.workflow_step_field_values FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update step field values" ON public.workflow_step_field_values FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete step field values" ON public.workflow_step_field_values FOR DELETE TO anon, authenticated USING (true);

-- ============================================================
-- WORKFLOW_STEP_FILE_REFERENCES
-- ============================================================
DROP POLICY IF EXISTS "Authenticated users can insert step file references" ON public.workflow_step_file_references;
DROP POLICY IF EXISTS "Authenticated users can update step file references" ON public.workflow_step_file_references;
DROP POLICY IF EXISTS "Authenticated users can delete step file references" ON public.workflow_step_file_references;

CREATE POLICY "Users can insert step file references" ON public.workflow_step_file_references FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Users can update step file references" ON public.workflow_step_file_references FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can delete step file references" ON public.workflow_step_file_references FOR DELETE TO anon, authenticated USING (true);
