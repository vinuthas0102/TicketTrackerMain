-- 21-oracle-add-review-by-eo-required.sql
-- Add "reviewByEORequired" flag to module configs (stored in the config JSON CLOB)
--
-- This flag controls whether the "Reviewed" status step is required in the ticket workflow.
-- When set to 'false' (the default), the Reviewed status is hidden from the summary,
-- add-task icons are gated on ACTIVE status instead of REVIEWED, and remarks become
-- optional when transitioning to "Start to work" (ACTIVE).
--
-- The config column is a JSON CLOB. We use JSON_MERGEPATCH to add the new key.

UPDATE modules
SET config = JSON_MERGEPATCH(
    COALESCE(config, '{}'),
    '{"reviewByEORequired": false}'
)
WHERE NOT JSON_EXISTS(config, '$.reviewByEORequired');

COMMIT;
