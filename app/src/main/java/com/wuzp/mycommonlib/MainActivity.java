package com.wuzp.mycommonlib;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import com.wuzp.commonlib.Utils.ToastUtils;

public class MainActivity extends AppCompatActivity {

    Handler mHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ToastUtils.show(MainActivity.this, "this is a lib util toast here");
            }
        }, 5000);
    }
}
