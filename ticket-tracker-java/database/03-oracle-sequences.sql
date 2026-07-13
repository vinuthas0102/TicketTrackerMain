/*
  Oracle Sequences for Auto-Incrementing Fields

  This script creates sequences for tables that need numeric auto-increment IDs.
  While most tables use SYS_GUID() for RAW(16) IDs, some tables may need
  sequences for specific business logic (e.g., ticket numbers).

  Run this script after 02-oracle-schema.sql
*/

-- ==================================================================================
-- Sequence for Ticket Numbers (Business-Friendly Sequential IDs)
-- ==================================================================================
CREATE SEQUENCE seq_ticket_number
  START WITH 1000
  INCREMENT BY 1
  NOCACHE
  NOCYCLE;

COMMENT ON SEQUENCE seq_ticket_number IS 'Generates sequential ticket numbers for display';

-- ==================================================================================
-- Utility Function: Generate Formatted Ticket Number
-- ==================================================================================
CREATE OR REPLACE FUNCTION generate_ticket_number(
  p_module_prefix VARCHAR2 DEFAULT 'TKT'
) RETURN VARCHAR2 IS
  v_next_number NUMBER;
  v_ticket_number VARCHAR2(50);
BEGIN
  SELECT seq_ticket_number.NEXTVAL INTO v_next_number FROM DUAL;
  v_ticket_number := p_module_prefix || '-' || LPAD(v_next_number, 6, '0');
  RETURN v_ticket_number;
END;
/

COMMENT ON FUNCTION generate_ticket_number IS 'Generates formatted ticket numbers like TKT-001000';

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Sequences created successfully!' FROM DUAL;
SELECT 'Function generate_ticket_number() available for use' FROM DUAL;
SELECT 'Next step: Run 04-oracle-triggers.sql' FROM DUAL;
