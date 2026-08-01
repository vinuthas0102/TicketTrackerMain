/*
# Update get_accessible_ticket_ids_for_user to filter by user regions

## Purpose
This migration updates the `get_accessible_ticket_ids_for_user` function so that
ALL roles (including EO) are filtered by the user's assigned regions. Previously,
EO saw all tickets unconditionally. Now, every role only sees tickets whose
`property_location` matches at least one of the user's assigned regions in
`user_regions`.

## Changes
- The EO branch now filters tickets by `property_location IN (SELECT region FROM user_regions WHERE user_id = p_user_id)`
- The dept_officer (DO) branch adds the same region filter
- The technician branch adds the same region filter
- The employee branch adds the same region filter
- The vendor branch adds the same region filter
- A finance role branch is added (previously fell through to default: no access)
- Tickets whose `property_location` does not match any of the user's regions
  are inaccessible (return empty array) until the ticket's location is corrected.

## Important Notes
1. EO is strictly limited to assigned regions — no override.
2. Region assignment is mandatory for all roles (enforced at application layer).
3. If a user has no regions assigned, they see no tickets.
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
  user_regions TEXT[];
BEGIN
  -- Get user role from database
  SELECT role INTO user_role FROM users WHERE id = p_user_id;

  -- Handle case where user not found
  IF user_role IS NULL THEN
    RETURN ARRAY[]::uuid[];
  END IF;

  -- Get the user's assigned regions
  SELECT array_agg(region) INTO user_regions
  FROM user_regions WHERE user_id = p_user_id;

  -- If user has no regions assigned, they see no tickets
  IF user_regions IS NULL OR array_length(user_regions, 1) IS NULL THEN
    RETURN ARRAY[]::uuid[];
  END IF;

  -- EO (Executive Officer) sees all tickets within their assigned regions
  IF user_role = 'eo' THEN
    SELECT array_agg(id) INTO ticket_ids
    FROM tickets
    WHERE property_location = ANY(user_regions);
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- DO (Department Officer) sees tickets created by, assigned to, or with workflow steps assigned,
  -- within their assigned regions
  IF user_role = 'dept_officer' THEN
    SELECT array_agg(DISTINCT id) INTO ticket_ids
    FROM tickets
    WHERE property_location = ANY(user_regions)
      AND (
        created_by = p_user_id
        OR assigned_to = p_user_id
        OR id IN (SELECT ticket_id FROM workflow_steps WHERE assigned_to = p_user_id)
      );
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- TECHNICIAN sees tickets created by, assigned to, or with workflow steps assigned,
  -- within their assigned regions
  IF user_role = 'technician' THEN
    SELECT array_agg(DISTINCT id) INTO ticket_ids
    FROM tickets
    WHERE property_location = ANY(user_regions)
      AND (
        created_by = p_user_id
        OR assigned_to = p_user_id
        OR id IN (SELECT ticket_id FROM workflow_steps WHERE assigned_to = p_user_id)
      );
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- EMPLOYEE sees own tickets (created by or assigned to), within their assigned regions
  IF user_role = 'employee' THEN
    SELECT array_agg(id) INTO ticket_ids
    FROM tickets
    WHERE property_location = ANY(user_regions)
      AND (created_by = p_user_id OR assigned_to = p_user_id);
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- VENDOR sees tickets with assigned workflow steps, within their assigned regions
  IF user_role = 'vendor' THEN
    SELECT array_agg(DISTINCT t.id) INTO ticket_ids
    FROM tickets t
    INNER JOIN workflow_steps ws ON ws.ticket_id = t.id
    WHERE t.property_location = ANY(user_regions)
      AND ws.assigned_to = p_user_id;
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- FINANCE sees all tickets within their assigned regions
  IF user_role = 'finance' THEN
    SELECT array_agg(id) INTO ticket_ids
    FROM tickets
    WHERE property_location = ANY(user_regions);
    RETURN COALESCE(ticket_ids, ARRAY[]::uuid[]);
  END IF;

  -- Default: no access
  RETURN ARRAY[]::uuid[];
END;
$$;

COMMENT ON FUNCTION get_accessible_ticket_ids_for_user IS 'Returns array of ticket IDs accessible to a user based on their role AND assigned regions. All roles (including EO) are filtered by property_location matching user_regions. Returns empty array if user has no access, no regions, or does not exist.';
