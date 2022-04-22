package com.afei.camerademo.surfaceview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.afei.camerademo.camera.Camera2Proxy;

public class Camera2SurfaceView extends SurfaceView {

    private static final String TAG = "Camera2SurfaceView";

    private Camera2Proxy mCameraProxy;
    private Size mPreviewSize;

    private GestureDetector mGestureDetector;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private float mOldDistance;

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
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) { // 单击聚焦
                mCameraProxy.triggerFocusAtPoint(e.getX(), e.getY(), getWidth(), getHeight());
                return true;
            }
        });
        setKeepScreenOn(true); // 设置屏幕常亮
    }

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Rect rect = holder.getSurfaceFrame();
            mCameraProxy.setUpCameraOutputs(rect.width(), rect.height());
            mPreviewSize = mCameraProxy.getPreviewSize();
            holder.setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged: width: " + width + ", height: " + height);
            float ratio;
            if (width > height) {
                ratio = height * 1.0f / width;
            } else {
                ratio = width * 1.0f / height;
            }
            setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth()); //固定竖屏显示
            if (ratio == mPreviewSize.getHeight() * 1f / mPreviewSize.getWidth()) {
                mCameraProxy.setPreviewSurface(holder); // 等view的大小固定后再设置surface
                mCameraProxy.openCamera();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCameraProxy.releaseCamera();
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
            mCameraProxy.triggerFocusAtPoint(event.getX(), event.getY(), getWidth(), getHeight());
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
