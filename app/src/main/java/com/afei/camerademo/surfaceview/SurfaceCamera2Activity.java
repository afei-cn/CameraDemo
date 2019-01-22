package com.afei.camerademo.surfaceview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.afei.camerademo.ImageUtils;
import com.afei.camerademo.R;
import com.afei.camerademo.camera.Camera2Proxy;

public class SurfaceCamera2Activity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SurfaceCamera2Activity";

    private ImageView mCloseIv;
    private ImageView mSwitchCameraIv;
    private ImageView mTakePictureIv;
    private ImageView mPictureIv;
    private Camera2SurfaceView mCameraView;

    private Camera2Proxy mCameraProxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_camera2);
        initView();
    }

    private void initView() {
        mCloseIv = findViewById(R.id.toolbar_close_iv);
        mCloseIv.setOnClickListener(this);
        mSwitchCameraIv = findViewById(R.id.toolbar_switch_iv);
        mSwitchCameraIv.setOnClickListener(this);
        mTakePictureIv = findViewById(R.id.take_picture_iv);
        mTakePictureIv.setOnClickListener(this);
        mPictureIv = findViewById(R.id.picture_iv);
        mPictureIv.setOnClickListener(this);
        mPictureIv.setImageBitmap(ImageUtils.getLatestThumbBitmap());
        mCameraView = findViewById(R.id.camera_view);
        mCameraProxy = mCameraView.getCameraProxy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_close_iv:
                finish();
                break;
            case R.id.toolbar_switch_iv:
                mCameraProxy.switchCamera();
                mCameraProxy.startPreview();
                break;
            case R.id.take_picture_iv:
                break;
            case R.id.picture_iv:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivity(intent);
                break;
        }
    }

    private class ImageSaveTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... bytes) {
            long time = System.currentTimeMillis();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes[0], 0, bytes[0].length);
            Log.d(TAG, "BitmapFactory.decodeByteArray time: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            ImageUtils.saveBitmap(bitmap);
            Log.d(TAG, "saveBitmap time: " + (System.currentTimeMillis() - time));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mPictureIv.setImageBitmap(ImageUtils.getLatestThumbBitmap());
        }
    }
}
