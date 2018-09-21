package com.msht.watersystem.filetool;

import java.io.File;
import java.io.FileFilter;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/12  
 */
public class MyFileFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
        if (!pathname.getName().startsWith(".")){
            return true;
        } else {
            return false;
        }
    }
}
