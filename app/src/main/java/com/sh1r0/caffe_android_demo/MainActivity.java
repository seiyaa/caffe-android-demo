package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sh1r0.caffe_android_demo.window.LogViewerService;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private Button btnCaffe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCaffe = (Button) findViewById(R.id.btnCaffe);
        btnCaffe.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent inferredIntent = new Intent(getApplicationContext(), CaffeInferenceService.class);
                startService(inferredIntent);
            }
        });

        Intent i = new Intent();
        i.setClass(getApplicationContext(), LogViewerService.class);
        startService(i);

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
