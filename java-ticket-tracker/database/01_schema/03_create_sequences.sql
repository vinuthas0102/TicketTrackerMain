/*
  Oracle Database Sequences for Ticket Tracker System

  Sequences for generating unique IDs and numbers
*/

-- Sequence for ticket numbers
CREATE SEQUENCE seq_ticket_number
    START WITH 1000
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Sequence for step numbers (within a ticket)
CREATE SEQUENCE seq_step_number
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Function to generate UUID-like strings (Oracle 11g compatible)
CREATE OR REPLACE FUNCTION generate_uuid RETURN VARCHAR2 IS
    v_uuid VARCHAR2(36);
BEGIN
    SELECT LOWER(
        REGEXP_REPLACE(
            RAWTOHEX(SYS_GUID()),
            '([A-F0-9]{8})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{12})',
            '\1-\2-\3-\4-\5'
        )
    ) INTO v_uuid FROM DUAL;
    RETURN v_uuid;
END;
/

COMMIT;

-- Verify sequence creation
SELECT sequence_name FROM user_sequences;
