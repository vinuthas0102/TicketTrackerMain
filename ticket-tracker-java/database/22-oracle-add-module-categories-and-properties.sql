-- ============================================================================
-- Module-linked categories + property master table
-- Mirrors the Supabase migration 20260728000001.
--
-- 1. master_categories: add module_id column, replace name unique constraint
--    with (module_id, name), backfill per-module categories from modules.config.
-- 2. master_properties: new table for property IDs managed in Master Setup.
-- ============================================================================

-- 1. master_categories: add module_id
ALTER TABLE master_categories ADD module_id RAW(16);

ALTER TABLE master_categories ADD CONSTRAINT fk_master_categories_module
  FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE SET NULL;

CREATE INDEX idx_master_categories_module_id ON master_categories(module_id);

-- Replace name-only unique with (module_id, name). The original inline UNIQUE
-- constraint has a system-generated name, so find and drop it dynamically.
DECLARE
  v_constraint_name user_constraints.constraint_name%TYPE;
BEGIN
  SELECT c.constraint_name INTO v_constraint_name
  FROM user_constraints c
  JOIN user_cons_columns cc ON cc.constraint_name = c.constraint_name
  WHERE c.table_name = 'MASTER_CATEGORIES'
    AND c.constraint_type = 'U'
    AND cc.column_name = 'NAME'
    AND ROWNUM = 1;
  EXECUTE IMMEDIATE 'ALTER TABLE master_categories DROP CONSTRAINT ' || v_constraint_name;
EXCEPTION
  WHEN NO_DATA_FOUND THEN NULL;
END;
/
ALTER TABLE master_categories ADD CONSTRAINT uq_master_categories_module_name
  UNIQUE (module_id, name);

-- Backfill per-module categories from modules.config.categories JSON array.
-- Oracle 12c+: use JSON_TABLE to expand the categories array.
INSERT INTO master_categories (module_id, name, is_active, display_order, created_at, updated_at)
SELECT
  m.id,
  jt.category_name,
  1,
  jt.ord,
  SYSTIMESTAMP,
  SYSTIMESTAMP
FROM modules m
CROSS JOIN JSON_TABLE(
  m.config,
  '$.categories[*]' COLUMNS (
    category_name VARCHAR2(255) PATH '$',
    ord FOR ORDINALITY
  )
) jt
WHERE m.config IS NOT NULL
  AND JSON_EXISTS(m.config, '$.categories')
  AND NOT EXISTS (
    SELECT 1 FROM master_categories mc
    WHERE mc.module_id = m.id AND mc.name = jt.category_name
  );

-- 2. master_properties table
CREATE TABLE master_properties (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(255) NOT NULL UNIQUE,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    display_order NUMBER(10) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
CREATE INDEX idx_master_properties_active ON master_properties(is_active);
CREATE INDEX idx_master_properties_order ON master_properties(display_order);

CREATE OR REPLACE TRIGGER trg_master_properties_updated_at
BEFORE UPDATE ON master_properties FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Seed master_properties
INSERT INTO master_properties (name, is_active, display_order) SELECT 'PROP001', 1, 1 FROM dual
  WHERE NOT EXISTS (SELECT 1 FROM master_properties WHERE name = 'PROP001');
INSERT INTO master_properties (name, is_active, display_order) SELECT 'PROP002', 1, 2 FROM dual
  WHERE NOT EXISTS (SELECT 1 FROM master_properties WHERE name = 'PROP002');
