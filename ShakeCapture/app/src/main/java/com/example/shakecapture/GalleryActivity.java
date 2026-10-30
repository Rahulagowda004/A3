package com.example.shakecapture;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryActivity extends AppCompatActivity implements PhotoAdapter.Listener {
    private PhotoAdapter adapter;
    private TextView emptyState;
    private RecyclerView recyclerView;
    private final ExecutorService repositoryExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        recyclerView = findViewById(R.id.photo_grid);
        emptyState = findViewById(R.id.empty_state);
        adapter = new PhotoAdapter(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhotos();
    }

    private void loadPhotos() {
        repositoryExecutor.execute(() -> {
            java.util.List<Photo> photos = PhotoRepository.getPhotos(this);
            runOnUiThread(() -> {
                adapter.setPhotos(photos);
                boolean empty = photos.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    @Override
    public void onPhotoClicked(Photo photo) {
        Intent intent = new Intent(this, PhotoViewerActivity.class);
        intent.putExtra(PhotoViewerActivity.EXTRA_PATH, photo.getFile().getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(Photo photo) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_photo_title)
                .setMessage(R.string.delete_photo_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (!PhotoRepository.delete(photo)) {
                        android.widget.Toast.makeText(this, R.string.delete_failed,
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                    loadPhotos();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        adapter.shutdown();
        repositoryExecutor.shutdownNow();
        super.onDestroy();
    }
}
