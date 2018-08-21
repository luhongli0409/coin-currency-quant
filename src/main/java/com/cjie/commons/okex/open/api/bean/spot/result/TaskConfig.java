package com.cjie.commons.okex.open.api.bean.spot.result;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
* Created by Liu Hailin 2018/08/21
*/
@Data
@ToString
public class TaskConfig implements Serializable {
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 是否执行 0 默认执行
     */
    private Integer status;

    /**
     * 0 时间段内执行 1 时间段外执行
     */
    private Integer timeStatus;

    /**
     * 开始时间
     */
    private Integer startHour;

    /**
     * 截止时间
     */
    private Integer endHour;

    private static final long serialVersionUID = 1L;
}