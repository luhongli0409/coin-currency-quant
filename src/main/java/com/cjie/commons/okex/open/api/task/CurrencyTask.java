package com.cjie.commons.okex.open.api.task;


import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CurrencyTask {

    @Autowired
    private CurrencyService currencyService;


    @Scheduled(cron = "*/15 * * * * ?")
    public void currencyBalance() throws JobExecutionException {
        CurrencyTask.log.info("CurrencyTask start mining");
        try {
            currencyService.currency("coinall", "cac", "usdt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        CurrencyTask.log.info("CurrencyTask end mining");
    }
}
