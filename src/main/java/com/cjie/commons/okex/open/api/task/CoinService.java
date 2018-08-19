package com.cjie.commons.okex.open.api.task;

import com.cjie.commons.okex.open.api.bean.spot.result.Account;
import com.cjie.commons.okex.open.api.bean.spot.result.Ticker;
import com.cjie.cryptocurrency.quant.mapper.TickerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CoinService {

    @Autowired
    private MineService mineService;

    @Autowired
    private TickerMapper tickerMapper;


    private static int numPrecision = 8;

    private static final int pricePrecision = 8;

    private static final int query_num = 40;

    private static Map<String, Double> minLimitPriceOrderNums = new HashMap<>();

    static {
        CoinService.minLimitPriceOrderNums.put("eos", 0.1);
        CoinService.minLimitPriceOrderNums.put("ltc", 0.001);
        CoinService.minLimitPriceOrderNums.put("bch", 0.001);
        CoinService.minLimitPriceOrderNums.put("okb", 0.5);
        CoinService.minLimitPriceOrderNums.put("cac", 1.0);
    }

    /**
     * 币币自动交易
     * @param site 站点
     * @param baseName
     * @param quotaName
     * @param increment
     * @param baseRatio
     * @throws Exception
     */
    public void coin(String site, String baseName, String quotaName, double increment, double baseRatio) throws Exception {

        String symbol = baseName.toUpperCase() + "-" + quotaName.toUpperCase();
        mineService.cancelOrders(site, mineService.getNotTradeOrders(site, symbol, "0", "100"));
        //查询余额
        Account baseAccount = mineService.getBalance(site, baseName);
        double baseAvailable = new BigDecimal(baseAccount.getAvailable()).doubleValue();

        Account quotaAccount = mineService.getBalance(site, quotaName);
        double quotaAvailable = new BigDecimal(quotaAccount.getAvailable()).doubleValue();

        Ticker ticker = mineService.getTicker(site, baseName, quotaName);
        Double marketPrice = Double.parseDouble(ticker.getLast());
        CoinService.log.info("ticker last {} -{}:{}", baseName, quotaName, marketPrice);

        double allAsset = baseAvailable * marketPrice + quotaAvailable;
        CoinService.log.info("basebalance:{}, qutobalance:{}, allAsset:{}, asset/2:{}, basebalance-quota:{}",
                baseAvailable, quotaAvailable, allAsset, allAsset * baseRatio, baseAvailable * marketPrice);

        if (allAsset * baseRatio - baseAvailable * marketPrice > allAsset * increment) {
            BigDecimal amount = new BigDecimal(allAsset * baseRatio - baseAvailable * marketPrice).setScale(CoinService.numPrecision, BigDecimal.ROUND_FLOOR);
            CoinService.log.info("baseAvailable:{}, quotaAvailable:{}", baseAvailable + amount.doubleValue(),
                    quotaAvailable - amount.doubleValue() * CoinService.getMarketPrice(marketPrice).doubleValue());
            CoinService.log.info("buy {}, price:{}", amount, marketPrice);
            //买入
            if (amount.doubleValue() - CoinService.minLimitPriceOrderNums.get(baseName.toLowerCase()) * marketPrice < 0) {
                CoinService.log.info("小于最小限价数量");
            } else {
                //计算下单价格
                BigDecimal marketPriceValue = getMarketPrice(marketPrice * (1 - increment));
                BigDecimal marketMinPrice = new BigDecimal(tickerMapper.selectMinByProductId(symbol,query_num)).setScale(CoinService.numPrecision, BigDecimal.ROUND_FLOOR);
                BigDecimal marketValue = marketPriceValue.min(marketMinPrice);
                BigDecimal baseamount = amount.divide(marketValue,CoinService.numPrecision, BigDecimal.ROUND_DOWN);
                log.info("order buy amount = {} and price = {}",baseamount,marketValue);
                mineService.buy(site, symbol, "limit", baseamount, marketValue);
            }
        }

        if (baseAvailable * marketPrice - allAsset * baseRatio > allAsset * increment) {
            //卖出
            BigDecimal amount = new BigDecimal(baseAvailable * marketPrice - allAsset * baseRatio).setScale(CoinService.numPrecision, BigDecimal.ROUND_FLOOR);
            CoinService.log.info("baseAvailable:{}, quotaAvailable:{}", baseAvailable - amount.doubleValue(),
                    quotaAvailable + amount.doubleValue() * CoinService.getMarketPrice(marketPrice).doubleValue());
            CoinService.log.info("sell {}, price:{}", amount, marketPrice);
            if (amount.doubleValue() - CoinService.minLimitPriceOrderNums.get(baseName.toLowerCase()) * marketPrice < 0) {
                CoinService.log.info("小于最小限价数量");
            } else {
                //计算下单价格
                BigDecimal marketPriceValue = getMarketPrice(marketPrice * (1 + increment));
                BigDecimal marketMaxPrice = new BigDecimal(tickerMapper.selectMaxByProductId(symbol,query_num)).setScale(CoinService.numPrecision, BigDecimal.ROUND_FLOOR);
                BigDecimal marketValue = marketPriceValue.max(marketMaxPrice);
                BigDecimal baseamount = amount.divide(marketValue,CoinService.numPrecision, BigDecimal.ROUND_DOWN);
                log.info("order sell amount = {} and price = {}",baseamount,marketValue);
                mineService.sell(site, symbol, "limit", baseamount, marketValue);
            }
        }
    }

    public static BigDecimal getMarketPrice(double marketPrice) {
        return MineService.getBigDecimal(marketPrice, pricePrecision);
    }

}
