/*
# Create user_regions table for region-based user profiles

## Purpose
This migration introduces a `user_regions` link table that allows each user to be
assigned to one or more regions (locations). This is the foundation for
location-scoped ticket visibility and location-filtered "Assigned To" lists.

## New Tables
- `user_regions`
  - `id` (uuid, primary key)
  - `user_id` (uuid, foreign key to `users(id)`, ON DELETE CASCADE)
  - `region` (text, not null — matches the `name` column in `master_locations`)
  - `created_at` (timestamptz, default now())
  - Unique constraint on `(user_id, region)` to prevent duplicate assignments

## Security
- RLS enabled on `user_regions`.
- Permissive CRUD policies for `anon, authenticated` roles, matching the pattern
  used by `master_locations` and other application tables where authorization
  is enforced at the application layer.

## Seed Data
- All existing users are backfilled with a default region of 'Location01'
  so they retain access after the change goes live.

## Important Notes
1. Region assignment is mandatory for all roles (enforced at the application layer).
2. EO users are strictly limited to their assigned regions (no override).
3. Tickets whose `property_location` does not match any of a user's regions
   are inaccessible until the ticket's location is corrected.
*/

-- Create the user_regions table
CREATE TABLE IF NOT EXISTS user_regions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  region text NOT NULL,
  created_at timestamptz DEFAULT now(),
  CONSTRAINT uq_user_region UNIQUE (user_id, region)
);

-- Enable RLS
ALTER TABLE user_regions ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any (idempotent)
DROP POLICY IF EXISTS "anon_select_user_regions" ON user_regions;
DROP POLICY IF EXISTS "anon_insert_user_regions" ON user_regions;
DROP POLICY IF EXISTS "anon_update_user_regions" ON user_regions;
DROP POLICY IF EXISTS "anon_delete_user_regions" ON user_regions;

-- Permissive CRUD policies (authorization enforced at application layer)
CREATE POLICY "anon_select_user_regions"
  ON user_regions FOR SELECT
  TO anon, authenticated USING (true);

CREATE POLICY "anon_insert_user_regions"
  ON user_regions FOR INSERT
  TO anon, authenticated WITH CHECK (true);

CREATE POLICY "anon_update_user_regions"
  ON user_regions FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);

CREATE POLICY "anon_delete_user_regions"
  ON user_regions FOR DELETE
  TO anon, authenticated USING (true);

-- Backfill existing users with default region 'Location01'
INSERT INTO user_regions (user_id, region)
SELECT id, 'Location01' FROM users
WHERE id NOT IN (SELECT user_id FROM user_regions WHERE region = 'Location01')
ON CONFLICT (user_id, region) DO NOTHING;

-- Index for efficient lookups
CREATE INDEX IF NOT EXISTS idx_user_regions_user_id ON user_regions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_regions_region ON user_regions(region);
