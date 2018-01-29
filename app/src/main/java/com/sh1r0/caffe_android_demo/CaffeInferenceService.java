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
import com.jamdeo.tv.common.EnumConstants;
import com.sh1r0.caffe_android_demo.window.LogViewerService;
import com.sh1r0.caffe_android_lib.CaffeMobile;
import com.zqlite.android.logly.Logly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CaffeInferenceService extends IntentService implements CNNListener {
    private static final String TAG = CaffeInferenceService.class.getSimpleName();
    private static final int MAX_QUEUE = 10;
    private static Map<Integer, Integer> PICTURE_MODE_MAPPER = new HashMap<Integer, Integer>();

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    private Executor executor = Executors.newSingleThreadExecutor();
    private Queue<Integer> resultQueue = new LinkedList<>();
    private List<CaffeResult> resultCounter = new ArrayList<>();

    public CaffeInferenceService() {
        super("CaffeInferenceService");
    }

    public CaffeInferenceService(String name) {
        super(name);
    }

    private static final String SCREEN_FILE = "/data/screen.jpg";
    private CaffeMobile caffeMobile;
    private CapturerA capturerA;
    private boolean shouldStop = false;
    private MessageHandler mHandler;
    File sdcard = Environment.getDataDirectory();
    String modelDir = sdcard.getAbsolutePath() + "/caffe_mobile";
    String modelProto = modelDir + "/my_deploy_mobile.prototxt";
    String modelBinary = modelDir + "/_iter_90000.caffemodel";
    private static String[] IMAGENET_CLASSES;

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
    }

    @Override
    public void onTaskCompleted(int result) {
        resultQueue.add(result);

        CaffeResult caffeResult = new CaffeResult(result);
        int index = resultCounter.indexOf(caffeResult);
        int count = resultCounter.get(index).getCount() + 1;
        resultCounter.get(index).setCount(count);
        Logly.i("resultQueue=" + resultQueue);
        LogViewerService.addMessage("resultQueue=" + resultQueue);

        if (resultQueue.size() == MAX_QUEUE) {
            int mostConfidenceLabel = 0;
            int mostConfideneceCount = resultCounter.get(0).getCount();
            for (int i = 0; i < resultCounter.size(); i++) {
                int currentCount = resultCounter.get(i).getCount();
                if (mostConfideneceCount < currentCount) {
                    mostConfidenceLabel = i;
                    mostConfideneceCount = currentCount;
                }
            }

            Logly.i("mostConfidenceLabel=" + mostConfidenceLabel + ", "
                    + IMAGENET_CLASSES[mostConfidenceLabel]);
            float percent = (float) mostConfideneceCount / MAX_QUEUE;
            Logly.i( "percent=" + percent);

            if (percent > 0.8f) {
                Log.d(TAG, "show toast");
                String mostConfidenece = IMAGENET_CLASSES[mostConfidenceLabel]
                        + "(" + mostConfidenceLabel + ")" + ":"
                        + mostConfideneceCount;
                Toast.makeText(this, mostConfidenece, Toast.LENGTH_SHORT).show();
                PictureManager pictureManager = TvManager.getInstance().getPictureManager(getApplicationContext());
                pictureManager.setPictureMode(PICTURE_MODE_MAPPER.get(mostConfidenceLabel));
            }

            // remove head result, and substract 1
            int headLabel = resultQueue.poll();
            CaffeResult headResult = new CaffeResult(headLabel);
            int headIndex = resultCounter.indexOf(headResult);
            resultCounter.get(headIndex).setCount(resultCounter.get(headIndex).getCount() - 1);
        }
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
                    capturerA = new CapturerA();
                    boolean result = capturerA.isCaptureSupported(getApplicationContext());
                    Log.i(TAG, "captureA can capture screen?" + result);

                    for (int i = 0; i < 6; i++) {
                        resultCounter.add(new CaffeResult(i));
                    }

                    /*
                         {SetPictureModePreview} ePictureMode = 0 biao zhun
                         {SetPictureModePreview} ePictureMode = 1 xian yan
                         {SetPictureModePreview} ePictureMode = 2 zi ran
                         {SetPictureModePreview} ePictureMode = 19 ti yu
                         {SetPictureModePreview} ePictureMode = 4  dian ying
                         {SetPictureModePreview} ePictureMode = 5  you xi
                     */
                    // Cartoon-------xian yan
                    PICTURE_MODE_MAPPER.put(0, EnumConstants.PictureMode.TIL_PICTURE_MODE_STADIUM);
                    // Entertain-----biao zhun
                    PICTURE_MODE_MAPPER.put(1, EnumConstants.PictureMode.TIL_PICTURE_MODE_STANDARD);
                    // Game----------biao zhun
                    PICTURE_MODE_MAPPER.put(2, EnumConstants.PictureMode.TIL_PICTURE_MODE_STANDARD);
                    // Movie---------dian ying
                    PICTURE_MODE_MAPPER.put(3, EnumConstants.PictureMode.TIL_PICTURE_MODE_CONCERT);
                    // News----------zi ran
                    PICTURE_MODE_MAPPER.put(4, EnumConstants.PictureMode.TIL_PICTURE_MODE_GAME);
                    // Sport---------ti yu
                    PICTURE_MODE_MAPPER.put(5, EnumConstants.PictureMode.TIL_PICTURE_MODE_HDR_MOVIE);
                    break;
                }
                case MSG_EXECUTE: {
                    Log.i(TAG, "CNNTask executed");

                    while (!shouldStop) {
                        Bitmap screen = capturerA.getScreenCapture(getApplicationContext(), 256, 256);
                        String fileFullPath = saveBitmapFile(screen);
                        CNNTask cnnTask = new CNNTask(CaffeInferenceService.this);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
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
//            Log.d(TAG, "strings=" + strings[0]);
            return caffeMobile.predictImage(strings[0])[0];
        }

        @Override
        protected void onPostExecute(Integer integer) {
            String result = IMAGENET_CLASSES[integer];
            Log.i(TAG, String.format("elapsed time %d ms, label is %s",
                    SystemClock.uptimeMillis() - startTime, result));

            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
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
