/*
# Update Maintenance Tracker categories and migrate existing tickets

## Summary
Replaces the category list for the "Maintenance Tracker" module with 12 new
facility/maintenance categories. Existing Maintenance Tracker tickets whose
category is no longer in the new list (e.g. "General", "Electrical", "Plumbing",
"HVAC", "General Maintenance", "Equipment Repair") are remapped to
"Civil Maintenance" so they remain valid under the new dropdown.

## Changes
1. `modules.config.categories` for the "Maintenance Tracker" module is replaced
   with the new 12-category list:
     - Civil Maintenance
     - Electrical Maintenance
     - Plumbing & Sanitary
     - Carpentry
     - HVAC / Air Conditioning
     - Water Supply
     - Sewage & Drainage
     - Road & External Area
     - Housekeeping, Fire & Safety
     - Security Systems
     - Street Lighting
     - Utility Services
2. `tickets.data->>'category'` for Maintenance Tracker tickets whose current
   category is not part of the new list is updated to "Civil Maintenance".
   Categories are stored inside the `tickets.data` jsonb column.

## Security
- No RLS or policy changes. Existing policies remain in force.
- This migration only updates data (module config and ticket data jsonb).

## Important notes
1. Only the "Maintenance Tracker" module is affected. The other modules
   (Complaints Tracker, Grievances Management, RTI Tracker, Project Execution
   Platform) keep their existing category lists.
2. The new category list is stored as a JSON array in `modules.config.categories`.
3. Ticket categories live in `tickets.data` jsonb under the `category` key.
*/

-- 1. Replace the Maintenance Tracker module's category list with the new 12 categories.
UPDATE modules
SET config = jsonb_set(
      config,
      '{categories}',
      '[
        "Civil Maintenance",
        "Electrical Maintenance",
        "Plumbing & Sanitary",
        "Carpentry",
        "HVAC / Air Conditioning",
        "Water Supply",
        "Sewage & Drainage",
        "Road & External Area",
        "Housekeeping, Fire & Safety",
        "Security Systems",
        "Street Lighting",
        "Utility Services"
      ]'::jsonb
    ),
    updated_at = now()
WHERE name = 'Maintenance Tracker';

-- 2. Remap Maintenance Tracker tickets whose category is not in the new list to
--    "Civil Maintenance". Categories are stored in tickets.data->>'category'.
UPDATE tickets
SET data = jsonb_set(data, '{category}', '"Civil Maintenance"'::jsonb),
    updated_at = now()
WHERE module_id = (
        SELECT id FROM modules WHERE name = 'Maintenance Tracker'
      )
  AND data ? 'category'
  AND data->>'category' IS DISTINCT FROM 'Civil Maintenance'
  AND data->>'category' NOT IN (
        'Civil Maintenance',
        'Electrical Maintenance',
        'Plumbing & Sanitary',
        'Carpentry',
        'HVAC / Air Conditioning',
        'Water Supply',
        'Sewage & Drainage',
        'Road & External Area',
        'Housekeeping, Fire & Safety',
        'Security Systems',
        'Street Lighting',
        'Utility Services'
      );
