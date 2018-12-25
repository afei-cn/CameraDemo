package com.afei.camerademo;

import android.hardware.Camera;
import android.support.v4.app.Fragment;

import com.afei.camerademo.camera.CameraProxy;
import com.afei.camerademo.glsurfaceview.GLSurfaceCameraFragment;
import com.afei.camerademo.surfaceview.SurfaceCameraFragment;
import com.afei.camerademo.textureview.TextureCameraFragment;

public abstract class CameraFragment extends Fragment {

    protected final String TAG = this.getClass().getSimpleName();

    protected CameraProxy mCameraProxy; // camera controller

    public static CameraFragment newInstance(int type) {
        CameraFragment cameraFragment = null;
        switch (type) {
            case CameraType.TYPE_SURFACEVIEW_CAMERA:
                cameraFragment = SurfaceCameraFragment.newInstance();
                break;
            case CameraType.TYPE_TEXTUREVIEW_CAMERA:
                cameraFragment = TextureCameraFragment.newInstance();
                break;
            case CameraType.TYPE_GLSURFACEVIEW_CAMERA:
                cameraFragment = GLSurfaceCameraFragment.newInstance();
                break;
        }
        return cameraFragment;
    }

    public CameraProxy getCameraProxy() {
        return mCameraProxy;
    }

    public abstract void startPreview();

    public void switchCamera() {
        mCameraProxy.switchCamera();
        startPreview();
    }

    public void takePicture(Camera.PictureCallback pictureCallback) {
        mCameraProxy.takePicture(pictureCallback);
    }
}
