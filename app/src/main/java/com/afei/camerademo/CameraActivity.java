package com.afei.camerademo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "CameraActivity";
    public static final String ARG_TYPE = "type";

    private ImageView mCloseIv;
    private ImageView mSwitchCameraIv;
    private ImageView mSettingsIv;
    private ImageView mTakePictureIv;

    private int mType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mType = intent.getIntExtra(ARG_TYPE, CameraType.TYPE_SURFACEVIEW_CAMERA);
        Log.d(TAG, "onCreate: mType: " + mType);
        setContentView(R.layout.activity_camera);
        initView();
        checkPermission();
    }

    private void initView() {
        mCloseIv = findViewById(R.id.toolbar_close_iv);
        mSwitchCameraIv = findViewById(R.id.toolbar_switch_iv);
        mSettingsIv = findViewById(R.id.toolbar_settings_iv);
        mTakePictureIv = findViewById(R.id.take_picture_iv);
        mCloseIv.setOnClickListener(this);
        mSwitchCameraIv.setOnClickListener(this);
        mSettingsIv.setOnClickListener(this);
        mTakePictureIv.setOnClickListener(this);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            for (int i = 0; i < permissions.length; i++) {
                int state = ContextCompat.checkSelfPermission(this, permissions[i]);
                if (state != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 200);
                    return;
                }
            }
        }
        attachFragment();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == 200) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "请在设置中打开摄像头或存储权限", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, 0);
                    return;
                }
            }
        }
        attachFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        attachFragment();
    }

    private void attachFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, CameraFragment
                .newInstance(mType)).commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_close_iv:
                finish();
                break;
            case R.id.toolbar_switch_iv:
                break;
            case R.id.toolbar_settings_iv:
                break;
            case R.id.take_picture_iv:
                break;
        }
    }
}
