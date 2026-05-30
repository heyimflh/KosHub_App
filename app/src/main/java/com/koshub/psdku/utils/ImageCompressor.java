package com.koshub.psdku.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility to compress and resize images.
 */
public class ImageCompressor {
    private static final String TAG = "ImageCompressor";
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 1280;
    private static final int QUALITY = 80;

    public static File compressImage(Context context, Uri imageUri) throws IOException {
        Bitmap bitmap = decodeSampledBitmapFromUri(context, imageUri, MAX_WIDTH, MAX_HEIGHT);
        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap from URI");
        }

        File cacheDir = context.getCacheDir();
        File compressedFile = File.createTempFile("koshub_upload_", ".jpg", cacheDir);

        try (FileOutputStream out = new FileOutputStream(compressedFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out);
            out.flush();
        } finally {
            bitmap.recycle();
        }

        Log.d(TAG, "Compressed image saved to: " + compressedFile.getAbsolutePath() + " size: " + compressedFile.length());
        return compressedFile;
    }

    private static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) return null;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        input = context.getContentResolver().openInputStream(uri);
        Bitmap sampledBitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        if (sampledBitmap == null) return null;

        // Scale to exact dimensions if needed
        return scaleBitmap(sampledBitmap, reqWidth, reqHeight);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float aspectRatio = (float) width / (float) height;
        if (width > height) {
            width = maxWidth;
            height = Math.round(width / aspectRatio);
        } else {
            height = maxHeight;
            width = Math.round(height * aspectRatio);
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        if (scaledBitmap != bitmap) {
            bitmap.recycle();
        }
        return scaledBitmap;
    }
}
