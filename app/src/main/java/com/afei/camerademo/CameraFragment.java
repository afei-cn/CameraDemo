package com.afei.camerademo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afei.camerademo.camera.CameraProxy;
import com.afei.camerademo.camera.CameraSurfaceView;
import com.afei.camerademo.camera.CameraTextureView;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private static final String ARG_TYPE = "type";
    private int mType;

    private View mContentView; // camera view
    private CameraProxy mCameraProxy; // camera controller

    public CameraFragment() {
    }

    public static CameraFragment newInstance(int type) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mType = getArguments().getInt(ARG_TYPE, CameraType.TYPE_SURFACEVIEW_CAMERA);
            Log.d(TAG, "onCreate: mType: " + mType);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        switch (mType) {
            case CameraType.TYPE_TEXTUREVIEW_CAMERA:
                mContentView = new CameraTextureView(getActivity());
            case CameraType.TYPE_SURFACEVIEW_CAMERA:
                mContentView = new CameraSurfaceView(getActivity());
                mCameraProxy = ((CameraSurfaceView) mContentView).getCameraProxy();
        }
        return mContentView;
    }

    public void switchCamera() {
        mCameraProxy.switchCamera();
    }

}
