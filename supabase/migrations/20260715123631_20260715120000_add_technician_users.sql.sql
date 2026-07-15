/*
# Add Technician Users for Civil and Electrical Departments

1. Purpose
- Insert two new technician users into the `users` table.
- One technician in the "Civil Manager" department and one in the "Electrical Manager" department.
- These users have role 'technician' and are active.

2. Data Inserted
- Civil Technician: email `civil.technician@tickettracker.com`, department `Civil Manager`, role `technician`
- Electrical Technician: email `electrical.technician@tickettracker.com`, department `Electrical Manager`, role `technician`

3. Security
- No schema changes. RLS already exists on the `users` table.
- No new policies needed.
*/

INSERT INTO users (id, name, email, role, department, active)
VALUES (
  'a1b2c3d4-e5f6-7890-abcd-ef1234567001',
  'Civil Technician',
  'civil.technician@tickettracker.com',
  'technician',
  'Civil Manager',
  true
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, email, role, department, active)
VALUES (
  'a1b2c3d4-e5f6-7890-abcd-ef1234567002',
  'Electrical Technician',
  'electrical.technician@tickettracker.com',
  'technician',
  'Electrical Manager',
  true
)
ON CONFLICT (email) DO NOTHING;
