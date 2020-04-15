package com.location.firebasedb;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    Context mContext;

    FirebaseDatabase mDatabase;
    private DatabaseReference mCurrentLocation;
    private DatabaseReference mLocationPoints;
    private String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        mDatabase = FirebaseDatabase.getInstance();
        mCurrentLocation = mDatabase.getReference("mCurrentLocation");
        mLocationPoints = mDatabase.getReference("mLocationPoints");

        key = mCurrentLocation.push().getKey();

        mCurrentLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    LocationPoint value = dataSnapshot.getValue(LocationPoint.class);
                    if (value != null)
                        Log.d("RRR ", "Lat: " + value.getLat() + "Lng: " + value.getLng());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("RRR ", "Failed to read value.", error.toException());
            }
        });
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                112);
        System.out.println("RRR MainActivity.requestPermissions");
        Toast.makeText(mContext, "check permission", Toast.LENGTH_SHORT).show();
    }

    double lat = 0;
    double lng = 0;
    double endLat = 0;
    double endLng = 0;
    double bearing = 0;

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {


            if (endLng == 0 || endLat == 0) {
                endLng = location.getLongitude();
                endLat = location.getLatitude();
            } else {
                lat = endLat;
                endLat = location.getLatitude();
                lng = endLng;
                endLng = location.getLongitude();

                bearing = bearingBetweenLocations(lat, lng, endLat, endLng);
                System.out.println("RRR bearing " + bearing);
            }

            System.out.println("RRR = " + "Lat: " + lat + "Lng: " + lng + "bearing: " + bearing);

            mCurrentLocation.child(key).setValue(new LocationPoint(lat, lng, bearing));
            mLocationPoints.child(mLocationPoints.push().getKey()).setValue(new LocationPoint(lat, lng, bearing));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    protected void onResume() {
        super.onResume();
        isLocationEnabled();
    }

    public void btClearDb(View view) {
        mCurrentLocation.removeValue();
        mLocationPoints.removeValue();
    }

    public void btStopFetching(View view) {
        locationManager.removeUpdates(locationListenerGPS);
    }

    public void btFetchLocation(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions();
        else locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000, 10, locationListenerGPS);
    }

    private void isLocationEnabled() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Enable Location");
            alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
            alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 112) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        2000, 10, locationListenerGPS);
                Toast.makeText(mContext, "All permissions granted", Toast.LENGTH_SHORT).show();
            } else Toast.makeText(mContext, "Permission(s) missing", Toast.LENGTH_SHORT).show();
        }
    }

    private double bearingBetweenLocations(double... points) {

        double PI = 3.14159;
        double lat1 = points[0] * PI / 180;
        double long1 = points[1] * PI / 180;
        double lat2 = points[2] * PI / 180;
        double long2 = points[3] * PI / 180;
        double dLon = (long2 - long1);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }

    public void btCheckBearing(View view) {
        double bearing = bearingBetweenLocations(13.0590, 80.2542, 13.052522, 80.251879);
        System.out.println("RRR bearing = " + bearing);
        Toast.makeText(mContext, "RRR bearing = " + bearing, Toast.LENGTH_SHORT).show();
    }
}