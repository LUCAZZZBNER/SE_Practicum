package com.delivery.catalog.item.service;

import static org.mockito.Mockito.*;

import com.delivery.catalog.item.dao.StockReservationDao;
import org.junit.jupiter.api.Test;

class StockReservationReaperTest {
  @Test void skipsRecoveryWhenInternalCredentialsAreMissing() {
    var dao = mock(StockReservationDao.class);
    var items = mock(ItemService.class);
    new StockReservationReaper(dao, items, "http://order", "").reconcile();
    verifyNoInteractions(dao, items);
  }
}
