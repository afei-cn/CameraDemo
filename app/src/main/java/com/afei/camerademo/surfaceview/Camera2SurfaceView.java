package com.afei.camerademo.surfaceview;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.afei.camerademo.camera.Camera2Proxy;

public class Camera2SurfaceView extends SurfaceView {

    private Camera2Proxy mCameraProxy;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public Camera2SurfaceView(Context context) {
        this(context, null);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(mSurfaceHolderCallback);
        mCameraProxy = new Camera2Proxy((Activity) context);
    }

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mCameraProxy.setPreviewSurface(holder);
            mCameraProxy.openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            int previewWidth = mCameraProxy.getPreviewSize().getWidth();
            int previewHeight = mCameraProxy.getPreviewSize().getHeight();
            if (width > height) {
                setAspectRatio(previewWidth, previewHeight);
            } else {
                setAspectRatio(previewHeight, previewWidth);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCameraProxy.releaseCamera();
        }
    };

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    public Camera2Proxy getCameraProxy() {
        return mCameraProxy;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

}
