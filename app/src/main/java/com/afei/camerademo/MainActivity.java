package com.afei.camerademo;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.afei.camerademo.glsurfaceview.GLSurfaceCamera2Activity;
import com.afei.camerademo.glsurfaceview.GLSurfaceCameraActivity;
import com.afei.camerademo.surfaceview.SurfaceCamera2Activity;
import com.afei.camerademo.surfaceview.SurfaceCameraActivity;
import com.afei.camerademo.textureview.TextureCamera2Activity;
import com.afei.camerademo.textureview.TextureCameraActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1;
    private final String[] PERMISSIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
    }

    public void startCameraActivity(View view) {
        Intent intent = null;
        switch (view.getId()) {
            case R.id.camera_btn1:
                intent = new Intent(this, SurfaceCameraActivity.class);
                break;
            case R.id.camera_btn2:
                intent = new Intent(this, TextureCameraActivity.class);
                break;
            case R.id.camera_btn3:
                intent = new Intent(this, GLSurfaceCameraActivity.class);
                break;
            case R.id.camera_btn4:
                intent = new Intent(this, SurfaceCamera2Activity.class);
                break;
            case R.id.camera_btn5:
                intent = new Intent(this, TextureCamera2Activity.class);
                break;
            case R.id.camera_btn6:
                intent = new Intent(this, GLSurfaceCamera2Activity.class);
                break;
        }
        startActivity(intent);
    }

    private boolean checkPermission() {
        for (int i = 0; i < PERMISSIONS.length; i++) {
            int state = ContextCompat.checkSelfPermission(this, PERMISSIONS[i]);
            if (state != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PERMISSION_GRANTED) {
                    Toast.makeText(this, "请在设置中相关权限", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
