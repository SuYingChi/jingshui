package com.msht.watersystem.base;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.AppPreferences;
import com.msht.watersystem.AppContext;
import com.msht.watersystem.Interface.ResendDataEvent;
import com.msht.watersystem.R;
import com.msht.watersystem.utilpackage.CachePreferencesUtil;
import com.msht.watersystem.utilpackage.FormatInformationBean;
import com.msht.watersystem.widget.LoadingDialog;
/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2015/9/17  
 */
public abstract class BaseActivity extends AppCompatActivity  {
    public TextView tvInTDS;
    public TextView tvOutTDS;
    public View layoutTDS;
    public Context   mContext;
    public AppLibsContext appLibsContext;
    public AppPreferences appPreferences;
    public LoadingDialog  loadingdialog;
    public static ResendDataEvent event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
       /* if(Build.VERSION.SDK_INT>=16){
            Window window=getWindow();
            WindowManager.LayoutParams params=window.getAttributes();
            params.systemUiVisibility=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            window.setAttributes(params);
        }*/
      //  ((AppContext)getApplication()).addActivity(this);
        mContext = this;
        appLibsContext = (AppLibsContext) this.getApplicationContext();
        appPreferences = AppPreferences.getInstance(appLibsContext);
      //  AppManager.getAppManager().addActivity(this);
        loadingdialog = new LoadingDialog(mContext);
    }
    protected void initWaterQuality() {
        tvInTDS =(TextView)findViewById(R.id.id_in_tds);
        tvOutTDS =(TextView)findViewById(R.id.id_out_tds);
        layoutTDS =findViewById(R.id.id_tds_layout);
        tvInTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
        tvOutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
        int tds= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.SHOW_TDS,0);
        if (tds==0){
            layoutTDS.setVisibility(View.GONE);
        }else {
            layoutTDS.setVisibility(View.VISIBLE);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

   // public abstract void onDestroyView();

    @Override
    protected void onDestroy() {
        super.onDestroy();
      //  AppManager.getAppManager().finishActivity(this);
      if (loadingdialog != null && loadingdialog.isShowing()) {
            loadingdialog.dismiss();
        }
        ((AppContext)getApplication()).removeActivity(this);
    }
}
