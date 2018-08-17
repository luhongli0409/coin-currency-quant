package com.cjie.commons.okex.open.api.task;


import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TickerTask {

    @Autowired
    private TickerService tickerService;

    @Scheduled(cron = "*/6 * * * * ?")
    public void mineCurrency1() throws JobExecutionException {
        TickerTask.log.info("start mining");
        try {
            tickerService.getTicker("coinall", "cac", "usdt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        TickerTask.log.info("end mining");


    }
}
