package com.cjie.commons.okex.open.api.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cjie.commons.okex.open.api.bean.account.result.Wallet;
import com.cjie.commons.okex.open.api.bean.spot.param.PlaceOrderParam;
import com.cjie.commons.okex.open.api.bean.spot.result.*;
import com.cjie.commons.okex.open.api.service.account.AccountAPIService;
import com.cjie.commons.okex.open.api.service.spot.SpotAccountAPIService;
import com.cjie.commons.okex.open.api.service.spot.SpotOrderAPIServive;
import com.cjie.commons.okex.open.api.utils.WXInfoUtils;
import com.cjie.cryptocurrency.quant.mapper.CurrencyOrderMapper;
import com.cjie.cryptocurrency.quant.mapper.TaskConfigMapper;
import com.cjie.cryptocurrency.quant.model.APIKey;
import com.cjie.cryptocurrency.quant.model.CurrencyOrder;
import com.cjie.cryptocurrency.quant.service.ApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Slf4j
public class ApiService {

    @Autowired
    private SpotAccountAPIService spotAccountAPIService;

    @Autowired
    private SpotOrderAPIServive spotOrderAPIService;

    @Autowired
    private ApiKeyService  apiKeyService;

    @Autowired
    private CurrencyOrderMapper currencyOrderMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TaskConfigMapper taskConfigMapper;

    @Autowired
    private AccountAPIService accountAPIService;

    private static String AVERAGE = "-AVERAGE";


    private static double initMultiple = 3;

    private static double maxNum = 30;

    private static int numPrecision = 8;


    private static Map<String, Double> minLimitPriceOrderNums = new HashMap<>();

    private static final int pricePrecision = 8;

    static {
        ApiService.minLimitPriceOrderNums.put("eos", 0.1);
        ApiService.minLimitPriceOrderNums.put("ltc", 0.001);
        ApiService.minLimitPriceOrderNums.put("bch", 0.001);
        ApiService.minLimitPriceOrderNums.put("okb", 0.5);
        ApiService.minLimitPriceOrderNums.put("cac", 1.0);
    }



