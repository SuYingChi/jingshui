package com.msht.watersystem.widget;


import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.msht.watersystem.R;


/**
 * 加载自定义对话框控件
 * @author hong
 */
public class LoadingDialog extends Dialog {
    private TextView loadText;
    private Context mContext;
   // private AnimationDrawable animationDrawable;

    /**
     * loading = new LoadingDialog(this); loading.show();
     *
     * @param context
     */
    public LoadingDialog(Context context) {
        super(context, R.style.loading_dialog);
        this.mContext = context;
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater!=null){
            View layout = inflater.inflate(R.layout.loadingdialog_layout, null);
            loadText = (TextView) layout.findViewById(R.id.loading_text);
            /*ImageView loadingImageView = (ImageView)layout
                    .findViewById(R.id.loading_imageView);*/
            setContentView(layout);
         //   animationDrawable = (AnimationDrawable) loadingImageView.getDrawable();
            this.setCanceledOnTouchOutside(false);
            this.setCancelable(false);
        }
    }

    @Override
    public void show() {
        /*if (!animationDrawable.isRunning()) {
            animationDrawable.start();
        }*/
        super.show();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                if(LoadingDialog.this.isShowing()){
                    LoadingDialog.this.dismiss();
                }
                break;
                default:
                    break;
        }
        return true;
    }


    @Override
    public void dismiss() {
        /*if (animationDrawable.isRunning()) {
            animationDrawable.stop();
        }*/
        super.dismiss();
    }
    public void setLoadText(String content) {
        loadText.setText(content);
    }
}