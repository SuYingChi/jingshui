package com.msht.watersystem.widget;


import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.msht.watersystem.R;


/**
 * 加载自定义对话框控件
 */
public class LoadingDialog extends Dialog {

    private Context mContext;
    private LayoutInflater inflater;
    private LayoutParams lp;
    private TextView loadtext;
    private ImageView loading_imageview;
    private AnimationDrawable animationDrawable;

    /**
     * loading = new LoadingDialog(this); loading.show();
     *
     * @param context
     */
    public LoadingDialog(Context context) {
        super(context, R.style.loading_dialog);
        this.mContext = context;
        inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.loadingdialog_layout, null);
        loadtext = (TextView) layout.findViewById(R.id.loading_text);
        loading_imageview = (ImageView) layout
                .findViewById(R.id.loading_imageview);
        setContentView(layout);
        animationDrawable = (AnimationDrawable) loading_imageview.getDrawable();
        this.setCanceledOnTouchOutside(false);
        this.setCancelable(false);
    }

    @Override
    public void show() {
        if (!animationDrawable.isRunning()) {
            animationDrawable.start();
        }
        super.show();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                if(LoadingDialog.this.isShowing())
                    LoadingDialog.this.dismiss();
                break;
        }
        return true;
    }


    @Override
    public void dismiss() {
        if (animationDrawable.isRunning()) {
            animationDrawable.stop();
        }
        super.dismiss();
    }
    public void setLoadText(String content) {
        loadtext.setText(content);
    }
}