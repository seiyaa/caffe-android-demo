package com.sh1r0.caffe_android_demo;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jiangjunhou on 2018-1-29.
 */

public class Util {
    public static String getShortTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return format.format(new Date(time));
    }
}