    /**
     * 自买自卖交易
     *
     * @param baseName    交易币名称
     * @param quotaName  市场币名称
     * @param increment 收益率一半
     * @throws Exception
     */
    public void mine1(String site, String baseName, String quotaName, double increment) throws Exception {
        String symbol = baseName.toUpperCase() + "-" + quotaName.toUpperCase();

        cancelOrders(site, getNotTradeOrders(site, symbol, "0", "100"));

        //查询余额
        Account baseAccount = getBalance(site, baseName);
        double baseHold = new BigDecimal(baseAccount.getBalance()).doubleValue() - new BigDecimal(baseAccount.getAvailable()).doubleValue();
        double baseBalance = new BigDecimal(baseAccount.getBalance()).doubleValue();


        Account quotaAccount = getBalance(site, quotaName);
        double quotaHold = new BigDecimal(quotaAccount.getBalance()).doubleValue() - new BigDecimal(quotaAccount.getAvailable()).doubleValue();
        double quotaBalance = new BigDecimal(quotaAccount.getBalance()).doubleValue();


        ApiService.log.info("===============balance: base:{},quota:{}========================", baseBalance, quotaBalance);

        Ticker ticker = getTicker(site, baseName, quotaName);
        Double marketPrice = Double.parseDouble(ticker.getLast());
        ApiService.log.info("ticker last {} -{}:{}", baseName, quotaName, marketPrice);
        double initUsdt = ApiService.maxNum * ApiService.initMultiple * marketPrice;
//
        //初始化
        if (!(baseHold > 0 || quotaHold > 0)) {
            if (isHaveInitBuyAndSell(site, baseBalance - baseHold, quotaBalance - quotaHold, marketPrice, initUsdt, symbol, "limit", increment)) {
                ApiService.log.info("================有进行初始化均衡操作=================");
                return;
            }
        }

        ApiService.log.info("=============================交易对开始=========================");
        //买单
        double priceBuy = quotaBalance - quotaHold;
        ApiService.log.info("apply trade buy price is :{}", priceBuy);
        BigDecimal baseAmountpriceBuy = getNum(Math.min(priceBuy * 0.99 / marketPrice, ApiService.maxNum));//预留点来扣手续费
        ApiService.log.info("apply trade buy amount is :{}", baseAmountpriceBuy);
        if (baseAmountpriceBuy.doubleValue() - ApiService.minLimitPriceOrderNums.get(baseName.toLowerCase()) < 0) {
            ApiService.log.info("小于最小限价数量buy :   {}", baseAmountpriceBuy.doubleValue() - ApiService.minLimitPriceOrderNums.get(baseName.toLowerCase()));
        } else {
            //判断是否有冻结的，如果冻结太多冻结就休眠，进行下次挖矿
            if (quotaHold > 0.3 * quotaBalance) {
                ApiService.log.info("quotaHold > 0.3 * quotaBalance");
            } else {
                try {
                    buyNotLimit(site, symbol, "limit", baseAmountpriceBuy, ApiService.getMarketPrice(marketPrice * (1 - increment)));
                } catch (Exception e) {
                    ApiService.log.error("交易对买出错", e);
                }
            }
        }


        //卖单
        double priceSell = (baseBalance - baseHold) * marketPrice;
        ApiService.log.info("apply trade sell price is :{}", priceSell);
        BigDecimal baseAmountSell = getNum(Math.min(priceSell * 0.99 / marketPrice, ApiService.maxNum));//预留点来扣手续费
        ApiService.log.info("apply trade sell amount is :{}", baseAmountSell);
        if (baseAmountSell.doubleValue() - ApiService.minLimitPriceOrderNums.get(baseName.toLowerCase()) < 0) {
            ApiService.log.info("小于最小限价数量sell :   {}", baseAmountSell.doubleValue() - ApiService.minLimitPriceOrderNums.get(baseName.toLowerCase()));
        } else {
            //判断是否有冻结的，如果冻结太多冻结就休眠，进行下次挖矿
            if (baseHold > 0.3 * baseBalance) {
                ApiService.log.info("baseHold > 0.3 * baseBalance");
            } else {
                try {
                    sellNotLimit(site, symbol, "limit", baseAmountSell, ApiService.getMarketPrice(marketPrice * (1 + increment)));
                } catch (Exception e) {
                    ApiService.log.error("交易对卖出错", e);
                }
            }
        }
        ApiService.log.info("=============================交易对结束=========================");

    }

