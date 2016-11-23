package com.sanmiao.genuine.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/9/19 0019.
 */
public class FileUtils {
    public static void append(byte[] bytes, String file){
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(file, true);
            fos.write(bytes);
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void write(byte[] bytes, String file){
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static byte[] read(String file){
        FileInputStream fis = null;
        File f = new File(file);
        byte[] bytes = new byte[(int)f.length()];
        try{
            fis = new FileInputStream(f);
            fis.read(bytes);
        }catch (Exception e){
            e.printStackTrace();
        }
        return bytes;
    }
}
