package com.msht.watersystem.Utils;

import android.content.Context;

/**
 * 处理dip 和px之间的相互转换
 *
 * @author huangzhong E-mail:mcloyal@163.com
 * @version 创建时间：2014-4-26 上午9:52:25
 */
public class SizeTools {

    /**
     * 根据手机分辨率从dp转成px
     *
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
	
    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f) - 15;
    }
}
