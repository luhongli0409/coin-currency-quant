package com.cjie.commons.okex.open.api.task;


import com.cjie.commons.okex.open.api.enums.TaskEnum;
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

    @Autowired
    private ApiService apiService;


    @Scheduled(cron = "0 */10 * * * ?")
    public void currencyBalance() throws JobExecutionException {
        CurrencyTask.log.info("CurrencyTask start mining");
        try {
            boolean flag = apiService.canExecute(TaskEnum.BALANCE_SEND_WX_TASK.getTaskId());
            CurrencyTask.log.info("CurrencyTask can execute flag :{}",flag);
            if(flag){
                currencyService.currency("coinall", "cac", "usdt");
                //currencyService.currency("okex", "okb", "usdt");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CurrencyTask.log.info("CurrencyTask end mining");
    }
}
