package com.cjie.commons.okex.open.api.task;


import com.cjie.commons.okex.open.api.enums.TaskEnum;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CoinTask {

    @Autowired
    private CoinService coinService;

    @Autowired
    private ApiService apiService;

    @Scheduled(cron = "*/3 * * * * ?")
    public void mineCurrency1() throws JobExecutionException {
        CoinTask.log.info("CoinTask start mining");
        try {
            boolean flag = apiService.canExecute(TaskEnum.COIN_TRADE_TASK.getTaskId());
            CoinTask.log.info("CoinTask can execute flag :{}",flag);
            if(flag){
                coinService.coin("coinall", "cac", "usdt", 0.005, 0.5);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CoinTask.log.info("CoinTask end mining");
    }

}
