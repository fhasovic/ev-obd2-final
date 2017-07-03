package com.reversecoder.obd.reader.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.util.BroadcastReceiverManager;
import com.reversecoder.obd.reader.util.MarkerManager;

/**
 * @author Md. Rashadul Alam
 */
public abstract class BaseMapActivity extends AppCompatActivity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult> {

    public static final String TAG = BaseMapActivity.class.getSimpleName();
    private static final int SETTINGS_LOCATION = 1;
    public GoogleApiClient mGoogleApiClient; // The google services connection.
    private LocationRequest mLocationRequest; // Periodic location request object.
    private static final int PERMISSIONS_REQUEST_LOCATION = 3;
    private static final String BROADCAST_ACTION = "android.location.PROVIDERS_CHANGED";
    private static final int INTENT_PLACE_SEARCH = 2; // For places search
    BroadcastReceiverManager receiverManager;

    //abstract methods
    public abstract void onGoogleClientApiConnected();

    public abstract void onUserLocationChanged(Location location);

    private PlaceSelectionListener mPlaceSelectionListener;
    private GoogleMap placeSearchGoogleMap;

    private boolean isRequestedForPermission = false;
    private boolean isRequestedForPlaceSearch = false;
    private boolean isRequestedForLocationSetting = false;

    public Marker mCurrentLocationMarker;
    private BitmapDescriptor mMarkerIcon;
    public Location mLastLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Check if Google Play Services Available or not
        if (!checkGooglePlayServices()) {
            Log.d("onCreate", "Google Play Services are not available");
            Toast.makeText(BaseMapActivity.this, "Please install google play service.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Log.d("onCreate", "Google Play Services available.");
            receiverManager = BroadcastReceiverManager.init(BaseMapActivity.this);

//            mMarkerIcon = MarkerManager.vectorToBitmap(BaseMapActivity.this, R.drawable.marker_black_star, 20);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (isRequestedForPermission) {
            return;
        }

        if (isRequestedForLocationSetting) {
            return;
        }

        if (isRequestedForPlaceSearch) {
            return;
        }

        initGoogleAPIClient();

        if (!receiverManager.isReceiverRegistered(gpsLocationReceiver)) {
            receiverManager.registerReceiver(gpsLocationReceiver, new IntentFilter(BROADCAST_ACTION));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isRequestedForPermission) {
            return;
        }

        if (isRequestedForLocationSetting) {
            return;
        }

        if (isRequestedForPlaceSearch) {
            return;
        }

        //Unregister receiver on stop
        if (receiverManager.isReceiverRegistered(gpsLocationReceiver)) {
            receiverManager.unregisterReceiver(gpsLocationReceiver);
        }

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    /*************************************************
     * LocationListener
     * ***********************************************/

    @Override
    public void onLocationChanged(Location location) {

        // Update last location the the new location
        mLastLocation = location;
        onUserLocationChanged(location);

//        // Remove the old current marker
//        if (mCurrentLocationMarker != null) {
//            mCurrentLocationMarker.remove();
//        }

        // Place current location marker.
//        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        MarkerOptions markerOptions = MarkerManager.getMarkerOptions(latLng, "That's you",  mMarkerIcon);
//        mCurrentLocationMarker = mMap.addMarker(markerOptions);


    }

    /*************************************************
     * Google play service availablity
     * ***********************************************/

    /**
     * Check if the user allows Google play services. Prerequisite for this app, bail if denied.
     *
     * @return True or False
     */
    public boolean checkGooglePlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        1234).show();
            }
            return false;
        }
        return true;
    }

    /*************************************************
     * Google client connection
     * ***********************************************/

    /* Initiate Google API Client  */
    public void initGoogleAPIClient() {
        //Without Google API Client Auto Location Dialog will not work
        Log.d(TAG, "buildGoogleApiClient");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(BaseMapActivity.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
        }
        mGoogleApiClient.connect();
    }


    /**
     * GoogleApiClient.ConnectionCallbacks
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Check location request Permission
        if (checkLocationRequestPermission()) {
            checkLocationSettings();
        }
    }

    /**
     * GoogleApiClient.ConnectionCallbacks
     */
    @Override
    public void onConnectionSuspended(int i) {
    }

    /**
     * GoogleApiClient.OnConnectionFailedListener
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient onConnectionFailed");
    }

    /*************************************************
     * Check permission
     * ***********************************************/

    public boolean checkLocationRequestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermissionSetting();
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    /*  Show Popup to access User Permission  */
    private void requestLocationPermissionSetting() {
        isRequestedForPermission = true;
        if (ActivityCompat.shouldShowRequestPermissionRationale(BaseMapActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(BaseMapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);

        } else {
            ActivityCompat.requestPermissions(BaseMapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        }
    }

    /* On Request permission method to check the permisison is granted or not for Marshmallow+ Devices  */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                isRequestedForPermission = false;
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //Check location settings
                    checkLocationSettings();

                } else {
                    updateStatus("Location Permission denied.");
//                    Toast.makeText(BaseMapActivity.this, "Location Permission denied.", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    /*************************************************
     * Check location setting
     * ***********************************************/

    /* Show Location Access Dialog */
    public void checkLocationSettings() {

        createLocationRequest();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient to show dialog always when GPS is off

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(this);
    }

    /**
     * Set up the location requests
     */
    private void createLocationRequest() {
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000); // ideal interval
            mLocationRequest.setFastestInterval(5000); // the fastest interval my app can handle
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // highest accuracy
        }
    }

    @Override
    public void onResult(LocationSettingsResult result) {
        final Status status = result.getStatus();
        final LocationSettingsStates state = result.getLocationSettingsStates();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                isRequestedForLocationSetting = false;
                if (!receiverManager.isReceiverRegistered(gpsLocationReceiver)) {
                    receiverManager.registerReceiver(gpsLocationReceiver, new IntentFilter(BROADCAST_ACTION));
                }

                // All location settings are satisfied. The client can initialize location
                // requests here.
                updateStatus("GPS is Enabled in your device.");
                onGoogleClientApiConnected();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                isRequestedForLocationSetting = true;
                // Location settings are not satisfied. But could be fixed by showing the user
                // a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(BaseMapActivity.this, SETTINGS_LOCATION);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                    // Ignore the error.
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                isRequestedForLocationSetting = false;
                // Location settings are not satisfied. However, we have no way to fix the
                // settings so we won't show the dialog.
                updateStatus("Sorry! This service is not available.");
                break;
        }
    }

    public static boolean isLocationSettingEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    //Run on UI
    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            checkLocationSettings();
        }
    };

    /* Broadcast receiver to check status of GPS */
    private BroadcastReceiver gpsLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //If Action is Location
            if (intent.getAction().matches(BROADCAST_ACTION)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                //Check if GPS is turned ON or OFF
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Log.e("About GPS", "GPS is Enabled in your device.");
                    updateStatus("GPS is Enabled in your device.");
                } else {
                    if (!isRequestedForPermission) {
                        //If GPS turned OFF show Location Dialog
                        new Handler().postDelayed(sendUpdatesToUI, 10);
                    }
                    // showLocationSettingDialog();
                    updateStatus("GPS is Disabled in your device.");
                    Log.e("About GPS", "GPS is Disabled in your device.");
                }

            }
        }
    };

    /*************************************************
     * Google place search
     * ***********************************************/

    public void doPlaceSearch(GoogleMap googleMap, PlaceSelectionListener placeSelectionListener) {
        try {
            isRequestedForPlaceSearch = true;
            placeSearchGoogleMap = googleMap;
            mPlaceSelectionListener = placeSelectionListener;
            Intent intent = new PlaceAutocomplete.IntentBuilder
                    (PlaceAutocomplete.MODE_OVERLAY)
                    .setBoundsBias(googleMap.getProjection().getVisibleRegion().latLngBounds)
                    .build(BaseMapActivity.this);
            startActivityForResult(intent, INTENT_PLACE_SEARCH);
        } catch (GooglePlayServicesRepairableException |
                GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    /*************************************************
     * Common methods for all
     * ***********************************************/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case SETTINGS_LOCATION:
                isRequestedForLocationSetting = false;
                switch (resultCode) {
                    case RESULT_OK:
                        Log.e("Settings", "Result OK");
                        updateStatus("GPS is Enabled in your device.");
                        //startLocationUpdates();
                        onGoogleClientApiConnected();
                        break;
                    case RESULT_CANCELED:
                        Log.e("Settings", "Result Cancel");
                        updateStatus("GPS is Disabled in your device.");
                        break;
                }
                break;

            case INTENT_PLACE_SEARCH:
                isRequestedForPlaceSearch = false;
                switch (resultCode) {
                    case RESULT_OK:
                        Place place = PlaceAutocomplete.getPlace(this, data);
                        LatLng placeLatLng = place.getLatLng();
                        if (mPlaceSelectionListener != null && placeSearchGoogleMap != null) {
                            mPlaceSelectionListener.onPlaceSelected(place);
                            animateCamera(placeSearchGoogleMap, placeLatLng);
                        }
                        break;
                    case PlaceAutocomplete.RESULT_ERROR:
                        Status status = PlaceAutocomplete.getStatus(this, data);
                        if (mPlaceSelectionListener != null) {
                            mPlaceSelectionListener.onError(status);
                            updateStatus("Place selection failed: " + status.getStatusMessage());
                        }
                        break;
                }
                break;
        }
    }

    //Method to update GPS status text
    private void updateStatus(String status) {
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }

    public void animateCamera(GoogleMap googleMap, LatLng selectedLatLng) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 16));
    }
}


