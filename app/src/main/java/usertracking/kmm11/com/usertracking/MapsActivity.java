package usertracking.kmm11.com.usertracking;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Debug;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.collect.ImmutableMap;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.presence.PNHereNowResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LinearLayout mLinearLayout;
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Circle myCircle;
    private Circle frCircle;
    private Circle controlCircle;
    private Marker myMarker;
    private boolean controlModeOn;
    private PubNub mPubnub_DataStream;
    private PubSubPnCallback mPubSubPnCallback;
    private float allowedDistance;
    private ArrayList<LatLng> latLngsCircle;

    private static final String TAG = "LocationActivity";
    private static final long INTERVAL = 1000;
    private static final long FASTEST_INTERVAL = 500;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 14;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_exit:
                exit();
                return true;
            case R.id.action_add_control:
                addControl();
                return true;
            case R.id.action_delete_control:
                deleteControl();
                return true;
            case R.id.action_find:
                find();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maps, menu);
        return true;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (this.latLngsCircle != null) {
            outState.putParcelableArrayList("latLngsCircle", this.latLngsCircle);
        }
        if (allowedDistance > 0){
            outState.putFloat("allowedDistance", this.allowedDistance);
        }
        outState.putBoolean("controleModeOn", this.controlModeOn);
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        this.mLinearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        this.mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.mPubSubPnCallback = new PubSubPnCallback(this);
        this.controlModeOn = false;
        initPubNub();
        initChannels();
        createLocationRequest();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                updateMap(location.getLatitude(), location.getLongitude(), false);
                if (Constants.MODE == "share" && mPubnub_DataStream != null) {
                    publish(location.getLatitude(), location.getLongitude());
                }
            }
        };
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar snackbar = Snackbar
                    .make(this.findViewById(R.id.map), "There is no permission", Snackbar.LENGTH_LONG);
            snackbar.setAction("allow", new MapsActivity.MyUndoListener());
            snackbar.show();
        }
        this.latLngsCircle = new ArrayList<>();
        this.latLngsCircle.add(new LatLng(0,0));
        this.latLngsCircle.add(new LatLng(0,0));
        this.latLngsCircle.add(new LatLng(0,0));

        if(savedInstanceState!=null){
            if(savedInstanceState.containsKey("latLngsCircle")) {
                this.latLngsCircle = savedInstanceState.getParcelableArrayList("latLngsCircle");
            }
            if (savedInstanceState.containsKey("allowedDistance")){
                this.allowedDistance = savedInstanceState.getFloat("allowedDistance");
            }
            if (savedInstanceState.containsKey("controleModeOn")){
                this.controlModeOn = savedInstanceState.getBoolean("controleModeOn");
            }
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if (!checkPermissions()) {
            startLocationUpdates();
            requestPermissions();
        } else {
            getLastLocation();
            startLocationUpdates();
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar snackbar = Snackbar
                    .make(this.findViewById(R.id.linearLayout), "There is no permission", Snackbar.LENGTH_LONG);
            snackbar.setAction("allow", new MapsActivity.MyUndoListener());
            snackbar.show();

        } else {
            Log.i(TAG, "Requesting permission");
            startLocationPermissionRequest();
        }
    }
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }
    protected void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        updateMap(location.getLatitude(), location.getLongitude(), false);
                    }
                });
    }
    private class MyUndoListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 0);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        CircleOptions myCircleOptions = new CircleOptions()
                .radius(50)
                .fillColor(Color.YELLOW).strokeColor(Color.DKGRAY)
                .strokeWidth(2);
        if (this.latLngsCircle != null && this.latLngsCircle.get(0) != null){
            myCircleOptions.center(this.latLngsCircle.get(0));
        }
        else{
            myCircleOptions.center(new LatLng(0,0));
        }
        myCircle = mMap.addCircle(myCircleOptions);

        CircleOptions frCircleOptions = new CircleOptions()
                .radius(50)
                .fillColor(Color.BLUE).strokeColor(Color.DKGRAY)
                .strokeWidth(2);
        if (this.latLngsCircle != null && this.latLngsCircle.get(1) != null){
            frCircleOptions.center(this.latLngsCircle.get(1));
        }
        else {
            frCircleOptions.center(new LatLng(0,0));
        }

        if (Constants.MODE == "share"){
            frCircleOptions.visible(false);
        }
        frCircle = mMap.addCircle(frCircleOptions);
        if (this.controlModeOn && this.latLngsCircle != null && this.latLngsCircle.get(2) != null) {
            MarkerOptions myMarkerOptions = new MarkerOptions()
                    .position(this.latLngsCircle.get(2)).draggable(true);
            myMarker = mMap.addMarker(myMarkerOptions);

            if (this.allowedDistance > 0){
                CircleOptions controlCircleOptions = new CircleOptions()
                        .center(myMarker.getPosition()).radius(allowedDistance)
                        .fillColor(0x220000FF).strokeColor(Color.DKGRAY)
                        .strokeWidth(2);
                controlCircle = mMap.addCircle(controlCircleOptions);
            }
        }
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if ((controlModeOn && myMarker == null) || (controlModeOn && myMarker.getPosition() == null)) {
                    MarkerOptions myMarkerOptions = new MarkerOptions()
                            .position(latLng).draggable(true);
                    myMarker = mMap.addMarker(myMarkerOptions);
                    latLngsCircle.set(2, latLng);
                }
            }
        });
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                double radius = calculateCircleRadiusMeterForMapCircle(12, myCircle.getCenter().latitude, mMap.getCameraPosition().zoom);
                myCircle.setRadius(radius);
                radius = calculateCircleRadiusMeterForMapCircle(12, frCircle.getCenter().latitude, mMap.getCameraPosition().zoom);
                float zoom = mMap.getCameraPosition().zoom;
                Log.v("f", "ZOOM = " + zoom);
                frCircle.setRadius(radius);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mLinearLayout.addView(createNewEditText());
                mLinearLayout.addView(createNewButton());
                mLinearLayout.setGravity(Gravity.CENTER);
                return false;
            }

            private Button createNewButton() {
                final Button button = new Button(getApplicationContext());
                button.setText("Ok");
                final LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                button.setLayoutParams(lparams);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            final EditText textView = (EditText) findViewById(R.id.edittext_for_distance);
                            allowedDistance = Float.parseFloat(textView.getText().toString());
                            if(((LinearLayout) mLinearLayout).getChildCount() > 0)
                                ((LinearLayout) mLinearLayout).removeAllViews();

                            // добавляем окружение
                            if (controlCircle == null) {
                                CircleOptions controlCircleOptions = new CircleOptions()
                                        .center(myMarker.getPosition()).radius(allowedDistance)
                                        .fillColor(0x220000FF).strokeColor(Color.DKGRAY)
                                        .strokeWidth(2);
                                controlCircle = mMap.addCircle(controlCircleOptions);
                            }
                            else {
                                controlCircle.setRadius(allowedDistance);
                            }
                        }
                        catch (NumberFormatException e) {
                        }
                    }
                });
                return button;
            }

            private EditText createNewEditText() {
                final LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                final EditText editText = new EditText(getApplicationContext());
                editText.setLayoutParams(lparams);
                editText.setId(R.id.edittext_for_distance);
                editText.setText("1000");
                editText.setGravity(Gravity.CENTER);
                editText.setBackgroundColor(Color.parseColor("#44FFEC23"));
                Log.v("f", "text view was created");
                return editText;
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                latLngsCircle.set(2, marker.getPosition());
                if (controlCircle != null) {
                    controlCircle.setCenter(marker.getPosition());
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
            }
        });

    }
    public void updateMap(final double latitude, final double longitude, boolean flag) {
        synchronized (mMap) {
            if (flag) {
                LatLng latLng = new LatLng(latitude, longitude);
                frCircle.setCenter(latLng);
                double radius = calculateCircleRadiusMeterForMapCircle(12, frCircle.getCenter().latitude, mMap.getCameraPosition().zoom);
                frCircle.setRadius(radius);
                this.latLngsCircle.set(1, new LatLng(latitude, longitude));
                if (controlModeOn && myMarker != null && myMarker.getPosition() != null && calculateDistance(latitude, longitude)){
                    frCircle.setFillColor(Color.RED);
                }
                else {
                    frCircle.setFillColor(Color.BLUE);
                }
            }
            else {
                myCircle.setCenter(new LatLng(latitude, longitude));
                double radius = calculateCircleRadiusMeterForMapCircle(12, myCircle.getCenter().latitude, mMap.getCameraPosition().zoom);
                myCircle.setRadius(radius);
                this.latLngsCircle.set(0, new LatLng(latitude, longitude));
            }
        }
    }

    private double calculateCircleRadiusMeterForMapCircle(final int _targetRadiusDip, final double _circleCenterLatitude,
                                                          final float _currentMapZoom) {
        final double arbitraryValueForDip = 156000D;
        final double oneDipDistance = Math.abs(Math.cos(Math.toRadians(_circleCenterLatitude))) * arbitraryValueForDip / Math.pow(2, _currentMapZoom);
        return oneDipDistance * (double) _targetRadiusDip;
    }

    private void publish(double latitude, double longitude) {

        final Map<String, String> message = ImmutableMap.<String, String>of("latitude", Double.toString(latitude), "longitude", Double.toString(longitude), "name", Constants.USERNAME);

        MapsActivity.this.mPubnub_DataStream.publish().channel(Constants.CHANNEL_NAME).message(message).async(
                new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        try {
                            if (!status.isError()) {
                                Log.v("PUBLISH", "publish done");
                            } else {
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

    }

    private boolean calculateDistance(double latitude, double longitude){
        Location markerLocation = new Location("");
        markerLocation.setLatitude(myMarker.getPosition().latitude);
        markerLocation.setLongitude(myMarker.getPosition().longitude);
        Location currentLocation = new Location("");
        currentLocation.setLatitude(latitude);
        currentLocation.setLongitude(longitude);
        if (markerLocation.distanceTo(currentLocation) <= this.allowedDistance || allowedDistance <= 0){
            return false;
        }
        else {
            return true;
        }
    }

    private final void initPubNub() {
        PNConfiguration config = new PNConfiguration();

        config.setPublishKey(Constants.PUBNUB_PUBLISH_KEY);
        config.setSubscribeKey(Constants.PUBNUB_SUBSCRIBE_KEY);
        config.setUuid(Constants.USERNAME);
        config.setSecure(true);

        this.mPubnub_DataStream = new PubNub(config);
    }

    private final void initChannels() {
        this.mPubnub_DataStream.addListener(this.mPubSubPnCallback);
        this.mPubnub_DataStream.subscribe().channels(Arrays.asList(Constants.CHANNEL_NAME)).withPresence().execute();
        this.mPubnub_DataStream.hereNow().channels(Arrays.asList(Constants.CHANNEL_NAME)).async(new PNCallback<PNHereNowResult>() {
            @Override
            public void onResponse(PNHereNowResult result, PNStatus status) {
                if (status.isError()) {
                    return;
                }
            }
        });
    }

    private void exit(){
        stopLocationUpdates();
        disconnectAndCleanup();
        Intent toLogin = new Intent(this, LoginActivity.class);
        startActivity(toLogin);
    }

    private void addControl(){
        if (Constants.MODE == "listen"){
            this.controlModeOn = true;
        }
    }

    private void deleteControl(){
        if (Constants.MODE == "listen") {
            if (myMarker != null) {
                myMarker.remove();
                myMarker = null;
            }
            if (controlCircle != null) {
                controlCircle.remove();
                controlCircle = null;
            }
            this.controlModeOn = false;
        }
    }
    private void find(){
        if (Constants.MODE == "listen") {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(frCircle.getCenter()));
        }
        else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(myCircle.getCenter()));
        }
    }
    private void disconnectAndCleanup() {
        getSharedPreferences(Constants.DATASTREAM_PREFS, MODE_PRIVATE).edit().clear().commit();

        if (this.mPubnub_DataStream != null) {
            this.mPubnub_DataStream.unsubscribe().channels(Arrays.asList(Constants.CHANNEL_NAME)).execute();
            this.mPubnub_DataStream.disconnect();
            this.mPubnub_DataStream = null;
        }
    }
}