    /**
     * 自买自卖交易
     *
     * @param baseName  交易币名称
     * @param quotaName 市场币名称
     * @param increment 收益率一半
     * @throws Exception
     */
    public void mine2(String site, String baseName, String quotaName, double increment) throws Exception {
        String symbol = baseName.toUpperCase() + "-" + quotaName.toUpperCase();

        cancelOrders(site, getNotTradeOrders(site, symbol, "0", "100"));

        //查询余额
        Account baseAccount = getBalance(site, baseName);
        double baseHold = new BigDecimal(baseAccount.getBalance()).doubleValue() - new BigDecimal(baseAccount.getAvailable()).doubleValue();
        double baseBalance = new BigDecimal(baseAccount.getBalance()).doubleValue();


        Account quotaAccount = getBalance(site, quotaName);
        double quotaHold = new BigDecimal(quotaAccount.getBalance()).doubleValue() - new BigDecimal(quotaAccount.getAvailable()).doubleValue();
        double quotaBalance = new BigDecimal(quotaAccount.getBalance()).doubleValue();


        //判断是否有冻结的，如果冻结太多冻结就休眠，进行下次挖矿
        if (baseHold > 0.3 * baseBalance
                && quotaHold > 0.3 * quotaBalance) {
            return;
        }

        ApiService.log.info("===============balance: base:{},quota:{}========================", baseBalance, quotaBalance);

        Ticker ticker = getTicker(site, baseName, quotaName);
        Double marketPrice = Double.parseDouble(ticker.getLast());
        ApiService.log.info("ticker last {} -{}:{}", baseName, quotaName, marketPrice);
        double initUsdt = ApiService.maxNum * ApiService.initMultiple * marketPrice;
//
        //初始化
        if (!(baseHold > 0 || quotaHold > 0)) {
            if (isHaveInitBuyAndSell(site, baseBalance - baseHold, quotaBalance - quotaHold, marketPrice, initUsdt, symbol, "limit", increment)) {
                ApiService.log.info("================有进行初始化均衡操作=================");
                return;
            }
        }
//
        //买单 卖单
        double price = Math.min((baseBalance - baseHold) * marketPrice, quotaBalance - quotaHold);

        BigDecimal baseAmount = getNum(Math.min(price * 0.99 / marketPrice, ApiService.maxNum));//预留点来扣手续费
        if (baseAmount.doubleValue() - ApiService.minLimitPriceOrderNums.get(baseName.toLowerCase()) < 0) {
            ApiService.log.info("小于最小限价数量");
            return;
        }

        ApiService.log.info("=============================交易对开始=========================");
        try {
            buyNotLimit(site, symbol, "limit", baseAmount, ApiService.getMarketPrice(marketPrice * (1 - increment)));
        } catch (Exception e) {
            ApiService.log.error("交易对买出错", e);
        }
        try {
            sellNotLimit(site, symbol, "limit", baseAmount, ApiService.getMarketPrice(marketPrice * (1 + increment)));
        } catch (Exception e) {
            ApiService.log.error("交易对卖出错", e);
        }
        ApiService.log.info("=============================交易对结束=========================");

    }

