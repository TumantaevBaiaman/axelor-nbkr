package com.axelor.apps.currencyrate.web;

import com.axelor.apps.currencyrate.service.NbkrCurrencyRateService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CurrencyRateController {

  private static final Logger LOG = LoggerFactory.getLogger(CurrencyRateController.class);

  public void refreshFromNbkr(ActionRequest request, ActionResponse response) {
    try {
      int count = Beans.get(NbkrCurrencyRateService.class).syncDailyRates();
      response.setNotify(String.format("NBKR sync OK: %d rates updated.", count));
      response.setReload(true);
    } catch (Exception e) {
      LOG.error("Manual NBKR sync failed", e);
      response.setError("NBKR sync failed: " + e.getMessage());
    }
  }
}
