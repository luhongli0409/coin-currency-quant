package com.cjie.commons.okex.open.api.bean.spot.result;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class OrderInfo {

    private Long order_id;

    private String price;

    private String funds;

    private String size;

    private String created_at;

    private String filled_size;

    private String exectued_value;

    private String status;

    private String side;

    private String type;

    private String product_id;

}
