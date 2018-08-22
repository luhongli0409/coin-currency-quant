package com.cjie.commons.okex.open.api.bean.spot.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * ResponseBody注解返回的JSON对象类
 *
 * @author okcoin-team
 * @date 2017-03-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseResult<T> implements Serializable {

    private T data;

    public static ResponseResult success() {
        return success(new Object());
    }

    public static <T> ResponseResult<T> success(final T data) {
        return build(data);
    }


    public static <T> ResponseResult<T> build(final T data) {
        return new ResponseResult<>(data);
    }
}
