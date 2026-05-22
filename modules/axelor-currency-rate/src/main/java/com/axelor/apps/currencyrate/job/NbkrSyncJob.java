package com.axelor.apps.currencyrate.job;

import com.axelor.apps.currencyrate.service.NbkrCurrencyRateService;
import com.axelor.inject.Beans;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NbkrSyncJob implements Job {

  private static final Logger LOG = LoggerFactory.getLogger(NbkrSyncJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    LOG.info("NbkrSyncJob fired at {}", context.getFireTime());
    try {
      int count = Beans.get(NbkrCurrencyRateService.class).syncDailyRates();
      LOG.info("NbkrSyncJob finished, {} rates updated", count);
    } catch (Exception e) {
      LOG.error("NbkrSyncJob failed", e);
      throw new JobExecutionException(e);
    }
  }
}