    /**
     * 动态调整策略
     *
     * @param baseName    交易币名称
     * @param quotaName  市场币名称
     * @param increment 收益率一半
     * @throws Exception
     * @throws Exception
     */
    public void  mine3(String site, String baseName, String quotaName, double increment, double baseRatio) throws Exception {


        String symbol = baseName.toUpperCase() + "-" + quotaName.toUpperCase();
        cancelOrders(site, getNotTradeOrders(site, symbol, "0", "100"));

        //查询余额
        Account baseAccount = getBalance(site, baseName);
        double baseHold = new BigDecimal(baseAccount.getBalance()).doubleValue() - new BigDecimal(baseAccount.getAvailable()).doubleValue();
        //double baseBalance = new BigDecimal(baseAccount.getBalance()).doubleValue();
        double baseBalance = new BigDecimal(baseAccount.getAvailable()).doubleValue();


        Account quotaAccount = getBalance(site, quotaName);
        double quotaHold = new BigDecimal(quotaAccount.getBalance()).doubleValue() - new BigDecimal(quotaAccount.getAvailable()).doubleValue();
        //double quotaBalance = new BigDecimal(quotaAccount.getBalance()).doubleValue();
        double quotaBalance = new BigDecimal(quotaAccount.getAvailable()).doubleValue();


        Ticker ticker = getTicker(site, baseName, quotaName);
        Double marketPrice = Double.parseDouble(ticker.getLast());
        ApiService.log.info("ticker last {} -{}:{}", baseName, quotaName, marketPrice);


        double allAsset = baseBalance * marketPrice + quotaBalance;
        ApiService.log.info("basebalance:{}, qutobalance:{}, allAsset:{}, asset/2:{}, basebalance-quota:{}",
                baseBalance, quotaBalance, allAsset, allAsset*baseRatio, baseBalance * marketPrice );

        if (allAsset*baseRatio - baseBalance * marketPrice  > allAsset * increment) {
            BigDecimal amount = new BigDecimal(allAsset * baseRatio - baseBalance * marketPrice).setScale(ApiService.numPrecision, BigDecimal.ROUND_FLOOR);
            ApiService.log.info("basebalance:{}, quotabalance:{}", baseBalance + amount.doubleValue(),
                    quotaBalance - amount.doubleValue() * ApiService.getMarketPrice(marketPrice).doubleValue());
            ApiService.log.info("buy {}, price:{}", amount, marketPrice);
            //买入
            if (amount.doubleValue() - ApiService.minLimitPriceOrderNums.get(baseName.toLowerCase()) * marketPrice < 0) {
                ApiService.log.info("小于最小限价数量");
            } else {
                BigDecimal baseamount = amount.divide(new BigDecimal(marketPrice),
                        ApiService.numPrecision, BigDecimal.ROUND_DOWN);
                buy(site, symbol, "limit", baseamount, ApiService.getMarketPrice(marketPrice * (1 - increment)));
            }
        }


        if (baseBalance * marketPrice - allAsset * baseRatio > allAsset * increment) {
            //卖出
            BigDecimal amount = new BigDecimal(baseBalance * marketPrice - allAsset * baseRatio).setScale(ApiService.numPrecision, BigDecimal.ROUND_FLOOR);
            ApiService.log.info("basebalance:{}, quotabalance:{}", baseBalance - amount.doubleValue(),
                    quotaBalance + amount.doubleValue() * ApiService.getMarketPrice(marketPrice).doubleValue());
            ApiService.log.info("sell {}, price:{}", amount, marketPrice);
            if (amount.doubleValue() - ApiService.minLimitPriceOrderNums.get(baseName.toLowerCase()) * marketPrice < 0) {
                ApiService.log.info("小于最小限价数量");
            } else {
                BigDecimal baseamount = amount.divide(new BigDecimal(marketPrice),
                        ApiService.numPrecision, BigDecimal.ROUND_DOWN);
                sell(site, symbol, "limit", baseamount, ApiService.getMarketPrice(marketPrice * (1 + increment)));
            }

        }

    }
    public boolean cancelOrders(String site, List<OrderInfo> orderIds) throws Exception {
        if (orderIds == null || orderIds.size() == 0) {
            return false;
        }
        for (OrderInfo orderInfo : orderIds) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            if (System.currentTimeMillis() - 8 * 3600 * 1000 - dateFormat.parse(orderInfo.getCreated_at()).getTime() < 15 * 60 * 1000) {
                continue;
            }
            CurrencyOrder currencyOrder = currencyOrderMapper.selectByOrderId(orderInfo.getOrder_id().toString());
            ApiService.log.info("cancel order id:{},currencyOrder :{}", orderInfo.getOrder_id(), currencyOrder);
            if (currencyOrder != null && currencyOrder.getId() > 0) {
                PlaceOrderParam placeOrderParam = new PlaceOrderParam();
                placeOrderParam.setProduct_id(orderInfo.getProduct_id());
                spotOrderAPIService.cancleOrderByOrderId(site, placeOrderParam, orderInfo.getOrder_id());
            }
        }
        return true;
    }

    public void buyNotLimit(String site, String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        subBuy(site, amount.toString(), marketPrice.toString(), symbol, type, marketPrice.toPlainString());
    }

    public void sellNotLimit(String site, String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        subSell(site, amount.toString(), marketPrice.toString(), symbol, type, marketPrice.toPlainString());
    }

    private boolean isHaveInitBuyAndSell(String site, double base, double quota, double marketPrice, double initUsdt, String symbol, String type, double increment) throws Exception {
        //初始化小的
        double baseValue = base * marketPrice;
        double num = Math.min((Math.abs(quota - baseValue) / 2), initUsdt);
        BigDecimal b = getNum(num / marketPrice);//现价的数量都为ft的数量
        if (b.doubleValue() - ApiService.minLimitPriceOrderNums.get(symbol.split("-")[0].toLowerCase()) < 0) {
            ApiService.log.info("小于最小限价数量");
            return false;
        }
        if (baseValue < quota && Math.abs(baseValue - quota) > 0.1 * (baseValue + quota)) {
            //买ft
            try {
                buy(site, symbol, type, b, ApiService.getMarketPrice(marketPrice * (1 - increment)));//此处不需要重试，让上次去判断余额后重新平衡
            } catch (Exception e) {
                ApiService.log.error("初始化买有异常发生", e);
                throw new Exception(e);
            }

        } else if (quota < baseValue && Math.abs(baseValue - quota) > 0.1 * (baseValue + quota)) {
            //卖ft
            try {
                sell(site, symbol, type, b, ApiService.getMarketPrice(marketPrice * (1 + increment)));//此处不需要重试，让上次去判断余额后重新平衡
            } catch (Exception e) {
                ApiService.log.error("初始化卖有异常发生", e);
                throw new Exception(e);
            }
        } else {
            return false;
        }

        Thread.sleep(3000);
        return true;
    }

    public void buy(String site, String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        BigDecimal maxNumDeci = getNum(ApiService.maxNum);
        while (amount.doubleValue() > 0) {
            if (amount.compareTo(maxNumDeci) > 0) {
                subBuy(site, maxNumDeci.toString(), marketPrice.toString(), symbol, type, marketPrice.toPlainString());
            } else {
                subBuy(site, amount.toString(), marketPrice.toString(), symbol, type, marketPrice.toPlainString());
                break;
            }
            amount = amount.subtract(maxNumDeci);

            Thread.sleep(5000);
        }

    }

    public void sell(String site, String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        BigDecimal maxNumDeci = getNum(ApiService.maxNum);
        while (amount.doubleValue() > 0) {
            if (amount.compareTo(maxNumDeci) > 0) {
                subSell(site, maxNumDeci.toString(), marketPrice.toString(), symbol, type, marketPrice.toPlainString());
            } else {
                subSell(site, amount.toString(), marketPrice.toString(), symbol, type, marketPrice.toPlainString());
                break;
            }
            amount = amount.subtract(maxNumDeci);

            Thread.sleep(5000);
        }
    }

    public void subSell(String site, String amount, String price, String symbol, String type, String marketPrice) throws Exception {
        createOrder(site, amount, price, "sell", symbol, type, marketPrice);

    }

    public void subBuy(String site,String amount, String price, String symbol, String type, String marketPrice) throws Exception {
        createOrder(site, amount, price, "buy", symbol, type, marketPrice);

    }

    private void createOrder(String site, String amount, String price, String buy, String symbol, String type, String marketPrice) {
        PlaceOrderParam placeOrderParam = new PlaceOrderParam();
        placeOrderParam.setProduct_id(symbol);
        placeOrderParam.setPrice(price);
        placeOrderParam.setSize(amount);
        placeOrderParam.setSide(buy);
        placeOrderParam.setType(type);

        OrderResult result = spotOrderAPIService.addOrder(site, placeOrderParam);

        String[] currencys = symbol.split("-");
        CurrencyOrder order = CurrencyOrder.builder()
                .baseCurrency(currencys[0])
                .quotaCurrency(currencys[1])
                .orderId(result.getOrder_id().toString())
                .orderPrice(new BigDecimal(price))
                .markePrice(new BigDecimal(marketPrice))
                .amount(new BigDecimal(amount))
                .site(site)
                .createTime(new Date())
                .type(buy)
                .build();
        currencyOrderMapper.insert(order);

        //记录下单平均价格
        String key = symbol.toUpperCase()+"-"+buy.toUpperCase()+AVERAGE;
        log.info("create order value average key:{}",key);
        String value = stringRedisTemplate.opsForValue().get("key");
        if(null == value || value.trim() == ""){
            stringRedisTemplate.opsForValue().set(key,price);
        }else{
            stringRedisTemplate.opsForValue().set(key,new BigDecimal(value).add(new BigDecimal(price)).divide(new BigDecimal("2")).toString());
        }
    }

    public static BigDecimal getMarketPrice(double marketPrice) {
        return ApiService.getBigDecimal(marketPrice, ApiService.pricePrecision);
    }

    public static BigDecimal getBigDecimal(double value, int scale) {
        return new BigDecimal(value).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }
    public BigDecimal getNum(double b) {//为了尽量能够成交，数字向下精度
        return new BigDecimal(b).setScale(ApiService.numPrecision, BigDecimal.ROUND_DOWN);
    }
    public Account getBalance(String site, String currency) throws Exception {

        return spotAccountAPIService.getAccountByCurrency(site, currency);
    }

    public Ticker getTicker(String site, String baseCurrency, String quotaCurrency) {
        String symbol = baseCurrency.toUpperCase() + "-" + quotaCurrency.toUpperCase();
        MultiValueMap<String, String> headers = new HttpHeaders();
        APIKey apiKey = apiKeyService.getApiKey(site);
        headers.add("Referer", apiKey.getDomain());
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.84 Safari/537.36");
        HttpEntity requestEntity = new HttpEntity<>(headers);

        String url =  apiKey.getDomain() + "/api/spot/v3/products/"+symbol+"/ticker";
        RestTemplate client = new RestTemplate();

        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String body = response.getBody();
        ApiService.log.info(body);
        return JSON.parseObject(body,Ticker.class);

    }

    public List<OrderInfo> getNotTradeOrders(String site, String symbol, String after, String limit) throws Exception {
        List<OrderInfo> list1 = getOrders(site, symbol, "open", after, limit, null);
        List<OrderInfo> list2 = getOrders(site, symbol, "part_filled", after, limit, null);
        list1.addAll(list2);
        return list1;
    }

    public List<OrderInfo> getOrders(String site, String symbol, String states, String after, String limit, String side) throws Exception {
        return spotOrderAPIService.getOrders(site, symbol, states, null, null, null);

    }

    public ValuationTicker getValuationTicker() {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Referer", "www.okb.com");
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.84 Safari/537.36");
        HttpEntity requestEntity = new HttpEntity<>(headers);

        String url = "https://www.okb.com/v2/futures/market/indexTicker.do?symbol=f_usd_btc";
        RestTemplate client = new RestTemplate();
        log.info(url);
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String body = response.getBody();
        log.info(body);
        ResponseResult<ValuationTicker> result = JSON.parseObject(body, new TypeReference<ResponseResult<ValuationTicker>>() {
        });
        return result.getData();
    }

    public void collectBalance() throws InterruptedException {

        StringBuilder sb = new StringBuilder();

        String[] sites = new String[]{"coinall", "okex"};
        BigDecimal sum = BigDecimal.ZERO;
        ValuationTicker valuationTicker = getValuationTicker();
        for (String site : sites) {
            BigDecimal spotAmount = getSpotValuation(site).setScale(8, RoundingMode.DOWN);
            BigDecimal spotUsdtAmount = spotAmount.multiply(valuationTicker.getLast()).setScale(8, RoundingMode.DOWN);
            BigDecimal spotCnyAmount = spotUsdtAmount.multiply(valuationTicker.getUsdCnyRate()).setScale(8, RoundingMode.DOWN);
            sb.append(site).append(":").append("spot:").append(spotAmount)
                    .append("usdt:").append(spotUsdtAmount)
                    .append("cny:").append(spotCnyAmount)
                    .append("\r\n\r\n");
            sum = sum.add(spotAmount);
            BigDecimal walletAmount = getWalletValuation(site).setScale(8, RoundingMode.DOWN);
            BigDecimal walletUsdtAmount = walletAmount.multiply(valuationTicker.getLast()).setScale(8, RoundingMode.DOWN);
            BigDecimal walletCnyAmount = walletUsdtAmount.multiply(valuationTicker.getUsdCnyRate()).setScale(8, RoundingMode.DOWN);
            sb.append(site).append(":").append("wallet:").append(walletAmount)
                    .append("usdt:").append(walletUsdtAmount)
                    .append("cny:").append(walletCnyAmount)
                    .append("\r\n\n");
            sum = sum.add(walletAmount);
        }
        sum = sum.setScale(8, RoundingMode.DOWN);
        BigDecimal usdtSum = sum.multiply(valuationTicker.getLast()).setScale(8, RoundingMode.DOWN);
        BigDecimal cnySum = usdtSum.multiply(valuationTicker.getUsdCnyRate()).setScale(8, RoundingMode.DOWN);
        sb.append("all").append(":").append(sum).append("usdt:").append(usdtSum).append("cny:").append(cnySum);
        WXInfoUtils.sendInfo("balance", sb.toString());

    }

    private BigDecimal getWalletValuation(String site) throws InterruptedException {
        List<String> noValueCurrencies = new ArrayList<>();
        noValueCurrencies.add("EON");
        noValueCurrencies.add("ADD");
        noValueCurrencies.add("CHL");
        noValueCurrencies.add("EOP");
        noValueCurrencies.add("EOX");
        noValueCurrencies.add("HORUS");
        noValueCurrencies.add("IQ");
        noValueCurrencies.add("MEETONE");


        BigDecimal sum = BigDecimal.ZERO;
        List<Wallet> accounts = accountAPIService.getWallet(site);
        if (!CollectionUtils.isEmpty(accounts)) {
            for (Wallet account : accounts) {
                if (noValueCurrencies.contains(account.getCurrency().toUpperCase())) {
                    continue;
                }
                if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    if ("usdt".equalsIgnoreCase(account.getCurrency())) {
                        Ticker ticker = getTicker(site, "btc", "usdt");
                        sum = sum.add(account.getBalance().divide(new BigDecimal(ticker.getLast()), 8, RoundingMode.DOWN));

                    } else if (!"btc".equalsIgnoreCase(account.getCurrency())) {
                        Ticker ticker = getTicker(site, account.getCurrency(), "btc");
                        sum = sum.add(account.getBalance().multiply(new BigDecimal(ticker.getLast())));
                    } else {
                        sum = sum.add(account.getBalance());
                    }
                }
                Thread.sleep(200);
            }
        }
        return sum;
    }

    private BigDecimal getSpotValuation(String site) throws InterruptedException {
        BigDecimal sum = BigDecimal.ZERO;
        List<Account> accounts = spotAccountAPIService.getAccounts(site);
        if (!CollectionUtils.isEmpty(accounts)) {
            for (Account account : accounts) {
                if (Double.parseDouble(account.getBalance()) > 0) {
                    if ("usdt".equalsIgnoreCase(account.getCurrency())) {
                        Ticker ticker = getTicker(site, "btc", "usdt");
                        sum = sum.add(new BigDecimal(account.getBalance()).divide(new BigDecimal(ticker.getLast()), 8, RoundingMode.DOWN));

                    } else if (!"btc".equalsIgnoreCase(account.getCurrency())) {
                        Ticker ticker = getTicker(site, account.getCurrency(), "btc");
                        sum = sum.add(new BigDecimal(account.getBalance()).multiply(new BigDecimal(ticker.getLast())));
                    } else {
                        sum = sum.add(new BigDecimal(account.getBalance()));
                    }
                }
                Thread.sleep(200);
            }
        }
        return sum;
    }

    /**
     * 判断定时任务是否允许执行
     * @param taskId
     * @return
     */
    public boolean  canExecute(long taskId){
        List<TaskConfig> taskConfigList = taskConfigMapper.select();
        TaskConfig config =
                taskConfigList.stream().filter(
                        taskConfig -> taskConfig.getId().longValue() == taskId)
                        .findFirst()
                        .orElse(null);
        if(null != config){
            if(config.getStatus() != 0){
                return false;
            }
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if(config.getTimeStatus() == 0){
                if(hour >= config.getStartHour() && hour <= config.getEndHour()){
                    return true;
                }
                return false;
            }else if(config.getTimeStatus() == 1){
                if(hour < config.getStartHour() && hour > config.getEndHour()){
                    return true;
                }
                return false;
            }
        }
        return true;
    }

}
