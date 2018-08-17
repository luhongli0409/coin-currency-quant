package com.cjie.cryptocurrency.quant.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CurrencyOrder {
    private Integer id;

    private String orderId;

    private String baseCurrency;

    private String quotaCurrency;

    private BigDecimal orderPrice;

    private BigDecimal markePrice;

    private BigDecimal amount;

    private String site;

    private Date createTime;

    private Integer type;

}