package com.moyu.xmz.common.util;

import com.moyu.xmz.common.exception.FileOperationException;

import java.io.File;

/**
 * @author xiaomingzhang
 * @date 2023/4/21
 */
public class FileUtils {


    public static void createFileIfNotExists(String fullPath) {
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileOperationException("创建文件发生异常");
        }
    }


    public static void createDirIfNotExists(String fullPath) {
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileOperationException("创建文件发生异常");
        }
    }



    public static void deleteOnExists(String fullPath) {
        try {
            File file = new File(fullPath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileOperationException("删除文件发生异常");
        }
    }


    public static boolean exists(String fullPath) {
        File file = new File(fullPath);
        return file.exists();
    }

}
