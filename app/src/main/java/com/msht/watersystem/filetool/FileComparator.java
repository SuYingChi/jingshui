package com.msht.watersystem.filetool;

import java.io.File;
import java.util.Comparator;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/12  
 */
public class FileCompatator implements Comparator<File> {
    @Override
    public int compare(File o1, File o2) {
        // TODO Auto-generated method stub
        // 都是目录
        if (o1.isDirectory() && o2.isDirectory()) {
            //都是目录时按照名字排序
            return o1.getName().compareToIgnoreCase(o2.getName());
            //目录与文件.目录在前
        } else if (o1.isDirectory() && o2.isFile()) {
            return -1;
            //文件与目录
        } else if (o2.isDirectory() && o1.isFile()) {
            return 1;
        } else {
            //都是文件
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
