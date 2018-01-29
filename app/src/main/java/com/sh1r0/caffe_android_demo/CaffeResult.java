package com.sh1r0.caffe_android_demo;

/**
 * Created by jiangjunhou on 2018-1-25.
 */

public class CaffeResult {

    private int label;
    private int count = 0;

    public CaffeResult(int label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CaffeResult that = (CaffeResult) o;

        return label == that.label;
    }

    @Override
    public int hashCode() {
        return label;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "CaffeResult{" +
                "label=" + label +
                ", count=" + count +
                '}';
    }
}
