/*
  # Drop unused and duplicate indexes

  ## Summary
  Removes indexes that are either never used (confirmed by pg_stat_user_indexes)
  or are exact duplicates of other indexes. Dropping them reduces write overhead
  and storage usage without impacting query performance.

  ## Dropped Indexes
  1. idx_documents_uploaded_by - unused on documents table
  2. idx_documents_is_completion_certificate - unused partial index on documents
  3. idx_tickets_requires_finance_approval - unused on tickets table
  4. idx_tickets_status_finance - unused composite on tickets table
  5. idx_finance_approvals_ticket_id - duplicate of idx_finance_approvals_ticket
  6. idx_finance_approvals_status - unused on finance_approvals
  7. idx_finance_approvals_finance_officer_id - unused (new idx_finance_approvals_finance_officer_id added for FK, keeping that one)

  ## Notes
  - idx_finance_approvals_ticket is kept as it was the original; idx_finance_approvals_ticket_id is the duplicate
  - The new FK index idx_finance_approvals_finance_officer_id added in the previous migration replaces
    the old unused idx_finance_approvals_finance_officer_id, so both would be identical — only one is needed
*/

DROP INDEX IF EXISTS public.idx_documents_uploaded_by;
DROP INDEX IF EXISTS public.idx_documents_is_completion_certificate;
DROP INDEX IF EXISTS public.idx_tickets_requires_finance_approval;
DROP INDEX IF EXISTS public.idx_tickets_status_finance;
DROP INDEX IF EXISTS public.idx_finance_approvals_ticket_id;
DROP INDEX IF EXISTS public.idx_finance_approvals_status;
