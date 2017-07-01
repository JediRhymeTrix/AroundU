package fun.hye.aroundu;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.widget.SearchView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ListView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Map;

public class Landing extends FragmentActivity implements GeoQueryEventListener {

    private static final GeoLocation INITIAL_CENTER = new GeoLocation(40.7589, -73.9851);//(56.83069, 1.94822);
    private static final int INITIAL_ZOOM_LEVEL = 14;
    private static final String GEO_FIRE_DB = "https://aroundu-18e5d.firebaseio.com";
    private static final String GEO_FIRE_REF = GEO_FIRE_DB + "/-KnxyWkJziPBgDq3Akgk" +
            " ";

    private GoogleMap map;
    private Circle searchCircle;
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    SupportMapFragment mapFragment;

    private Map<String, Marker> markers;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            setContentView(R.layout.activity_landing);
            SearchView se = (SearchView) findViewById(R.id.search);
            se.setBackgroundColor(Color.rgb(53, 53, 53));
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.

            listView = (ListView) findViewById(R.id.map);

            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    map = googleMap;
                    some();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void some() {
        try {
            LatLng latLngCenter = new LatLng(INITIAL_CENTER.latitude, INITIAL_CENTER.longitude);
            this.searchCircle = this.map.addCircle(new CircleOptions().center(latLngCenter).radius(1000));
            this.searchCircle.setFillColor(Color.argb(66, 255, 0, 255));
            this.searchCircle.setStrokeColor(Color.argb(66, 0, 0, 0));
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));

            FirebaseOptions options = new FirebaseOptions.Builder().setApiKey("AIzaSyB0hqpv_NNDJ3vZSdH1_knV5bBYztiCIpY").setApplicationId("1:76997838740:android:14fa7cbc50e02afd").setDatabaseUrl(GEO_FIRE_DB).build();
            FirebaseApp app = FirebaseApp.initializeApp(this, options, "fun.hye.aroundu");

            // setup GeoFire
            //this.geoFire = new GeoFire(FirebaseDatabase.getInstance(app).getReferenceFromUrl(GEO_FIRE_REF));

            // radius in km
            //this.geoQuery = this.geoFire.queryAtLocation(INITIAL_CENTER, 1);

            // setup markers
            this.markers = new HashMap<>();
            //this.geoQuery.addGeoQueryEventListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // remove all event listeners to stop updating in the background
        this.geoQuery.removeAllListeners();
        for (Marker marker : this.markers.values()) {
            marker.remove();
        }
        this.markers.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // add an event listener to start updating locations again

    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        // Add a new marker to the map
        Marker marker = this.map.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)));
        this.markers.put(key, marker);
    }

    @Override
    public void onKeyExited(String key) {
        // Remove any old marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            marker.remove();
            this.markers.remove(key);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        // Move the marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            this.animateMarkerTo(marker, location.latitude, location.longitude);
        }
    }

    @Override
    public void onGeoQueryReady() {
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("There was an unexpected error querying GeoFire: " + error.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Animation handler for old APIs without animation support
    private void animateMarkerTo(final Marker marker, final double lat, final double lng) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long DURATION_MS = 3000;
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final LatLng startPosition = marker.getPosition();
        handler.post(new Runnable() {
            @Override
            public void run() {
                float elapsed = SystemClock.uptimeMillis() - start;
                float t = elapsed / DURATION_MS;
                float v = interpolator.getInterpolation(t);

                double currentLat = (lat - startPosition.latitude) * v + startPosition.latitude;
                double currentLng = (lng - startPosition.longitude) * v + startPosition.longitude;
                marker.setPosition(new LatLng(currentLat, currentLng));

                // if animation is not finished yet, repeat
                if (t < 1) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }
}