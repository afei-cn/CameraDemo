package com.afei.camerademo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * 相机View
 */
public class CameraTextureView extends TextureView implements Camera.AutoFocusCallback {

    private final static String TAG = "CameraTextureView";
    private final static boolean DEBUG = true;
    private final static int SCALE_TYPE_4_3 = 1; // 自定义属性中4:3比例的枚举对应的值为1
    private final static int SCALE_TYPE_16_9 = 2; // 自定义属性中16:9比例的枚举对应的值为2

    private Camera mCamera; // 相机对象
    private Matrix matrix = new Matrix(); // 记录屏幕拉伸的矩阵，用于绘制人脸框使用
    private PreviewCallback mPreviewCallback; // 相机预览的数据回调

    private float mPreviewScale; // 预览显示的比例(4:3/16:9)
    private int mResolution; // 分辨率大小，以预览高度为标准(320, 480, 720, 1080...)
    private int mCameraFacing; // 摄像头方向

    public int mPreviewWidth; // 预览宽度
    public int mPreviewHeight; // 预览高度
    public int mDegrees; // 预览显示的角度
    public byte[] mBuffer; // 预览缓冲数据，使用可以让底层减少重复创建byte[]，起到重用的作用
    private float scaleX;
    private float scaleY;

    public CameraTextureView(Context context) {
        super(context);
        init(context, null);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // 设置屏幕常亮
        setKeepScreenOn(true);
        // 自定义属性
        // TypedArray a = context.obtainStyledAttributes(attrs,
        // R.styleable.CameraView);
        mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        mResolution = 480;
        int scaleType = 1;
        mPreviewScale = getPreviewScale(scaleType);
        // a.recycle();
        this.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            releaseCamera(); // 5.释放相机资源
            return false;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                openCamera(mCameraFacing); // 1.打开相机
                initParameters(); // 2.设置相机参数
                mCamera.setPreviewTexture(surface);
                updateCamera(); // 4.更新相机属性，每次更换分辨率需要更新的操作，包括设置预览大小和方向，开始预览
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "打开相机失败", Toast.LENGTH_SHORT).show();
            }
            Matrix mirrorMatrix = new Matrix();
            mirrorMatrix.setScale(-1, 1, getWidth() / 2, 0);
            CameraTextureView.this.setTransform(mirrorMatrix);

            if (getWidth() > getHeight()) {
                scaleX = getWidth() / (float) mPreviewWidth;
                scaleY = getHeight() / (float) mPreviewHeight;
            } else {
                scaleX = getWidth() / (float) mPreviewHeight;
                scaleY = getHeight() / (float) mPreviewWidth;
            }
            matrix.setScale(scaleX, scaleY);
            matrix.postScale(-1, 1, getWidth() / 2, 0);
        }

    };

    private void openCamera(int mCameraFacing) throws RuntimeException {
        releaseCamera();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == mCameraFacing) {
                mCamera = Camera.open(i); // 打开对应的摄像头，获取到camera实例
                return;
            }
        }
    }

    private void initParameters() {
        if (mCamera == null) {
            return;
        }
        try {
            Parameters parameters = mCamera.getParameters();
            // 如果摄像头不支持这些参数都会出错的，所以设置的时候一定要判断是否支持
            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes != null && supportedFlashModes.contains(Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF); // 设置闪光模式
            }
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO); // 设置聚焦模式
            }
            parameters.setPreviewFormat(ImageFormat.NV21); // 设置预览图片格式
            parameters.setPictureFormat(ImageFormat.JPEG); // 设置拍照图片格式
            mCamera.setParameters(parameters); // 将设置好的parameters添加到相机里
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 每次启动摄像头、切换分辨率都需要进行的操作，所以抽离出来作为一个单独的方法
     */
    private void updateCamera() {
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview(); // 1.先停止预览
        setCameraDisplayOrientation((Activity) getContext(), mCamera); // 2.设置相机的显示方向
        initPreviewSize(); // 3.初始化相机预览尺寸
        initPreviewBuffer(); // 4.初始化相机预览的缓存
        mCamera.startPreview(); // 5.开始预览
    }

    /**
     * 初始化预览尺寸大小并设置，根据拉伸比例、分辨率来计算
     */
    private void initPreviewSize() {
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        Size fitPreviewSize = getFitPreviewSize(parameters); // 获取适合的预览大小
        mPreviewWidth = fitPreviewSize.width;
        mPreviewHeight = fitPreviewSize.height;
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight); // 设置预览图片大小
        if (DEBUG) {
            Log.d(TAG, "initPreviewSize() mPreviewWidth: " + mPreviewWidth + ", mPreviewHeight: " + mPreviewHeight);
        }
        mCamera.setParameters(parameters);
    }

    /**
     * 具体计算最佳分辨率大小的方法
     */
    private Size getFitPreviewSize(Parameters parameters) {
        List<Size> previewSizes = parameters.getSupportedPreviewSizes(); // 获取支持的预览尺寸大小
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < previewSizes.size(); i++) {
            Size previewSize = previewSizes.get(i);
            if (DEBUG) {
                Log.d(TAG, "SupportedPreviewSize, width: " + previewSize.width + ", height: " + previewSize.height);
            }
            // 找到一个与设置的分辨率差值最小的相机支持的分辨率大小
            if (previewSize.width * mPreviewScale == previewSize.height) {
                int delta = Math.abs(mResolution - previewSize.height);
                if (delta == 0) {
                    return previewSize;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return previewSizes.get(index); // 默认返回与设置的分辨率最接近的预览尺寸
    }

    private void initPreviewBuffer() {
        if (mCamera == null) {
            return;
        }
        mBuffer = new byte[mPreviewWidth * mPreviewHeight * 3 / 2]; // 初始化预览缓冲数据的大小
        if (DEBUG) {
            Log.d(TAG, "initPreviewBuffer() mBuffer.length: " + mBuffer.length);
        }
        mCamera.addCallbackBuffer(mBuffer); // 将此预览缓冲数据添加到相机预览缓冲数据队列里
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback); // 设置预览的回调
    }

    /**
     * 设置相机显示的方向，必须设置，否则显示的图像方向会错误
     */
    private void setCameraDisplayOrientation(Activity activity, Camera camera) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: // portrait
                mDegrees = 90;
                break;
            case Surface.ROTATION_90: // landscape
                mDegrees = 0;
                break;
            case Surface.ROTATION_180: // portrait-reverse
                mDegrees = 270;
                break;
            case Surface.ROTATION_270: // landscape-reverse
                mDegrees = 180;
                break;
            default:
                mDegrees = 180;
                break;
        }

        camera.setDisplayOrientation(mDegrees);
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (null != mCamera) {
            if (DEBUG) {
                Log.v(TAG, "releaseCamera()");
            }
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 根据自定义属性的模式（4:3模式,16:9模式,auto模式）来获取相机的显示比例
     */
    private float getPreviewScale(int type) {
        if (type == SCALE_TYPE_4_3) { // 4:3模式
            return 0.75f;
        }
        if (type == SCALE_TYPE_16_9) { // 16:9模式
            return 0.5625f;
        }
        return getScreenScale(); // auto模式
    }

    /**
     * 获取设备屏幕的拉伸比例，目前安卓的设备屏幕比例只有4：3和16：9两种
     */
    private float getScreenScale() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        float width = displayMetrics.widthPixels;
        float height = displayMetrics.heightPixels;
        float scale;
        if (width > height) {
            scale = height / width;
        } else {
            scale = width / height;
        }
        if (DEBUG) {
            Log.d(TAG, "displayMetrics.widthPixels : " + width);
            Log.d(TAG, "displayMetrics.heightPixels : " + height);
            Log.d(TAG, "scale : " + scale);
        }
        return Math.abs(scale - 0.75f) > Math.abs(scale - 0.5625f) ? 0.5625f : 0.75f; // 0.75(4:3)
        // 或者
        // 0.5625(16:9)
    }

    /**
     * 以点击的坐标点（基于CameraView控件大小的坐标系）为中心进行聚焦
     */
    private void focusOnPoint(int x, int y) {
        if (DEBUG) {
            Log.d(TAG, "touch point (" + x + ", " + y + ")");
        }
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        // 1.先要判断是否支持设置聚焦区域
        if (parameters.getMaxNumFocusAreas() > 0) {
            int width = getWidth();
            int height = getHeight();
            // 2.以触摸点为中心点，view窄边的1/4为聚焦区域的默认边长
            int length = Math.min(width, height) >> 3; // 1/8的长度
            int left = x - length;
            int top = y - length;
            int right = x + length;
            int bottom = y + length;
            // 3.映射，因为相机聚焦的区域是一个(-1000,-1000)到(1000,1000)的坐标区域
            left = left * 2000 / width - 1000;
            top = top * 2000 / height - 1000;
            right = right * 2000 / width - 1000;
            bottom = bottom * 2000 / height - 1000;
            // 4.判断上述矩形区域是否超过边界，若超过则设置为临界值
            left = left < -1000 ? -1000 : left;
            top = top < -1000 ? -1000 : top;
            right = right > 1000 ? 1000 : right;
            bottom = bottom > 1000 ? 1000 : bottom;
            if (DEBUG) {
                Log.d(TAG, "focus area (" + left + ", " + top + ", " + right + ", " + bottom + ")");
            }
            ArrayList<Camera.Area> areas = new ArrayList<Camera.Area>();
            areas.add(new Camera.Area(new Rect(left, top, right, bottom), 600));
            parameters.setFocusAreas(areas);
        }
        try {
            mCamera.cancelAutoFocus(); // 先要取消掉进程中所有的聚焦功能
            mCamera.setParameters(parameters);
            mCamera.autoFocus(this); // 调用聚焦
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPreviewCallback(PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    /**
     * 每次预览的回调中，需要调用这个方法才可以起到重用mBuffer
     */
    public void addCallbackBuffer() {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(mBuffer);
        }
    }

    /**
     * 切换前后摄像头
     */
    public void switchCamera() {
        mCameraFacing ^= 1; // 先改变摄像头朝向
        openCamera(mCameraFacing); // 重新打开对应的摄像头
        try {
            initParameters(); // 重新初始化参数
            mCamera.setPreviewTexture(this.getSurfaceTexture());
            updateCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换分辨率
     */
    public void resetResolution(Size size) {
        float scale = size.height * 1.0f / size.width;
        if (mPreviewScale == scale && mResolution == size.height) {
            return; // 比例分辨率均为改变，直接返回
        }
        mResolution = size.height; // 重置分辨率大小
        if (mPreviewScale == scale) {
            // 比例未改变，只需要重新更新预览分辨率即可
            updateCamera();
            resetMatrix();
        } else {
            // 比例改变，因为View的大小将要变化了，需要重新布局
            mPreviewScale = scale;
            requestLayout();
        }
    }

    /**
     * 比例未改变时，surfaceView的大小并没有改变，只是相机的预览分辨率改变了，不会调用到surfaceChanged()方法，
     * 所以就需要手动重置matrix
     */
    private void resetMatrix() {
        int width = getWidth();
        int height = getHeight();
        if (DEBUG) {
            Log.d(TAG, "resetMatrix() width: " + width + ", height: " + height);
        }
        if (width > height) {
            matrix.setScale(width / (float) mPreviewWidth, height / (float) mPreviewHeight);
        } else {
            matrix.setScale(width / (float) mPreviewHeight, height / (float) mPreviewWidth);
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    public List<Size> getSupportPreviewSize() {
        if (mCamera == null) {
            return null;
        }
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public boolean isFrontCamera() {
        return mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public void startPreview() {
        if (mCamera != null) {
            if (DEBUG) {
                Log.d(TAG, "startPreview()");
            }
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            if (DEBUG) {
                Log.d(TAG, "stopPreview()");
            }
            mCamera.stopPreview();
        }
    }

    /**
     * 设置长按可切换前后摄像头
     */
    public void setLongClickSwitchCamera() {
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switchCamera();
                return true;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
        float scale; // 宽高比，用 较小数/较大数
        int finalWidth, finalHeight; // 根据预览的比例去重新计算和设置View的宽高
        if (originalWidth < originalHeight) {
            scale = originalWidth * 1.0f / originalHeight;
            if (scale == mPreviewScale) { // 比例一样则不改变
                finalWidth = originalWidth;
                finalHeight = originalHeight;
            } else {
                if (mPreviewScale == 0.75f) { // 预览比例4:3,压缩高度
                    finalWidth = originalWidth;
                    finalHeight = finalWidth * 4 / 3;
                } else { // 预览比例16:9,压缩宽度
                    finalHeight = originalHeight;
                    finalWidth = finalHeight * 10 / 16;
                }
            }
        } else {
            scale = originalHeight * 1.0f / originalWidth;
            if (scale == mPreviewScale) { // 比例一样则不改变
                finalWidth = originalWidth;
                finalHeight = originalHeight;
            } else {
                if (mPreviewScale == 0.75f) { // 预览比例4:3,压缩宽度
                    finalHeight = originalHeight;
                    finalWidth = finalHeight * 4 / 3;
                } else { // 预览比例16:9,压缩高度
                    finalWidth = originalWidth;
                    finalHeight = finalWidth * 10 / 16;
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "originalWidth : " + originalWidth);
            Log.d(TAG, "originalHeight : " + originalHeight);
            Log.d(TAG, "finalWidth : " + finalWidth);
            Log.d(TAG, "finalHeight : " + finalHeight);
        }
        setMeasuredDimension(finalWidth, finalHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                focusOnPoint((int) event.getX(), (int) event.getY()); // 点击聚焦
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (DEBUG) {
            Log.d(TAG, "onAutoFocus : " + success);
        }
    }
}
