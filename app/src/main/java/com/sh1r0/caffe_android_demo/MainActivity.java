package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import com.sh1r0.caffe_android_demo.artphelper.ARTPHelper;
import com.zqlite.android.onepiece.OnePiece;
import com.zqlite.android.onepiece.ViewID;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private Button btnCaffe;

    @ViewID(R.id.show_window)
    private Button mShowWindowBtn ;
    @ViewID(R.id.hide_window)
    private Button mHideWindowBtn;
    final ARTPHelper artpHelper = new ARTPHelper(true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OnePiece.own().initViews(this);

        artpHelper.writeExternalStorage();
        artpHelper.requestPermissions(this);

        mShowWindowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        Lolly.showLolly(MainActivity.this, new String[]{"caffe"});
                    }else{
                        artpHelper.tryToDropZone(MainActivity.this);
                    }
                }else{
                    Lolly.showLolly(MainActivity.this, new String[]{"caffe"});
                }

            }
        });

        mHideWindowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Lolly.hideLolly(MainActivity.this);
            }
        });

        Lolly.saveLog(MainActivity.this);

        btnCaffe = (Button) findViewById(R.id.btnCaffe);
        btnCaffe.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent inferredIntent = new Intent(getApplicationContext(), CaffeInferenceService.class);
                startService(inferredIntent);
            }
        });

/*        btnTensorflow = (Button) findViewById(R.id.btnTensorflow);
        btnTensorflow.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent inferredIntent = new Intent(getApplicationContext(), TensorflowInferenceService.class);
                inferredIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(inferredIntent);
            }
        });*/
    }
}
