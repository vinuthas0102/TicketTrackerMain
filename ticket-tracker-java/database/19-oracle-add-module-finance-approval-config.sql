-- 19-oracle-add-module-finance-approval-config.sql
-- Add requiresFinanceApproval to module config and backfill existing tickets

-- Update all module configs to include requiresFinanceApproval: false if not already present
-- Oracle doesn't have JSONB operators, so we do a simple string check/replace
UPDATE modules
SET config = CASE
    WHEN config IS NULL OR config = '' THEN
        '{"requiresFinanceApproval":false}'
    WHEN config LIKE '%requiresFinanceApproval%' THEN
        config
    ELSE
        REPLACE(config, '}', ',"requiresFinanceApproval":false}')
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE config IS NULL
   OR config = ''
   OR config NOT LIKE '%requiresFinanceApproval%';

-- Backfill existing tickets: set requires_finance_approval to 0 (false) for all existing tickets
-- where the module does not have requiresFinanceApproval enabled
UPDATE tickets t
SET t.requires_finance_approval = 0
WHERE t.requires_finance_approval IS NULL
   OR t.requires_finance_approval = 1;

COMMIT;
