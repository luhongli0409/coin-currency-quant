package com.cjie.commons.okex.open.api.task;


import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

//@Component
@Slf4j
public class MineTask {

    @Autowired
    private MineService mineService;

    @Scheduled(cron = "*/3 * * * * ?")
    public void mineCurrency1() throws JobExecutionException {
        MineTask.log.info("start mining");
        try {
            mineService.mine2("coinall", "cac", "usdt", 0.002);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MineTask.log.info("end mining");


    }

}
