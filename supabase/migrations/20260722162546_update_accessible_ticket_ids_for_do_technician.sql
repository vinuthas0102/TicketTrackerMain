/*
# Update get_accessible_ticket_ids_for_user for DO and TECHNICIAN roles

1. Purpose
- Previously, `dept_officer` (DO) users only saw tickets where they had workflow
  steps assigned to them. This missed tickets they created or tickets assigned
  to them at the ticket level.
- `technician` role was not handled at all and returned an empty array.
- This update makes both `dept_officer` and `technician` see tickets where:
  a) the user created the ticket (created_by = p_user_id), OR
  b) the ticket is assigned to the user (assigned_to = p_user_id), OR
  c) the user has a workflow step assigned to them on that ticket.

2. Changes
- DROP and recreate `get_accessible_ticket_ids_for_user` function.
- `dept_officer` branch: UNION of tickets created by, assigned to, or having
  a workflow step assigned to the user.
- `technician` branch: same logic as `dept_officer`.
- All other roles (eo, employee, vendor, finance) remain unchanged.

3. Security
- This is a SECURITY DEFINER function; no RLS policy changes.
- The function only returns ticket IDs, not ticket data; the caller still
  fetches tickets through normal RLS-protected queries.

4. Notes
- Idempotent: uses DROP FUNCTION IF EXISTS before recreating.
- Returns uuid[] (same return type as before).
*/

DROP FUNCTION IF EXISTS get_accessible_ticket_ids_for_user(UUID);

CREATE OR REPLACE FUNCTION get_accessible_ticket_ids_for_user(p_user_id UUID)
RETURNS uuid[]
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  user_role TEXT;
  ticket_ids uuid[];
BEGIN
  -- Get user role from database
  SELECT role INTO user_role FROM users WHERE id = p_user_id;

  -- Handle case where user not found
  IF user_role IS NULL THEN
    RETURN ARRAY[]::uuid[];
  END IF;

  -- EO (Executive Officer) sees all tickets
  IF user_role = 'eo' THEN
    SELECT array_agg(id) INTO ticket_ids FROM tickets;
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- DO (Department Officer) sees tickets created by, assigned to, or with workflow steps assigned
  IF user_role = 'dept_officer' THEN
    SELECT array_agg(DISTINCT id) INTO ticket_ids
    FROM tickets
    WHERE created_by = p_user_id
       OR assigned_to = p_user_id
       OR id IN (SELECT ticket_id FROM workflow_steps WHERE assigned_to = p_user_id);
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- TECHNICIAN sees tickets created by, assigned to, or with workflow steps assigned
  IF user_role = 'technician' THEN
    SELECT array_agg(DISTINCT id) INTO ticket_ids
    FROM tickets
    WHERE created_by = p_user_id
       OR assigned_to = p_user_id
       OR id IN (SELECT ticket_id FROM workflow_steps WHERE assigned_to = p_user_id);
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- EMPLOYEE sees own tickets (created by or assigned to)
  IF user_role = 'employee' THEN
    SELECT array_agg(id) INTO ticket_ids
    FROM tickets
    WHERE created_by = p_user_id OR assigned_to = p_user_id;
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- VENDOR sees tickets with assigned workflow steps
  IF user_role = 'vendor' THEN
    SELECT array_agg(DISTINCT ticket_id) INTO ticket_ids
    FROM workflow_steps
    WHERE assigned_to = p_user_id;
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- Default: no access
  RETURN ARRAY[]::uuid[];
END;
$$;

COMMENT ON FUNCTION get_accessible_ticket_ids_for_user IS 'Returns array of ticket IDs accessible to a user based on their role. DO and TECHNICIAN see tickets they created, are assigned to, or have workflow steps assigned. Returns empty array if user has no access or does not exist.';
