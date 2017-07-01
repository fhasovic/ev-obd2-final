package com.reversecoder.obd.reader.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.map.sync.WattsOCMSyncTask;
import com.reversecoder.obd.reader.map.sync.WattsOCMSyncTaskListener;
import com.reversecoder.obd.reader.map.util.WattsImageUtils;
import com.reversecoder.obd.reader.map.util.WattsMapUtils;

import java.util.HashMap;

/**
 * @author Md. Rashadul Alam
 */
public class MapActivity extends BaseMapActivity implements
        OnMapReadyCallback,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraIdleListener {

    private GoogleMap mMap; // The map object.

    private BitmapDescriptor mMarkerIconCar; // Icon for the car.
    private BitmapDescriptor mMarkerIconStation; // Icon for charging stations.
    private BitmapDescriptor mMarkerIconStationFast; // Icon for fast charging stations.

    private LatLng mLastCameraCenter; // Latitude and longitude of last camera center.
    private LatLng mLastOCMCameraCenter; // Latitude and longitude of last camera center where the OCM api was synced against the content provider.

    private HashMap<Long, Marker> mVisibleStationMarkers = new HashMap<>(); // hashMap of station markers in the current map

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initLoginUI();
        initLoginAction();
    }

    private void initLoginUI() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        // Create the car location marker bitmap
        mMarkerIconCar = WattsImageUtils.vectorToBitmap(this, R.drawable.ic_car_color_sharp, getResources().getInteger(R.integer.car_icon_add_to_size));
        // Create the charging station marker bitmap
        mMarkerIconStation = WattsImageUtils.vectorToBitmap(this, R.drawable.ic_station, getResources().getInteger(R.integer.station_icon_add_to_size));
        // Create the charging station marker bitmap
        mMarkerIconStationFast = WattsImageUtils.vectorToBitmap(this, R.drawable.ic_station_fast, getResources().getInteger(R.integer.station_icon_add_to_size));

    }

    private void initLoginAction() {
        // Fab for the my location. With onClickListener
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_my_location);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "My location fab clicked!");
                // Try to set last location, update car marker, and zoom to location
                updateCurrentLocation(true);

            }
        });
    }

    @Override
    public void onGoogleClientApiConnected() {
        updateCurrentLocation(true);
    }

    @Override
    public void onUserLocationChanged(Location location) {
        // Remove the old car marker
        if (mCurrentLocationMarker != null) {
            mCurrentLocationMarker.remove();
        }

        // Place current location car marker.
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = WattsImageUtils.getCarMarkerOptions(
                latLng,
                getString(R.string.marker_current),
                mMarkerIconCar
        );
        mCurrentLocationMarker = mMap.addMarker(markerOptions);
        animateCamera(mMap, latLng);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                // Disable my location button, using own fab for this
                mMap.setMyLocationEnabled(false);
                // Disable Map Toolbar
                mMap.getUiSettings().setMapToolbarEnabled(false);
                // Enable zoom control
                mMap.getUiSettings().setZoomControlsEnabled(true);
            }
        } else {
            // Disable my location button, using own fab for this
            mMap.setMyLocationEnabled(false);
            // Disable Map Toolbar
            mMap.getUiSettings().setMapToolbarEnabled(false);
            // Enable zoom control
            mMap.getUiSettings().setZoomControlsEnabled(true);
        }
        // Setup callback for camera movement (onCameraMove).
        mMap.setOnCameraMoveListener(this);
        // Setup callback for when camera has stopped moving (onCameraIdle).
        mMap.setOnCameraIdleListener(this);
//        // Setup callback for when user clicks on marker
//        mMap.setOnMarkerClickListener(this);

    }

    @Override
    public void onCameraMove() {

        // Get the current visible region of the map
        VisibleRegion visibleRegion = mMap.getProjection().getVisibleRegion();
        // Get the center of current map view
        mLastCameraCenter = visibleRegion.latLngBounds.getCenter();

    }

    @Override
    public void onCameraIdle() {

        // Init sync from OCM
        float[] results = new float[3];
        if ((mLastCameraCenter != null) && (mLastOCMCameraCenter != null)) {
            Location.distanceBetween(
                    mLastCameraCenter.latitude,
                    mLastCameraCenter.longitude,
                    mLastOCMCameraCenter.latitude,
                    mLastOCMCameraCenter.longitude,
                    results);

            float ocmCameraDelta = results[0]; //
            Log.d(TAG, "onCameraIdle camera delta: " + results[0] + ", " + results[1] + ", " + results[2]);
            // update content provider if significant movement
            if (ocmCameraDelta > getResources().getInteger(R.integer.delta_trigger_camera_significantly_changed)) {

                mLastOCMCameraCenter = mLastCameraCenter;

                executeOCMSync(mLastCameraCenter.latitude, mLastCameraCenter.longitude);

            }

            // Add and update markers for stations in the current visible area
            WattsMapUtils.updateStationMarkers(this, mMap, mVisibleStationMarkers, mMarkerIconStation, mMarkerIconStationFast);
        }
    }

    protected synchronized void executeOCMSync(Double latitude, Double longitude) {
        // TODO: add some more rate limiting?
        WattsOCMSyncTask wattsOCMSyncTask = new WattsOCMSyncTask(this,
                latitude,
                longitude,
                (double) getResources().getInteger(R.integer.ocm_radius_km),
                new WattsOCMSyncTaskListener() {
                    @Override
                    public void onOCMSyncSuccess(Object object) {

                        // Also Add and update markers for stations in the current visible area
                        // every time an ocm sync if finished in case of slow updates
                        WattsMapUtils.updateStationMarkers(MapActivity.this, mMap, mVisibleStationMarkers, mMarkerIconStation, mMarkerIconStationFast);
                    }

                    @Override
                    public void onOCMSyncFailure(Exception exception) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_OCM_sync_failure) + exception.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                }
        );

        wattsOCMSyncTask.execute();
    }

    protected void updateCurrentLocation(Boolean moveCamera) {
        if ((ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) && (checkLocationRequestPermission())) {
            // Handle locations of handset
            if (mGoogleApiClient != null) {
                // Get the last location, and center the map
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (mLastLocation != null) {

                    LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    MarkerOptions markerOptions = WattsImageUtils.getCarMarkerOptions(
                            latLng,
                            getString(R.string.marker_current),
                            mMarkerIconCar
                    );
                    // Remove the old car marker
                    if (mCurrentLocationMarker != null) {
                        mCurrentLocationMarker.remove();
                    }
                    mCurrentLocationMarker = mMap.addMarker(markerOptions);

                    if (moveCamera) {
                        // move the camera
//                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        animateCamera(mMap, latLng);
                    }
                    // save camera center
                    mLastCameraCenter = mMap.getProjection().getVisibleRegion().latLngBounds.getCenter();

                    // OCM camera center is the same as last camera center at this point.
                    mLastOCMCameraCenter = mLastCameraCenter;
                }
            } else {
                Log.d(TAG, "GoogleApiClient not connected");
            }
        }
    }
}
