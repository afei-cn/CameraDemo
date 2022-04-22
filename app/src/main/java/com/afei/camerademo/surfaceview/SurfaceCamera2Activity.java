package com.afei.camerademo.surfaceview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.afei.camerademo.ImageUtils;
import com.afei.camerademo.R;
import com.afei.camerademo.camera.Camera2Proxy;

import java.nio.ByteBuffer;

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
                break;
            case R.id.take_picture_iv:
                mCameraProxy.captureStillPicture(reader -> new ImageSaveTask().execute(reader.acquireNextImage()));
                break;
            case R.id.picture_iv:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivity(intent);
                break;
        }
    }

    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image ... images) {
            ByteBuffer buffer = images[0].getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            if (mCameraProxy.isFrontCamera()) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                // 前置摄像头需要左右镜像
                Bitmap rotateBitmap = ImageUtils.rotateBitmap(bitmap, 0, true, true);
                ImageUtils.saveBitmap(rotateBitmap);
                rotateBitmap.recycle();
            } else {
                ImageUtils.saveImage(bytes);
            }
            images[0].close();
            return ImageUtils.getLatestThumbBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
        }
    }
}
