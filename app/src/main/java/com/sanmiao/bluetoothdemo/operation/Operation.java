package com.sanmiao.bluetoothdemo.operation;

/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/11/19 0019 10:08.
 */

public abstract class Operation{

    public int TIME_OUT = 500;

    public int SATISFY_TYPE = -1;

    public String ID = "";

    protected abstract boolean operate();
}
