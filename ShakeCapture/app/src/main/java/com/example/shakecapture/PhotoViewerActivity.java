package com.example.shakecapture;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class PhotoViewerActivity extends AppCompatActivity {
    public static final String EXTRA_PATH = "photo_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);
        ImageView imageView = findViewById(R.id.full_photo);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            finish();
            return;
        }
        File photo = new File(path);
        boolean validPath;
        try {
            validPath = photo.isFile() && photo.getCanonicalFile().toPath().startsWith(
                    PhotoRepository.getPhotoDirectory(this).getCanonicalFile().toPath());
        } catch (java.io.IOException exception) {
            validPath = false;
        }
        if (!validPath) {
            Toast.makeText(this, R.string.photo_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        imageView.setImageBitmap(BitmapFactory.decodeFile(photo.getAbsolutePath()));
        imageView.setOnClickListener(v -> finish());
    }
}
