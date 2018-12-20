package com.afei.camerademo.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraProxy implements Camera.AutoFocusCallback {

    private static final String TAG = "CameraProxy";

    private Activity mActivity;
    private Camera mCamera;
    private CameraInfo mCameraInfo = new CameraInfo();
    private int mCameraId = CameraInfo.CAMERA_FACING_BACK;
    private int mPreviewWidth = 1440; // default 1440
    private int mPreviewHeight = 1080; // default 1080
    private float mPreviewScale = mPreviewHeight * 1f / mPreviewWidth;
    private PreviewCallback mPreviewCallback; // 相机预览的数据回调

    public byte[] mPreviewBuffer;

    public CameraProxy(Activity activity) {
        mActivity = activity;
    }

    public void openCamera() {
        Log.d(TAG, "openCamera cameraId: " + mCameraId);
        mCamera = Camera.open(mCameraId);
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        initConfig();
        setDisplayOrientation();
    }

    public void releaseCamera() {
        if (mCamera != null) {
            Log.v(TAG, "releaseCamera");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void startPreview(SurfaceHolder holder) {
        if (mCamera != null) {
            Log.v(TAG, "startPreview");
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    public void startPreview(SurfaceTexture surface) {
        if (mCamera != null) {
            Log.v(TAG, "startPreview");
            try {
                mCamera.setPreviewTexture(surface);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            Log.v(TAG, "stopPreview");
            mCamera.stopPreview();
        }
    }

    private void initConfig() {
        Log.v(TAG, "initConfig");
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
            parameters.setExposureCompensation(0); // 设置曝光强度
            Size previewSize = getSuitableSize(parameters.getSupportedPreviewSizes());
            mPreviewWidth = previewSize.width;
            mPreviewHeight = previewSize.height;
            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight); // 设置预览图片大小
            Log.d(TAG, "previewWidth: " + mPreviewWidth + ", previewHeight: " + mPreviewHeight);
            Size pictureSize = getSuitableSize(parameters.getSupportedPictureSizes());
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            Log.d(TAG, "pictureWidth: " + pictureSize.width + ", pictureHeight: " + pictureSize.height);
            mCamera.setParameters(parameters); // 将设置好的parameters添加到相机里
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Size getSuitableSize(List<Size> sizes) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.size(); i++) {
            Size previewSize = sizes.get(i);
            Log.v(TAG, "SupportedPreviewSize, width: " + previewSize.width + ", height: " + previewSize.height);
            // 找到一个与设置的分辨率差值最小的相机支持的分辨率大小
            if (previewSize.width * mPreviewScale == previewSize.height) {
                int delta = Math.abs(mPreviewWidth - previewSize.width);
                if (delta == 0) {
                    return previewSize;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes.get(index); // 默认返回与设置的分辨率最接近的预览尺寸
    }

    /**
     * 设置相机显示的方向，必须设置，否则显示的图像方向会错误
     */
    private void setDisplayOrientation() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    public void setPreviewCallback(PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
        if (mPreviewBuffer == null) {
            mPreviewBuffer = new byte[mPreviewWidth * mPreviewHeight * 3 / 2];
        }
        mCamera.addCallbackBuffer(mPreviewBuffer);
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback); // 设置预览的回调
    }

    public void takePicture(Camera.PictureCallback pictureCallback) {
        mCamera.takePicture(null, null, pictureCallback);
    }

    public void switchCamera() {
        mCameraId ^= 1; // 先改变摄像头朝向
        releaseCamera();
        openCamera();
    }

    public void focusOnPoint(int x, int y, int width, int height) {
        Log.v(TAG, "touch point (" + x + ", " + y + ")");
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        // 1.先要判断是否支持设置聚焦区域
        if (parameters.getMaxNumFocusAreas() > 0) {
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
            Log.d(TAG, "focus area (" + left + ", " + top + ", " + right + ", " + bottom + ")");
            ArrayList<Camera.Area> areas = new ArrayList<>();
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

    public Camera getCamera() {
        return mCamera;
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "onAutoFocus: " + success);
    }
}
