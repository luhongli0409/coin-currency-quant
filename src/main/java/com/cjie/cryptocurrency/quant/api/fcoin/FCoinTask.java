package com.cjie.cryptocurrency.quant.api.fcoin;


import com.cjie.cryptocurrency.quant.api.fcoin.FcoinUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FCoinTask {

    @Scheduled(fixedDelay = 60 * 1000)
    public void mineCurrency() throws JobExecutionException {
        FcoinUtils fcoinUtils = new FcoinUtils();
        try {
            //fcoinUtils.ftusdt1("ftusdt", "ft", "usdt",0);
            //fcoinUtils.ftusdt2("ftusdt", "ft", "usdt",0.05);
            //fcoinUtils.ftusdt1("btcusdt","btc","usdt",0);
            //fcoinUtils.ftusdt2("ftusdt","ft","usdt",0.01);
            fcoinUtils.ftusdt2("ftusdt", "ft", "usdt", 0.01);

            //fcoinUtils.ftusdt1("icxeth","icx","eth",0);
        }catch (Exception e){
            log.info("==========FcoinJob发生异常============",e);
            throw new JobExecutionException("ftustd 方法体执行异常");
        }
    }
}