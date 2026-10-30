package com.example.shakecapture;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialButton takePhoto = findViewById(R.id.take_photo_button);
        MaterialButton browsePhotos = findViewById(R.id.browse_photos_button);
        takePhoto.setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));
        browsePhotos.setOnClickListener(v -> startActivity(new Intent(this, GalleryActivity.class)));
    }
}