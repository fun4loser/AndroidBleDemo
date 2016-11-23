package com.sanmiao.bluetoothdemo.operation;

/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/11/19 0019 10:58.
 */

public abstract class DisconnectOperation extends Operation {

    public static final int TIME_OUT = 1000;
    public static final int SATISFY_TYPE = 2;

    {
        super.TIME_OUT = TIME_OUT;
        super.SATISFY_TYPE = SATISFY_TYPE;
    }
}
