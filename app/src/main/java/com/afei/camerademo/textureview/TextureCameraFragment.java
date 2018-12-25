package com.afei.camerademo.textureview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afei.camerademo.CameraFragment;
import com.afei.camerademo.surfaceview.CameraSurfaceView;

public class TextureCameraFragment extends CameraFragment {

    private CameraTextureView mCameraView;

    public static TextureCameraFragment newInstance() {
        TextureCameraFragment fragment = new TextureCameraFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCameraView = new CameraTextureView(getActivity());
        mCameraProxy = mCameraView.getCameraProxy();
        return mCameraView;
    }

    @Override
    public void startPreview() {
        mCameraProxy.startPreview(mCameraView.getSurfaceTexture());
    }

}
