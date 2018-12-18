package com.afei.camerademo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startCameraActivity(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        switch (view.getId()) {
            case R.id.camera_btn1:
                intent.putExtra(CameraActivity.ARG_TYPE, CameraType.TYPE_SURFACEVIEW_CAMERA);
                break;
            case R.id.camera_btn2:
                intent.putExtra(CameraActivity.ARG_TYPE, CameraType.TYPE_TEXTUREVIEW_CAMERA);
                break;
            case R.id.camera_btn3:
                intent.putExtra(CameraActivity.ARG_TYPE, CameraType.TYPE_GLSURFACEVIEW_CAMERA);
                break;
        }
        startActivity(intent);
    }
}
