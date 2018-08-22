package com.cjie.commons.okex.open.api.bean.spot.result;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @Author: hongli.lu
 * @Date: 2018/8/22 下午6:05
 */
@Data
@ToString
public class ValuationTicker {

    private BigDecimal changePercent;

    private BigDecimal high;

    private BigDecimal last;

    private BigDecimal low;

    private BigDecimal open;

    private BigDecimal usdCnyRate;
}
