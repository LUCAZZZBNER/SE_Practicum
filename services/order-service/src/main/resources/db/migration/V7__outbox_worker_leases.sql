-- Lease rows while an instance performs outbox work. Expired leases are reclaimable,
-- so a crashed instance does not permanently stall delivery.
ALTER TABLE order_event_outbox
  ADD COLUMN claimed_by VARCHAR(64),
  ADD COLUMN claim_until DATETIME(3),
  ADD KEY idx_order_event_claim(claimed_by, claim_until);
ALTER TABLE order_stock_release_outbox
  ADD COLUMN claimed_by VARCHAR(64),
  ADD COLUMN claim_until DATETIME(3),
  ADD KEY idx_release_claim(claimed_by, claim_until);
