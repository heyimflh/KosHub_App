package com.koshub.psdku.utils;

/**
 * Configuration for Cloudinary image uploads.
 *
 * TODO SECURITY: Untuk production, upload sebaiknya memakai signed upload dari backend.
 * Untuk demo, unsigned preset dipakai dengan pembatasan ketat di Cloudinary Console.
 *
 * IMPORTANT: Only use unsigned upload presets in the Android app.
 * Never include the Cloudinary API Secret or API Key here.
 */
public class CloudinaryConfig {
    public static final String CLOUD_NAME = "doyiag572";
    public static final String UPLOAD_PRESET = "koshub_unsigned"; // Unsigned preset name
    public static final String BASE_FOLDER = "koshub";
}
