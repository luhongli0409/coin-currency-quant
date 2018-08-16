package com.cjie.commons.okex.open.api.task;

import com.alibaba.fastjson.JSON;
import com.cjie.commons.okex.open.api.bean.spot.result.Ticker;
import com.cjie.cryptocurrency.quant.mapper.TickerMapper;
import com.cjie.cryptocurrency.quant.model.APIKey;
import com.cjie.cryptocurrency.quant.service.ApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class TickerService {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private TickerMapper tickerMapper;

    public void getTicker(String site, String baseCurrency, String quotaCurrency) {
        String symbol = baseCurrency.toUpperCase() + "-" + quotaCurrency.toUpperCase();
        MultiValueMap<String, String> headers = new HttpHeaders();
        APIKey apiKey = apiKeyService.getApiKey(site);
        headers.add("Referer", apiKey.getDomain());
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.84 Safari/537.36");
        HttpEntity requestEntity = new HttpEntity<>(headers);

        String url = apiKey.getDomain() + "/api/spot/v3/products/" + symbol + "/ticker";
        RestTemplate client = new RestTemplate();

        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String body = response.getBody();
        TickerService.log.info(body);
        Ticker ticker = JSON.parseObject(body, Ticker.class);
        tickerMapper.insert(ticker);
    }
}
