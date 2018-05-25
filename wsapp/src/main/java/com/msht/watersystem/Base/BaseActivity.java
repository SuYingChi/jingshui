package com.msht.watersystem.Base;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.AppManager;
import com.mcloyal.serialport.AppPreferences;
import com.msht.watersystem.AppContext;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.widget.LoadingDialog;

/**
 * Created by huangzhong on 2015/9/17.
 */
public abstract class BaseActivity extends AppCompatActivity {
    public TextView  tv_InTDS;
    public TextView  tv_OutTDS;
    public View      layout_TDS;
    public Context   mContext;
    public AppLibsContext appLibsContext;
    public AppPreferences appPreferences;
    public LoadingDialog  loadingdialog;

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
        ((AppContext)getApplication()).addActivity(this);
        mContext = this;
        appLibsContext = (AppLibsContext) this.getApplicationContext();
        appPreferences = AppPreferences.getInstance(appLibsContext);
      //  AppManager.getAppManager().addActivity(this);
        loadingdialog = new LoadingDialog(mContext);
    }
    protected void iniWaterQualiy() {
        tv_InTDS=(TextView)findViewById(R.id.id_in_tds);
        tv_OutTDS=(TextView)findViewById(R.id.id_out_tds);
        layout_TDS=findViewById(R.id.id_tds_layout);
        tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
        tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
        int tds= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.ShowTds,0);
        if (tds==0){
            layout_TDS.setVisibility(View.GONE);
        }else {
            layout_TDS.setVisibility(View.VISIBLE);
        }
    }
    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

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
