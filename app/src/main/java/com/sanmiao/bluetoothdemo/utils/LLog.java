package com.sanmiao.bluetoothdemo.utils;


/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/11/16 0016 10:02.
 */

public class LLog {

    public static final String TAG = "=-=";

    public static void d(Class<?> clz, String msg) {
        System.out.println(clz.getSimpleName() + TAG + msg);
    }
}
