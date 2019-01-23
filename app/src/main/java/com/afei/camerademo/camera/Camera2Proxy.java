package com.afei.camerademo.camera;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class Camera2Proxy {

    private static final String TAG = "Camera2Proxy";

    private Activity mActivity;

    private int mCameraId = CameraCharacteristics.LENS_FACING_FRONT; // 要打开的摄像头ID
    private Size mPreviewSize; // 预览大小

    private CameraManager mCameraManager; // 相机管理者
    private CameraCharacteristics mCameraCharacteristics; // 相机属性
    private CameraDevice mCameraDevice; // 相机对象
    private CaptureRequest.Builder mPreviewRequestBuilder; // 相机预览请求的构造器
    private CameraCaptureSession mCaptureSession;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageReader mImageReader;
    private Surface mPreviewSurface;
    private OrientationEventListener mOrientationEventListener;
    private int mLatestRotation = 0;

    /**
     * 打开摄像头的回调
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened");
            mCameraDevice = camera;
            initPreviewRequestBuilder();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected");
            releaseCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera Open failed, error: " + error);
            releaseCamera();
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    public Camera2Proxy(Activity activity) {
        mActivity = activity;
        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        mOrientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                mLatestRotation = orientation;
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void openCamera(int width, int height) {
        Log.v(TAG, "openCamera");
        startBackgroundThread(); // 对应 releaseCamera() 方法中的 stopBackgroundThread()
        mOrientationEventListener.enable();
        try {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(Integer.toString(mCameraId));
            StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics
                    .SCALER_STREAM_CONFIGURATION_MAP);
            // 拍照大小，选择能支持的一个最大的图片大小
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            Log.d(TAG, "picture size: " + largest.getWidth() + "*" + largest.getHeight());
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            // 预览大小，根据上面选择的拍照图片的长宽比，选择一个和控件长宽差不多的大小
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
            Log.d(TAG, "preview size: " + mPreviewSize.getWidth() + "*" + mPreviewSize.getHeight());
            // 打开摄像头
            mCameraManager.openCamera(Integer.toString(mCameraId), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void releaseCamera() {
        Log.v(TAG, "releaseCamera");
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mOrientationEventListener.disable();
        stopBackgroundThread(); // 对应 openCamera() 方法中的 startBackgroundThread()
    }

    public void setImageAvailableListener(ImageReader.OnImageAvailableListener onImageAvailableListener) {
        if (mImageReader == null) {
            Log.w(TAG, "setImageAvailableListener: mImageReader is null");
            return;
        }
        mImageReader.setOnImageAvailableListener(onImageAvailableListener, null);
    }

    public void setPreviewSurface(SurfaceHolder holder) {
        mPreviewSurface = holder.getSurface();
    }

    public void setPreviewSurface(SurfaceTexture surfaceTexture) {
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);
        mPreviewSurface = surface;
    }

    private void initPreviewRequestBuilder() {
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mPreviewSurface); // 设置预览输出的 Surface
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            // 设置连续自动对焦
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest
                                    .CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // 设置关闭闪光灯
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE_OFF);
                            // 设置完后自动开始预览
                            startPreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "ConfigureFailed. session: mCaptureSession");
                        }
                    }, mBackgroundHandler); // handle 传入 null 表示使用当前线程的 Looper
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startPreview() {
        Log.v(TAG, "startPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "startPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            // 开始预览，即一直发送预览的请求
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        Log.v(TAG, "stopPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "stopPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void captureStillPicture() {
        try {
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice
                    .TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(mLatestRotation));
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    startPreview();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getJpegOrientation(int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
        Log.d(TAG, "jpegOrientation: " + jpegOrientation);
        return jpegOrientation;
    }

    public boolean isFrontCamera() {
        return mCameraId == CameraCharacteristics.LENS_FACING_BACK;
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public void switchCamera(int width, int height) {
        mCameraId ^= 1;
        Log.d(TAG, "switchCamera: mCameraId: " + mCameraId);
        releaseCamera();
        openCamera(width, height);
    }

    private Size chooseOptimalSize(Size[] sizes, int viewWidth, int viewHeight, Size pictureSize) {
        int totalRotation = getRotation();
        boolean swapRotation = totalRotation == 90 || totalRotation == 270;
        int width = swapRotation ? viewHeight : viewWidth;
        int height = swapRotation ? viewWidth : viewHeight;
        return getSuitableSize(sizes, width, height, pictureSize);
    }

    private int getRotation() {
        int displayRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        switch (displayRotation) {
            case Surface.ROTATION_0:
                displayRotation = 90;
                break;
            case Surface.ROTATION_90:
                displayRotation = 0;
                break;
            case Surface.ROTATION_180:
                displayRotation = 270;
                break;
            case Surface.ROTATION_270:
                displayRotation = 180;
                break;
        }
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return (displayRotation + sensorOrientation + 270) % 360;
    }

    private Size getSuitableSize(Size[] sizes, int width, int height, Size pictureSize) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        float aspectRatio = pictureSize.getHeight() / pictureSize.getWidth();
        for (int i = 0; i < sizes.length; i++) {
            Size size = sizes[i];
            // 先判断比例是否相等
            if (size.getWidth() * aspectRatio == size.getHeight()) {
                int delta = Math.abs(width - size.getWidth());
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes[index];
    }

    private void startBackgroundThread() {
        if (mBackgroundThread == null || mBackgroundHandler == null) {
            Log.v(TAG, "startBackgroundThread");
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        Log.v(TAG, "stopBackgroundThread");
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}
