package com.afei.camerademo.surfaceview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afei.camerademo.CameraFragment;

public class SurfaceCameraFragment extends CameraFragment {

    private CameraSurfaceView mCameraView;

    public static SurfaceCameraFragment newInstance() {
        SurfaceCameraFragment fragment = new SurfaceCameraFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCameraView = new CameraSurfaceView(getActivity());
        mCameraProxy = mCameraView.getCameraProxy();
        return mCameraView;
    }

    @Override
    public void switchCamera() {
        mCameraProxy.switchCamera();
        mCameraProxy.startPreview(mCameraView.getHolder());
    }
}
