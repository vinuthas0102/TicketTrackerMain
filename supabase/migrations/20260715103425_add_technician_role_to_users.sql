/*
# Add 'technician' role to users table

1. Changes
- Alter the `users` table role CHECK constraint to include 'technician'.
- The previous constraint allowed: 'employee', 'eo', 'dept_officer', 'vendor', 'finance'.
- New constraint allows: 'employee', 'eo', 'dept_officer', 'vendor', 'finance', 'technician'.
2. Security
- No RLS changes. Existing policies remain unchanged.
3. Notes
- This is a non-destructive change: it only widens the allowed values for the role column.
- Existing rows are unaffected.
*/

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check
  CHECK (role IN ('employee', 'eo', 'dept_officer', 'vendor', 'finance', 'technician'));