/*
  # Consolidate duplicate permissive SELECT RLS policies

  ## Summary
  Multiple tables had two overlapping SELECT policies for the same roles, causing
  the "Multiple Permissive Policies" warning. This migration drops the redundant
  duplicate policies, keeping only one consolidated policy per table/role combination.

  ## Tables Fixed
  1. audit_logs - drop "Allow anon to select audit logs" (keep "Allow anonymous users to view audit logs")
  2. field_definitions - drop "Authenticated users can manage field definitions" SELECT overlap
     (keep "Anyone can view field definitions")
  3. field_dropdown_options - drop "Authenticated users can manage dropdown options" SELECT overlap
  4. file_attachments - drop "Authenticated users can manage file attachments" SELECT overlap
  5. file_reference_templates - drop "Anyone can view templates" (keep "Allow public read of active file reference templates")
  6. module_field_configurations - drop "Authenticated users can manage module field configurations" SELECT overlap
  7. modules - drop "Authenticated users can manage modules" SELECT overlap
  8. ticket_field_values - drop "Authenticated users can manage ticket field values" SELECT overlap
  9. tickets - drop "Allow anon to select tickets" (keep "Users can view tickets")
  10. users - drop "Allow anon to select users" (keep "Anyone can view users"), drop "Authenticated users can manage users" SELECT overlap
  11. workflow_comments - drop "Authenticated users can manage workflow comments" SELECT overlap
  12. workflow_step_dependencies - drop "Authenticated users can manage dependencies" SELECT overlap
  13. workflow_step_field_values - drop "Authenticated users can manage step field values" SELECT overlap
  14. workflow_steps - drop "Allow anon full access to workflow steps" ALL policy (will replace with scoped policies)

  ## Notes
  - The ALL policies that cause SELECT duplicates will be replaced by separate INSERT/UPDATE/DELETE policies
    in the next migration (fix_always_true_rls_policies)
  - For now we drop the conflicting ALL policies that include SELECT for authenticated users on tables
    where a public SELECT already exists
*/

-- audit_logs: drop the narrower anon-only duplicate (keep the broader anon+authenticated one)
DROP POLICY IF EXISTS "Allow anon to select audit logs" ON public.audit_logs;

-- tickets: drop the narrower anon-only duplicate (keep "Users can view tickets" which is public)
DROP POLICY IF EXISTS "Allow anon to select tickets" ON public.tickets;

-- users: drop the narrower anon-only duplicate (keep "Anyone can view users")
DROP POLICY IF EXISTS "Allow anon to select users" ON public.users;

-- file_reference_templates: drop "Anyone can view templates" (keep the more restrictive is_active=true policy)
DROP POLICY IF EXISTS "Anyone can view templates" ON public.file_reference_templates;

-- The ALL policies below create SELECT duplicates with the public SELECT policies.
-- Drop and re-create them as separate INSERT/UPDATE/DELETE policies (no SELECT) in next migration.
-- For now, drop the ALL policies that are conflicting:
DROP POLICY IF EXISTS "Authenticated users can manage field definitions" ON public.field_definitions;
DROP POLICY IF EXISTS "Authenticated users can manage dropdown options" ON public.field_dropdown_options;
DROP POLICY IF EXISTS "Authenticated users can manage file attachments" ON public.file_attachments;
DROP POLICY IF EXISTS "Authenticated users can manage module field configurations" ON public.module_field_configurations;
DROP POLICY IF EXISTS "Authenticated users can manage modules" ON public.modules;
DROP POLICY IF EXISTS "Authenticated users can manage ticket field values" ON public.ticket_field_values;
DROP POLICY IF EXISTS "Authenticated users can manage users" ON public.users;
DROP POLICY IF EXISTS "Authenticated users can manage workflow comments" ON public.workflow_comments;
DROP POLICY IF EXISTS "Authenticated users can manage dependencies" ON public.workflow_step_dependencies;
DROP POLICY IF EXISTS "Authenticated users can manage step field values" ON public.workflow_step_field_values;

-- Replace the anon ALL policy on workflow_steps with separate scoped policies
DROP POLICY IF EXISTS "Allow anon full access to workflow steps" ON public.workflow_steps;
