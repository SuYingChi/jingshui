package com.msht.watersystem.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;

import com.msht.watersystem.filetool.MyFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/6  
 */
public class BitmapViewListUtil {
    public  static List<Bitmap> getBitmapViewListUtil(Context context){
        List<Bitmap> imageList= new ArrayList<Bitmap>();
        List<String> fileImageList = new ArrayList<String>();
        File scanner5Directory = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/images/");
        if (scanner5Directory.exists() && scanner5Directory.isDirectory() && scanner5Directory.list().length > 0) {
            for (File file : scanner5Directory.listFiles()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) {
                    fileImageList.add(path);
                }
            }
            for (int i = 0; i < fileImageList.size(); i++) {
                imageList.add(BitmapUtil.decodeSampledBitmapFromRSavaSD(fileImageList.get(i), 1633, 888));
                VariableUtil.imageViewList=imageList;
            }
        } else if (!scanner5Directory.exists()) {
            scanner5Directory.mkdirs();
        }
        return imageList;
    }

    public  static List<Bitmap> getBitmapListUtil(Context context){
        List<Bitmap> imageList= new ArrayList<Bitmap>();
        List<String> fileImageList = new ArrayList<String>();
        File scanner5Directory = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/images/");
        File[] fileList=scanner5Directory.listFiles(new MyFileFilter());
        if (scanner5Directory.exists() && scanner5Directory.isDirectory() && scanner5Directory.list().length > 0) {
            fileList = FileUtil.sort(fileList);
            for (File file : fileList) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) {
                    fileImageList.add(path);
                }
            }
            for (int i = 0; i < fileImageList.size(); i++) {
                imageList.add(BitmapUtil.decodeSampledBitmapFromRSavaSD(fileImageList.get(i), 1633, 888));
                VariableUtil.imageViewList=imageList;
            }
        } else if (!scanner5Directory.exists()) {
            scanner5Directory.mkdirs();
        }
        return imageList;
    }
}
