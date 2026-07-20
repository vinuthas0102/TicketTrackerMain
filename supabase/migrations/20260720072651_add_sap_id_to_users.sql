/*
# Add SAP ID column to users table

1. New Columns
- `users.sap_id` (text, nullable) — stores the SAP ID associated with a user profile.
  Populated via the admin user create/edit screens. Displayed read-only on the
  ticket summary page using the ticket creator's SAP ID.

2. Modified Tables
- `users`: adds the `sap_id` column. No existing columns changed or removed.

3. Security
- No RLS policy changes. Existing user policies continue to govern access to the
  `users` table, so `sap_id` is exposed only to roles already permitted to read
  user records.

4. Important Notes
- The column is nullable so existing rows are unaffected; their `sap_id` stays NULL
  until an admin sets it through the user management UI.
- Idempotent: safe to re-run.
*/

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'sap_id'
  ) THEN
    ALTER TABLE users ADD COLUMN sap_id text;
  END IF;
END $$;
