-- Lease stale checkout claims before the reaper performs an external decision lookup.
ALTER TABLE cart_items
  ADD COLUMN reaper_claimed_by VARCHAR(64),
  ADD COLUMN reaper_claim_until DATETIME(3),
  ADD KEY idx_cart_reaper_claim(reaper_claimed_by, reaper_claim_until);
