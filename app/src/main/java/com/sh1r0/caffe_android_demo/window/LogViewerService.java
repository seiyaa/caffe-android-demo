package com.sh1r0.caffe_android_demo.window;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sh1r0.caffe_android_demo.R;
import com.sh1r0.caffe_android_demo.Util;

import java.util.LinkedList;

/**
 * Created by jiangjunhou on 2018-1-29.
 */


public class LogViewerService extends Service {

    public static final String TAG = LogViewerService.class.getSimpleName();
    public static final int MSG_MESSAGE_SHOW = 1;
    public static final LinkedList<Message> mMessages = new LinkedList<>();
    private static int INIT_X = 10;
    private static int INIT_Y = 650;

    private static TextView mTextView = null;
    private static ScrollView mScrollView = null;
    private static int SLEEP_INTERVAL = 200;

    private static Handler mMessageHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_MESSAGE_SHOW:
                    final String time = Util.getShortTime(System.currentTimeMillis());
                    mTextView.append(time + ": " + ((String) msg.obj) + "\n");
                    mScrollView.fullScroll(android.view.View.FOCUS_DOWN);
                    break;
                default:
                    break;
            }
        }
    };

    private View mLayout = null;
    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mLayoutParams;
    private Display mDisplay = null;
    private DisplayMetrics mDisplaymetrics = new DisplayMetrics();
    private MessagesChecker mMsgChecker = null;

    public static synchronized void addMessage(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            mMessages.add(mMessageHandler.obtainMessage(MSG_MESSAGE_SHOW, msg));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        init();
    }

    private void init() {
        mLayout = LayoutInflater.from(getApplicationContext()).inflate(R.layout.window_logviewer, null);

        mTextView = (TextView) mLayout.findViewById(R.id.tv);
        mScrollView = (ScrollView) mLayout.findViewById(R.id.scrollView);

        //set custom width and height and alpha
        mLayoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, 0, 0, PixelFormat.RGBA_8888);

        //set default parameters
        updateDate(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, Gravity.LEFT | Gravity.TOP,
                INIT_X, INIT_Y);

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(mLayout, mLayoutParams);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplay.getMetrics(mDisplaymetrics);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mMsgChecker == null) {
            mMsgChecker = new MessagesChecker();
            Thread t1 = new Thread(mMsgChecker);
            t1.setName(TAG);
            t1.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateDate(int type, int flag, int gravity, int x, int y) {
        mLayoutParams.type = type;
        mLayoutParams.flags = flag;
        mLayoutParams.gravity = gravity;
        mLayoutParams.x = x;
        mLayoutParams.y = y;
    }

    @Override
    public void onDestroy() {
        if (mMsgChecker != null) {
            mMsgChecker.setStop(true);
        }
        super.onDestroy();
    }

    private class MessagesChecker implements Runnable {

        private boolean isStop = false;

        @Override
        public void run() {
            //do check
            while (!isStop) {
                if (mMessages.size() > 0) {
                    Message message = mMessages.pollFirst();
                    if (message != null) {
                        try {
                            Thread.sleep(SLEEP_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        message.sendToTarget();
                    }
                }
            }
        }

        public void setStop(boolean isStop) {
            this.isStop = isStop;
        }
    }
}
