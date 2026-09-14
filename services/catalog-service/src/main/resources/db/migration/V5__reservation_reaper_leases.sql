-- Prevent multiple catalog replicas from reconciling the same reservation.
ALTER TABLE stock_reservations
  ADD COLUMN reaper_claimed_by VARCHAR(64),
  ADD COLUMN reaper_claim_until DATETIME(3),
  ADD KEY idx_stock_reservation_reaper(reaper_claimed_by, reaper_claim_until);
