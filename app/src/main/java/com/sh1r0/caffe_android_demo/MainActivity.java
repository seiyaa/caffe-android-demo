package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class MainActivity extends Activity implements CNNListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static String[] IMAGENET_CLASSES;

    private Button btnCamera;
    private ImageView ivCaptured;
    private TextView tvLabel;
    private ProgressDialog dialog;
    private CaffeMobile caffeMobile;
    File sdcard = Environment.getDataDirectory();
    String modelDir = sdcard.getAbsolutePath() + "/caffe_mobile/bvlc_reference_caffenet";
    String modelProto = modelDir + "/deploy.prototxt";
    String modelBinary = modelDir + "/bvlc_reference_caffenet.caffemodel";

    private CapturerA capturerA;
    private boolean shouldStop = false;
    private MessageHandler mHandler;
    private class MessageHandler extends Handler {

        public static final int MSG_INIT = 0;
        public static final int MSG_EXECUTE = 1;

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT: {
                    capturerA = new CapturerA();
                    boolean result = capturerA.isCaptureSupported(getApplicationContext());
                    Log.i(TAG, "captureA can capture screen?" + result);
                    break;
                }
                case MSG_EXECUTE: {
                    Log.i(TAG, "CNNTask executed");

                    while(!shouldStop) {
//                        String imgPath = "/data/cartoon/1.bmp";
//                        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);

                        Bitmap screen = capturerA.getScreenCapture(getApplicationContext(), 1280, 720);
//                        dialog = ProgressDialog.show(MainActivity.this,
//                                "Predicting...", "Wait for one sec...", true);

                        String fileFullPath = saveBitmapFile(screen);
                        CNNTask cnnTask = new CNNTask(MainActivity.this);
                        cnnTask.execute(fileFullPath);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                    break;
                }

                default: {
                    Log.e(TAG, "handleMessage received unexpected msg=" + msg);
                    break;
                }
            }
        }
    }

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                mHandler.sendEmptyMessage(MessageHandler.MSG_EXECUTE);
            }
        });


        // TODO: implement a splash screen(?
        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(modelProto, modelBinary);

        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);

        AssetManager am = getAssets();
        try {
            InputStream is = am.open("word.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            IMAGENET_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new MessageHandler(thread.getLooper());
        mHandler.sendEmptyMessage(MessageHandler.MSG_INIT);
        ivCaptured.setImageResource(R.drawable.cartoon_1);
    }


    private void initPrediction() {
        btnCamera.setEnabled(false);
//        btnSelect.setEnabled(false);
        tvLabel.setText("");
    }

    private class CNNTask extends AsyncTask<String, Void, Integer> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            Log.d(TAG, "strings=" + strings[0] );
            return caffeMobile.predictImage(strings[0])[0];
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.i(TAG, String.format("elapsed time %d ms, label is %s",
                    SystemClock.uptimeMillis() - startTime, IMAGENET_CLASSES[integer]));
            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
    }

    @Override
    public void onTaskCompleted(int result) {
        tvLabel.setText(IMAGENET_CLASSES[result]);
        btnCamera.setEnabled(true);

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final String CMD_CHMOD_SCREENCAPTURE_FILE = "chmod 777 /data/screen*.jpg";

    private String saveBitmapFile(Bitmap bmp) {
        if (bmp == null || bmp.isRecycled()) return null;

        // create new file
        String fileName = Environment.getDataDirectory() + "/screen.jpg";
        File file = new File(fileName);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();

            // change chmod 777
            Runtime.getRuntime().exec(CMD_CHMOD_SCREENCAPTURE_FILE);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                    out = null;
                } catch (Exception e) {
                }
            }
        }

        return fileName;
    }
}
