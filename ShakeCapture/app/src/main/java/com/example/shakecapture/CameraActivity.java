package com.example.shakecapture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements ShakeDetector.Listener {
    private PreviewView previewView;
    private TextView statusText;
    private ProgressBar loading;
    private ImageCapture imageCapture;
    private ShakeDetector shakeDetector;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private boolean captureScheduled;
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    statusText.setText(R.string.camera_permission_denied);
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.camera_preview);
        statusText = findViewById(R.id.camera_status);
        loading = findViewById(R.id.camera_loading);
        shakeDetector = new ShakeDetector(this, this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        loading.setVisibility(View.VISIBLE);
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = providerFuture.get();
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                loading.setVisibility(View.GONE);
                statusText.setText(R.string.shake_ready);
                shakeDetector.start();
            } catch (Exception exception) {
                loading.setVisibility(View.GONE);
                statusText.setText(R.string.camera_initialization_failed);
                Toast.makeText(this, R.string.camera_initialization_failed,
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onShakeStarted() {
        if (!captureScheduled) {
            statusText.setText(R.string.shake_in_progress);
        }
    }

    @Override
    public void onShakeStopped() {
        if (captureScheduled || imageCapture == null) {
            return;
        }
        captureScheduled = true;
        statusText.setText(R.string.capture_countdown);
        handler.postDelayed(this::capturePhoto, 1000L);
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            captureScheduled = false;
            return;
        }
        File destination = PhotoRepository.createPhotoFile(this);
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(destination).build();
        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        runOnUiThread(() -> {
                            captureScheduled = false;
                            statusText.setText(R.string.photo_saved);
                            Toast.makeText(CameraActivity.this, R.string.photo_saved,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        destination.delete();
                        runOnUiThread(() -> {
                            captureScheduled = false;
                            statusText.setText(R.string.capture_failed);
                            Toast.makeText(CameraActivity.this, R.string.capture_failed,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    @Override
    public void onSensorUnavailable() {
        statusText.setText(R.string.accelerometer_unavailable);
        Toast.makeText(this, R.string.accelerometer_unavailable, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shakeDetector != null && imageCapture != null) {
            shakeDetector.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shakeDetector != null) {
            shakeDetector.stop();
        }
        handler.removeCallbacksAndMessages(null);
        captureScheduled = false;
    }

    @Override
    protected void onDestroy() {
        cameraExecutor.shutdown();
        super.onDestroy();
    }
}
