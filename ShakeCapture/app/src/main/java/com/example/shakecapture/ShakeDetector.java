package com.example.shakecapture;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

public class ShakeDetector implements SensorEventListener {
    public interface Listener {
        void onShakeStarted();
        void onShakeStopped();
        void onSensorUnavailable();
    }

    private static final float START_THRESHOLD = 2.4f;
    private static final float STOP_THRESHOLD = 1.25f;
    private static final long QUIET_PERIOD_MS = 220L;

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean shaking;
    private boolean stopPending;

    public ShakeDetector(Context context, Listener listener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null
                ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.listener = listener;
    }

    public boolean isAvailable() {
        return accelerometer != null;
    }

    public void start() {
        if (!isAvailable()) {
            listener.onSensorUnavailable();
            return;
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        handler.removeCallbacksAndMessages(null);
        shaking = false;
        stopPending = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float acceleration = Math.abs((float) Math.sqrt(x * x + y * y + z * z)
                - SensorManager.GRAVITY_EARTH);
        if (!shaking && acceleration >= START_THRESHOLD) {
            shaking = true;
            stopPending = false;
            listener.onShakeStarted();
        } else if (shaking && acceleration < STOP_THRESHOLD && !stopPending) {
            stopPending = true;
            handler.postDelayed(() -> {
                if (shaking) {
                    shaking = false;
                    stopPending = false;
                    listener.onShakeStopped();
                }
            }, QUIET_PERIOD_MS);
        } else if (shaking && acceleration >= STOP_THRESHOLD) {
            handler.removeCallbacksAndMessages(null);
            stopPending = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
