package com.cjie.commons.okex.open.api.enums;


public enum TaskEnum {

    BALANCE_SEND_WX_TASK(1),

    COIN_TRADE_TASK(2),;

    private long taskId;

    TaskEnum(long taskId) {
        this.taskId = taskId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
