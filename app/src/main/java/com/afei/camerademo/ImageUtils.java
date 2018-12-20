package com.afei.camerademo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    private static final String GALLERY_PATH = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_DCIM) + File.separator + "Camera";

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Thumbnails._ID,
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static void saveImage(byte[] jpeg) {
        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Log.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        try {
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(jpeg);
            os.flush();
            os.close();
            insertToDB(outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void insertToDB(String picturePath) {
        ContentValues values = new ContentValues();
        ContentResolver resolver = MyApp.getInstance().getContentResolver();
        values.put(MediaStore.Images.ImageColumns.DATA, picturePath);
        values.put(MediaStore.Images.ImageColumns.TITLE, picturePath.substring(picturePath.lastIndexOf("/") + 1));
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static Bitmap getLatestThumbBitmap(Context context) {
        // 按照时间顺序降序查询
        Cursor cursor = MediaStore.Images.Media.query(context.getContentResolver(), MediaStore.Images.Media
                .EXTERNAL_CONTENT_URI, STORE_IMAGES, null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
        cursor.moveToFirst();
        long id = cursor.getLong(0);
        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), id, MediaStore.Images
                .Thumbnails.MICRO_KIND, null);
        Log.d(TAG, "bitmap width: " + bitmap.getWidth());
        Log.d(TAG, "bitmap height: " + bitmap.getHeight());
        return bitmap;
    }
}
