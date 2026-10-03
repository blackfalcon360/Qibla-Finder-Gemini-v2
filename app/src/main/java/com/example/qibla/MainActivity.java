package com.example.qibla;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView arrowImageView;
    private TextView degreeTextView;

    private SensorManager sensorManager;
    private Sensor compassSensor;

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // Kaaba Coordinates (Makkah)
    private static final double KAABA_LATITUDE = 21.422487;
    private static final double KAABA_LONGITUDE = 39.826206;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arrowImageView = findViewById(R.id.arrowImageView);
        degreeTextView = findViewById(R.id.degreeTextView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        } else {
            getUserLocation();
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = location;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (compassSensor != null) {
            sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float azimuth = Math.round(event.values[0]);

        if (currentLocation != null) {
            float qiblaBearing = calculateQiblaBearing(currentLocation.getLatitude(), currentLocation.getLongitude());
            float direction = azimuth - qiblaBearing;

            arrowImageView.setRotation(-direction);
            degreeTextView.setText(Math.round(qiblaBearing) + "°");
        } else {
            arrowImageView.setRotation(-azimuth);
            degreeTextView.setText(Math.round(azimuth) + "°");
        }
    }

    private float calculateQiblaBearing(double lat, double lon) {
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double lon2 = Math.toRadians(KAABA_LONGITUDE);

        double deltaLon = lon2 - lon1;

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

        double bearing = Math.atan2(y, x);
        bearing = Math.toDegrees(bearing);
        return (float) ((bearing + 360) % 360);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            Toast.makeText(this, "Permission required for accurate Qibla direction", Toast.LENGTH_SHORT).show();
        }
    }
}
