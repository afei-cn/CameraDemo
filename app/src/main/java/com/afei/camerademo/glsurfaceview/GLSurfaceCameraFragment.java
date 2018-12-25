package com.afei.camerademo.glsurfaceview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afei.camerademo.CameraFragment;

public class GLSurfaceCameraFragment extends CameraFragment {

    private CameraGLSurfaceView mCameraView;

    public static GLSurfaceCameraFragment newInstance() {
        GLSurfaceCameraFragment fragment = new GLSurfaceCameraFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCameraView = new CameraGLSurfaceView(getActivity());
        mCameraProxy = mCameraView.getCameraProxy();
        return mCameraView;
    }

    @Override
    public void startPreview() {
        mCameraProxy.startPreview(mCameraView.getSurfaceTexture());
    }

}
