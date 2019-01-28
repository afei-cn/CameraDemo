package com.afei.camerademo.textureview;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;

import com.afei.camerademo.camera.Camera2Proxy;

public class Camera2TextureView extends TextureView {

    private static final String TAG = "CameraTextureView";
    private Camera2Proxy mCameraProxy;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private float mOldDistance;

    public Camera2TextureView(Context context) {
        this(context, null);
    }

    public Camera2TextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        setSurfaceTextureListener(mSurfaceTextureListener);
        mCameraProxy = new Camera2Proxy((Activity) context);
    }

    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.v(TAG, "onSurfaceTextureAvailable. width: " + width + ", height: " + height);
            mCameraProxy.setPreviewSurface(surface);
            mCameraProxy.openCamera(width, height);
            // resize TextureView
            int previewWidth = mCameraProxy.getPreviewSize().getWidth();
            int previewHeight = mCameraProxy.getPreviewSize().getHeight();
            if (width > height) {
                setAspectRatio(previewWidth, previewHeight);
            } else {
                setAspectRatio(previewHeight, previewWidth);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.v(TAG, "onSurfaceTextureSizeChanged. width: " + width + ", height: " + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.v(TAG, "onSurfaceTextureDestroyed");
            mCameraProxy.releaseCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private void setAspectRatio(int width, int height) {
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            mCameraProxy.focusOnPoint((int) event.getX(), (int) event.getY(), getWidth(), getHeight());
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDistance = getFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDistance = getFingerSpacing(event);
                if (newDistance > mOldDistance) {
                    mCameraProxy.handleZoom(true);
                } else if (newDistance < mOldDistance) {
                    mCameraProxy.handleZoom(false);
                }
                mOldDistance = newDistance;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

}
