package com.example.shakecapture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PhotoRepository {
    private static final String DIRECTORY_NAME = "shakecapture_photos";
    private static final String FILE_PREFIX = "shakecapture_";

    private PhotoRepository() {
    }

    public static File getPhotoDirectory(Context context) {
        File directory = new File(context.getFilesDir(), DIRECTORY_NAME);
        if (!directory.exists() && !directory.mkdirs()) {
            return directory;
        }
        return directory;
    }

    public static File createPhotoFile(Context context) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
                .format(new Date());
        return new File(getPhotoDirectory(context), FILE_PREFIX + timestamp + ".jpg");
    }

    public static List<Photo> getPhotos(Context context) {
        File[] files = getPhotoDirectory(context).listFiles((dir, name) ->
                name.startsWith(FILE_PREFIX) && name.toLowerCase(Locale.US).endsWith(".jpg"));
        List<Photo> photos = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    photos.add(new Photo(file));
                }
            }
        }
        Collections.sort(photos, Comparator.comparingLong(
                (Photo photo) -> photo.getFile().lastModified()).reversed());
        return photos;
    }

    public static boolean delete(Photo photo) {
        return photo.getFile().delete();
    }

    public static Bitmap decodeThumbnail(Photo photo, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photo.getFile().getAbsolutePath(), options);
        options.inSampleSize = calculateSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(photo.getFile().getAbsolutePath(), options);
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int width, int height) {
        int sampleSize = 1;
        while (options.outHeight / sampleSize > height * 2
                || options.outWidth / sampleSize > width * 2) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    public static void saveBitmap(Bitmap bitmap, File destination) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(destination)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                throw new IOException("Unable to encode JPEG image");
            }
        }
    }
}
