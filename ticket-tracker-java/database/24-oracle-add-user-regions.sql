-- ==================================================================================
-- TABLE: user_regions
-- Link table for region-based user profiles. Each user can be assigned to
-- one or more regions (locations). This is the foundation for location-scoped
-- ticket visibility and location-filtered "Assigned To" lists.
-- ==================================================================================

CREATE TABLE user_regions (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  user_id RAW(16) NOT NULL,
  region VARCHAR2(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_user_region UNIQUE (user_id, region),
  CONSTRAINT fk_user_regions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE user_regions IS 'Link table for region-based user profiles (multiple regions per user)';
COMMENT ON COLUMN user_regions.region IS 'Region/location name matching master_locations.name';

CREATE INDEX idx_user_regions_user_id ON user_regions(user_id);
CREATE INDEX idx_user_regions_region ON user_regions(region);

-- Backfill existing users with default region 'Location01'
INSERT INTO user_regions (user_id, region)
SELECT id, 'Location01' FROM users
WHERE id NOT IN (SELECT user_id FROM user_regions WHERE region = 'Location01');
