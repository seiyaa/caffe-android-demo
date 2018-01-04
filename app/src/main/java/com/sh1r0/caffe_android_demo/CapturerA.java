package com.sh1r0.caffe_android_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>
 * The capture solution is provided by SurfaceControl
 * to invoke the HAL module interfaces
 */
public class CapturerA {
    private static final String TAG = "CapturerA";
    private static final String PROPERTY_SCREEN_120HZ = "ro.his.tv.screen.is120HZ";

    private static final String CLASS_SURFACE_SESSION = "android.view.SurfaceSession";
    private static final String CLASS_SURFACE_CONTROL = "android.view.SurfaceControl";
    private static final String METHOD_NAME_SCREENSHOT = "screenshot";

    private DisplayMetrics mDisplayMetrics;

    //SurfaceSession
    private Class mSurfaceSessionClass;
    private Constructor<?> mSurfaceSessionConstructor;
    private Object mSurfaceSessionInstance;
    private static final int HIDDEN = 0x00000004;

    private Class mSurfaceControlClass;
    private Object mSurfaceControlInstance;
    private Method mSurfaceControlMethodScreenshot;
    private Constructor<?> mSurfaceControlConstructor;

    // for one capture action
    private Object mLock = new Object();

    public CapturerA() {
        super();
        mDisplayMetrics = new DisplayMetrics();
    }

    public Bitmap getScreenCapture(Context context, int width, int height) {
        if (!isCaptureSupported(context)) {
            Log.w(TAG, "getScreenCapture(), Capture is not supported.");
            return null;
        }

        synchronized (mLock) {
            if (mSurfaceControlInstance == null || mSurfaceControlMethodScreenshot == null) {
                Log.w(TAG, "getScreenCapture(), should never happened here with NULL objects.");
                return null;
            }

            try {
                // invoke SurfaceControl's static method: screenshot
                return (Bitmap) mSurfaceControlMethodScreenshot.invoke(null, width, height);
            } catch (IllegalAccessException e) {
                Log.w(TAG, "getScreenCapture(), IllegalAccessException = " + e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, "getScreenCapture(), InvocationTargetException = " + e);
            }

            return null;
        }
    }

    public int getCurrentVideoStatus(Context context) {
        return -1;
    }

    public boolean isCaptureSupported(Context context) {
        boolean res = false;

        try {
            if (null == mSurfaceSessionClass) {
                mSurfaceSessionClass = Class.forName(CLASS_SURFACE_SESSION);
                mSurfaceSessionConstructor = mSurfaceSessionClass.getDeclaredConstructor();
                mSurfaceSessionInstance = mSurfaceSessionConstructor.newInstance();
            }

            if (null == mSurfaceControlClass) {
                mSurfaceControlClass = Class.forName(CLASS_SURFACE_CONTROL);
//                Constructor[] constructors = mSurfaceControlClass.getDeclaredConstructors();
//                for(Constructor c:constructors) {
//                    Log.i(TAG, c.toString());
//                }
                mSurfaceControlConstructor = mSurfaceControlClass
                        .getDeclaredConstructor(mSurfaceSessionClass, String.class,
                                int.class, int.class, int.class, int.class);
                mSurfaceControlConstructor.setAccessible(true);
                mSurfaceControlInstance = mSurfaceControlConstructor
                        .newInstance(mSurfaceSessionInstance, "ScreenCapturer",
//                                mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels,
                                1280, 720,
                                PixelFormat.RGB_565,
                                HIDDEN
                        );
            }

            // static method
            if (mSurfaceControlMethodScreenshot == null && mSurfaceControlClass != null) {
                mSurfaceControlMethodScreenshot = mSurfaceControlClass.getMethod(
                        METHOD_NAME_SCREENSHOT, int.class, int.class);
            }

            if (mSurfaceControlInstance != null && mSurfaceControlMethodScreenshot != null) {
                // reflect success
                res = true;
            }
        } catch (ClassNotFoundException e) {
            Log.w(TAG, Log.getStackTraceString(e));
        } catch (NoSuchMethodException e) {
            Log.w(TAG, Log.getStackTraceString(e));
        } catch (InstantiationException e) {
            Log.w(TAG, Log.getStackTraceString(e));
        } finally {
            Log.i(TAG, "isCaptureSupported(), res = " + res);
            return res;
        }
    }
}


