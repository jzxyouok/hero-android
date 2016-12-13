package com.hero.depandency;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * Created by R9L7NGH on 2016/3/16.
 */
public class BitmapUtils {
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            return inSampleSize;
        }
        return 1;
    }

    public static long getBitmapResolution(String filename) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        final int height = options.outHeight;
        final int width = options.outWidth;
        return height * width;
    }

    public static int getSuitableSampleSize(long resolution, long targetResolution) {
        final int max = 10;
        for (int i = 1; i < max; i++) {
            long newResolution = resolution / (i * i);
            if (newResolution < targetResolution) {
                return i;
            }
        }
        return max;
    }

    public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

    public static Bitmap decodeBitmapFromBase64(String base64Str) {
        try {
            byte[] decode = Base64Utils.decode(base64Str, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将Bitmap压缩到指定尺寸。
     *
     * @param bitmap
     * @param width
     * @param height
     * @return
     */
    public static Bitmap compressBitmap(Bitmap bitmap, int width, int height) {
        if (bitmap == null || bitmap.getWidth() == width && bitmap.getHeight() == height) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(width / (float) bitmap.getWidth(), height / (float) bitmap.getHeight());
        Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        return result;
    }

    /**
     * 从bitmap中读取二进制数据
     *
     * 注意：从BitmapCache中读取的bitmap一律禁止回收
     *
     * @param bmp
     * @param needRecycle 从BitmapCache中读取的图片请传入false
     * @return
     */
    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
