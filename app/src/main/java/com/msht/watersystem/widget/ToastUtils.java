package com.msht.watersystem.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.msht.watersystem.R;
import com.msht.watersystem.Utils.SizeTools;


/**
 * 提示信息
 *
 * @author huangzhong E-mail:mcloyal@163.com
 */
public class ToastUtils {
    private final static int BOTTOMSIZE = 40;

    /**
     * 弹出Toast消息
     *
     * @param msg
     */
    public static void toastMessage(Context context, String msg) {
        createToast(context, msg);

    }

    public static void toastMessage(Context context, int msg) {
        createToast(context, msg);
    }

    public static void toastMessage(Context context, String msg, int time) {
        createToast(context, msg, time);
    }

    /**
     * 创建Toast并显示
     *
     * @param context
     */
    private static void createToast(Context context, String msg) {
        View layout = View.inflate(context, R.layout.widget_toast_layout,
                null);
        TextView toastTxt = (TextView) layout.findViewById(R.id.toast_txt);
        toastTxt.setText(msg);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, SizeTools.dip2px(context, BOTTOMSIZE));
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    /**
     * 创建Toast并显示
     *
     * @param context
     */
    private static void createToast(Context context, int msg) {
        View layout = View.inflate(context, R.layout.widget_toast_layout,
                null);
        TextView toastTxt = (TextView) layout.findViewById(R.id.toast_txt);
        toastTxt.setText(msg);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, SizeTools.dip2px(context, BOTTOMSIZE));
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    /**
     * 创建Toast并显示
     *
     * @param context
     */
    private static void createToast(Context context, String msg, int time) {
        View layout = View.inflate(context, R.layout.widget_toast_layout,
                null);
        TextView toastTxt = (TextView) layout.findViewById(R.id.toast_txt);
        toastTxt.setText(msg);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, SizeTools.dip2px(context, BOTTOMSIZE));
        toast.setDuration(time);
        toast.setView(layout);
        toast.show();
    }
}
