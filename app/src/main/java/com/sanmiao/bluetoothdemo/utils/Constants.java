package com.sanmiao.bluetoothdemo.utils;

import java.util.UUID;

/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/11/16 0016 16:16.
 */

public class Constants {
    //    cmd
    public static final String CMD_TURN_ON = "7e000201292713557f";
    public static final String CMD_TURN_OFF = "7e000200292713557f";
    public static final String CMD_QUERY = "7e000101292713557f";

    //  uuid
    public static final UUID UUID_CONNECT = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SEND_CMD = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    //  cmd interval
    public static final long CMD_INTERVAL = 100;
}
