package com.afei.camerademo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afei.camerademo.camera.CameraSurfaceView;

public class CameraFragment extends Fragment {

    public static final int TYPE_TEXTURE_VIEW_CAMERA = 1;

    private static final String ARG_PARAM1 = "param1";
    private int mType;

    public CameraFragment() {
    }

    public static CameraFragment newInstance(int type) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mType = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CameraSurfaceView cameraView = new CameraSurfaceView(getActivity());
        return cameraView;
    }

}
