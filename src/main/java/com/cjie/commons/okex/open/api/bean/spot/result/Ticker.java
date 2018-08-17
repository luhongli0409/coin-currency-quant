package com.cjie.commons.okex.open.api.bean.spot.result;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Ticker {

    private Long id;

    private String buy;

    private String high_24h;

    private String last;

    private String low_24h;

    private String sell;

    private String volume;

    private String open_24h;

    private String close;

    private String product_id;

    private String timestamp;

    private String best_ask;

    private String best_bid;
}
