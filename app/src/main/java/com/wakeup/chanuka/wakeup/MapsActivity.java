package com.wakeup.chanuka.wakeup;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wakeup.chanuka.wakeup.common.AlertDialogUtil;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, OnMyLocationButtonClickListener, ActivityCompat.OnRequestPermissionsResultCallback, LocationListener {


    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /* Position */
    private static final int MINIMUM_TIME = 5000;  // 10s
    private static final int MINIMUM_DISTANCE = 3; // 50m

    private static final int ALARM_DISTANCE = 50;

    private static MapsActivity inst;
    private boolean mPermissionDenied = false;
    private GoogleMap mMap;
    private String mProviderName;
    private LatLng destination;
    LocationManager locationManager;
    AlarmManager alarmManager;
    MediaPlayer mMediaPlayer = new MediaPlayer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Get the best provider between gps, network and passive
        Criteria criteria = new Criteria();
        mProviderName = locationManager.getBestProvider(criteria, true);

        if (PermissionUtil.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                && PermissionUtil.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {

            // No one provider activated: prompt GPS
            if (mProviderName == null || mProviderName.equals("")) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }

            locationManager.requestLocationUpdates(mProviderName, MINIMUM_TIME, MINIMUM_DISTANCE, this);
            Location location = locationManager.getLastKnownLocation(mProviderName);
            if (location != null) {
                onLocationChanged(location);

            }

            setContentView(R.layout.activity_maps);
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        } else {

            if (PermissionUtil.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                PermissionUtil.requestPermissions(this,"ACCESS_COARSE_LOCATION");
            }
            if (PermissionUtil.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                PermissionUtil.requestPermissions(this,"ACCESS_FINE_LOCATION");
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMaxZoomPreference(mMap.getMaxZoomLevel());
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                setDestination(latLng);
                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                // This will be displayed on taping the marker
                markerOptions.title(latLng.latitude + " : " + latLng.longitude);
                markerOptions.draggable(true);

                // Clears the previously touched position
                mMap.clear();

                // Animating to the touched position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.setMaxZoomPreference(mMap.getMaxZoomLevel());

                // Placing a marker on the touched position
                mMap.addMarker(markerOptions);
                AlertDialogUtil.show(MapsActivity.this, "my title", "my message");

            }
        });

        enableMyLocation();
//        initializeLocationManager();
    }

    public void setDestination(LatLng latLng) {
        destination = latLng;
    }

    public LatLng getDestination() {
        return destination;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!mMediaPlayer.isPlaying()) {
            LatLng dest = getDestination();

            if (dest != null) {
                Toast.makeText(getApplicationContext(), "destination lat lon" + (float) destination.latitude + " : " + (float) destination.longitude, Toast.LENGTH_SHORT).show();

                float dist = distanceBetween(location, dest);

                if (dist < ALARM_DISTANCE) {
                    Toast.makeText(getApplicationContext(), "dist < 80", Toast.LENGTH_SHORT).show();
                    playAlarm();

                } else {
                    Toast.makeText(getApplicationContext(), "dist else", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onProviderDisabled(String arg0) {

        Log.i("called", "onProviderDisabled");
    }

    @Override
    public void onProviderEnabled(String arg0) {

        Log.i("called", "onProviderEnabled");
    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

        Log.i("called", "onStatusChanged");
    }

    private void playAlarm() {

        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            mMediaPlayer.setDataSource(this, alert);

            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            int maxVolume = audioManager.getStreamVolume(audioManager.STREAM_RING);
//            int maxVolume = audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepare();
            mMediaPlayer.start();

        } catch (Exception e) {
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {

        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            PermissionUtil.requestPermissions(this,"ACCESS_COARSE_LOCATION");
        }
        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PermissionUtil.requestPermissions(this,"ACCESS_FINE_LOCATION");
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        LatLng myLatLng = getMyLatLng();
//        LatLng sydney = new LatLng(-34, 151);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLatLng));

        return false;
    }

    private Location getMyLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            PermissionUtil.requestPermissions(this, "ACCESS_COARSE_LOCATION");
        }
        Location myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (myLocation == null) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            String provider = lm.getBestProvider(criteria, true);
            myLocation = lm.getLastKnownLocation(provider);
        }

        return myLocation;
    }

    public LatLng getMyLatLng() {
        Location myLocation = getMyLocation();
        LatLng myLatLng;
        if (myLocation != null) {
            myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        } else {
            myLatLng = new LatLng(0, 0);
        }
        return myLatLng;
    }

    public LatLng getMyLatLngByLocation(Location myLocation) {

        LatLng myLatLng;
        if (myLocation != null) {
            myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        } else {
            myLatLng = new LatLng(0, 0);
        }
        return myLatLng;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    public static float distanceBetween(Location myLocation, LatLng destLatLng) {
        Location dest = new Location("");
        dest.setLatitude(destLatLng.latitude);
        dest.setLongitude(destLatLng.longitude);

        return myLocation.distanceTo(dest);
    }
}
