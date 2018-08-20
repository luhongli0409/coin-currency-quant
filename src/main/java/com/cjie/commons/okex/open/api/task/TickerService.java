package com.cjie.commons.okex.open.api.task;

import com.cjie.commons.okex.open.api.bean.spot.result.Ticker;
import com.cjie.cryptocurrency.quant.mapper.TickerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class TickerService {

    @Autowired
    private ApiService apiService;

    @Autowired
    private TickerMapper tickerMapper;

    public void getTicker(String site, String baseCurrency, String quotaCurrency) {
        Ticker ticker = apiService.getTicker(site,baseCurrency,quotaCurrency);
        Ticker oldTicker = tickerMapper.selectByProductId(ticker.getProduct_id());
        if (oldTicker != null
                && !(new BigDecimal(oldTicker.getLast()).setScale(5, BigDecimal.ROUND_UP)
                .compareTo(new BigDecimal(ticker.getLast()).setScale(5, BigDecimal.ROUND_UP)) == 0)) {
            tickerMapper.insert(ticker);
        } else if (oldTicker == null) {
            tickerMapper.insert(ticker);
        }
    }
}
