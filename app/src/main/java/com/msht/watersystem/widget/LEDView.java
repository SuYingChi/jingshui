package com.msht.watersystem.widget;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.msht.watersystem.R;

import java.io.File;

/**
 * Created by hong on 2017/11/7.
 */

public class LEDView extends LinearLayout {

    private static final String FONT_DIGITAL_7 = "fonts" + File.separator
            + "digital-7.ttf";
    private TextView ledNumber;
    private TextView ledBg;
    public LEDView(Context context) {
        super(context);
        initView(context);
    }

    public LEDView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public LEDView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.ledview_layout, this);
        ledNumber = (TextView) view.findViewById(R.id.ledview_number);
        ledBg = (TextView) view.findViewById(R.id.ledview_bg);
        AssetManager assets = context.getAssets();
        final Typeface font = Typeface.createFromAsset(assets, FONT_DIGITAL_7);
        ledNumber.setTypeface(font);// 设置字体样式
        ledBg.setTypeface(font);// 设置字体样式
    }
    /**
     * 显示电子数字
     * @param
     * @param bg 背景数字显示样式，即背景数字
     * @param number 需要显示的数字样式
     */
    public void setLedView( String bg, String number) {
        /*ledBg.setTextSize(size);
        ledNumber.setTextSize(size);*/
        ledBg.setText(bg);
        ledNumber.setText(number);
    }

}
