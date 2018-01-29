/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sh1r0.caffe_android_demo;

import android.app.IntentService;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.jamdeo.tv.PictureManager;
import com.jamdeo.tv.TvManager;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TensorflowInferenceService extends IntentService {
    private static final String TAG = TensorflowInferenceService.class.getSimpleName();
    private CapturerA capturerA;

    private static final int INPUT_SIZE = 256;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "conv1.name";
    private static final String OUTPUT_NAME = "softmax_linear/out";

    private static final String MODEL_FILE = "file:///android_asset/graph_wyl_train3.pb";
    private static final String LABEL_FILE = "file:///android_asset/label.txt";

    public static final String PHOTO_FILE = "/data/screen.jpg";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private Handler mHandler;

    public TensorflowInferenceService() {
        super("TensorflowInferenceService");
    }

    public TensorflowInferenceService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.i(TAG, "onHandleIntent, action in workIntent: " + workIntent);
        mHandler.sendEmptyMessage(MessageHandler.MSG_EXECUTE);
        Looper.loop();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new MessageHandler(thread.getLooper());
        mHandler.sendEmptyMessage(MessageHandler.MSG_INIT);
    }

    private class MessageHandler extends Handler {
        public static final int MSG_INIT = 0;
        public static final int MSG_EXECUTE = 1;

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT: {
                    try {
                        classifier = TensorFlowImageClassifier.create(
                                getAssets(),
                                MODEL_FILE,
                                LABEL_FILE,
                                INPUT_SIZE,
                                IMAGE_MEAN,
                                IMAGE_STD,
                                INPUT_NAME,
                                OUTPUT_NAME);
                    } catch (final Exception e) {
                        throw new RuntimeException("Error initializing TensorFlow!", e);
                    }

                    capturerA = new CapturerA();
                    boolean result = capturerA.isCaptureSupported(getApplicationContext());
                    Log.i(TAG, "captureA can capture screen?" + result);
                    break;
                }
                case MSG_EXECUTE: {
                    Log.i(TAG, "tf executed");

                    while (true) {
                        Bitmap bitmap = capturerA.getScreenCapture(getApplicationContext(), INPUT_SIZE, INPUT_SIZE);
                        if (null == bitmap) {
                            Log.e(TAG, bitmap + " is null");
                            return;
                        }
                        if (null == classifier) {
                            Log.e(TAG, "classifier is null, try again");
                            return;
                        }

                        List<Classifier.Recognition> results = new ArrayList<>();
                        results = classifier.recognizeImage(bitmap);
                        Log.v(TAG, results.size() + " results");

                        float confidence = results.get(0).getConfidence();

                        Toast.makeText(getApplicationContext(), results.get(0).getTitle(), Toast.LENGTH_SHORT).show();
                        PictureManager pictureManager = TvManager.getInstance().getPictureManager(getApplicationContext());
//                        pictureManager.setPictureMode(result);

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                }

                default: {
                    Log.e(TAG, "handleMessage received unexpected msg=" + msg);
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (null != classifier) {
                    classifier.close();
                }
            }
        });
    }
}
