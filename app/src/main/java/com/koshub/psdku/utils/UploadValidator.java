package com.koshub.psdku.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.util.Arrays;
import java.util.List;

/**
 * Utility for validating image uploads.
 */
public class UploadValidator {

    public static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    public static class ValidationResult {
        public final boolean isValid;
        public final String message;

        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }

    public static ValidationResult validateImage(Context context, Uri uri) {
        if (uri == null) {
            return new ValidationResult(false, "File tidak ditemukan.");
        }

        long size = getFileSize(context, uri);
        if (size <= 0) {
            return new ValidationResult(false, "File tidak valid atau kosong.");
        }
        if (size > MAX_IMAGE_SIZE_BYTES) {
            return new ValidationResult(false, "Ukuran file terlalu besar. Maksimal 5 MB.");
        }

        String mimeType = getMimeType(context, uri);
        if (!isAllowedImageType(mimeType)) {
            return new ValidationResult(false, "Format file tidak didukung. Gunakan JPG, PNG, atau WEBP.");
        }

        return new ValidationResult(true, "OK");
    }

    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            String path = uri.getPath();
            if (path != null) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    size = file.length();
                }
            }
        }
        return size;
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            mimeType = context.getContentResolver().getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (fileExtension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
            }
        }
        return mimeType;
    }

    public static boolean isAllowedImageType(String mimeType) {
        return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase());
    }
}
