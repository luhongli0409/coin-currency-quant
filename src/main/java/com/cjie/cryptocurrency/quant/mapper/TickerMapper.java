package com.cjie.cryptocurrency.quant.mapper;

import com.cjie.commons.okex.open.api.bean.spot.result.Ticker;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TickerMapper {

    List<Ticker> select(Ticker record);

    int deleteByPrimaryKey(Long id);

    int insert(Ticker record);

    int insertSelective(Ticker record);

    Ticker selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Ticker record);

    int updateByPrimaryKey(Ticker record);

    Ticker selectByProductId(@Param("product_id") String productId);

    String selectMaxByProductId(@Param("product_id") String productId, @Param("limitNum") Integer limitNum);

    String selectMinByProductId(@Param("product_id") String productId,@Param("limitNum") Integer limitNum);

}