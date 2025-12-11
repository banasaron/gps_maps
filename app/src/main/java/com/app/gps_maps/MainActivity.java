package com.app.gps_maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.CarrierConfigManager;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity implements LocationListener{
    private static final String TAG = "main--";
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 2;
    private static final int MY_PERMISSION_ACCESS_INTERNET = 3;
    private static final int MY_PERMISSION_ACCESS_SEND_SMS = 4;
    TextView bestprovider;
    TextView longitude;
    TextView latitude;
    TextView archivaldata;
    LocationManager locationManager;
    Criteria criteria;
    Location location;
    String bp;
    private int amount;

    //  osm  ----------------------------------------------------------------------------
    MapView osm;
    MapController mapController;
    SwipeRefreshLayout swipeRefreshLayout;
    TextView text_network;
    TextView text_gps;
    // fab menu
    FabMenu fabMenu;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = findViewById(R.id.refreshLayout);
        text_network = findViewById(R.id.text_network);
        text_gps = findViewById(R.id.text_gps);
        swipeRefreshLayout.setOnRefreshListener(()->{
            swipeRefreshLayout.setRefreshing(false);
            boolean cnn = isNetworkAvailable();
            boolean gps = isGpsAvailable();
            if(cnn){
                text_network.setText("NETWORK ON");
                text_network.setTextColor(Color.GREEN);
            }else {
                text_network.setText("NETWORK OFF");
                text_network.setTextColor(Color.RED);
            }
            if(gps){
                text_gps.setText("GPS ON");
                text_gps.setTextColor(Color.GREEN);
            }else {
                text_gps.setText("GPS OFF");
                text_gps.setTextColor(Color.RED);
            }
        });
        swipeRefreshLayout.setColorSchemeColors(getColor(R.color.secondary));

        bestprovider = findViewById(R.id.bestprovider);
        longitude = findViewById(R.id.longitude);
        latitude = findViewById(R.id.latitude);
        archivaldata = findViewById(R.id.archival_data);

        criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        bp = locationManager.getBestProvider(criteria, true);
        final String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.SEND_SMS
        };
        final int[] codes = {
                MY_PERMISSION_ACCESS_FINE_LOCATION,
                MY_PERMISSION_ACCESS_COARSE_LOCATION,
                MY_PERMISSION_ACCESS_INTERNET,
                MY_PERMISSION_ACCESS_SEND_SMS
        };
        for(int i=0; i<permissions.length; i++){
            if(ActivityCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[] {permissions[i]}, codes[i]);

            }
        }

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
//            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_ACCESS_COARSE_LOCATION);
//            return;
//        }
        location = locationManager.getLastKnownLocation(bp);
        Log.d(TAG, String.valueOf(location));
        locationManager.requestLocationUpdates(
                ""+bp,
                500,
                0.5f,
                (LocationListener) this
        );

        bestprovider.setText("Best provider: "+bp);
        longitude.setText("Longitude: "+location.getLongitude());
        latitude.setText("Latitude: "+location.getLatitude());
        archivaldata.setText("Measurement readings:\n\n");
        Log.d("GPSA", bp+" "+location.getLongitude()+ " "+location.getLatitude());

//part2
        osm = findViewById(R.id.osm);
        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        osm.setTileSource(TileSourceFactory.MAPNIK);
        osm.setBuiltInZoomControls(true);
        osm.setMultiTouchControls(true);

        mapController = (MapController) osm.getController();
        mapController.setZoom(12);

        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapController.setCenter(geoPoint);
        mapController.animateTo(geoPoint);


        addMarkerToMap(geoPoint);
        //fab menu

        fabMenu = new FabMenu(this, findViewById(R.id.mainFab),
                new FloatingActionButton[]{findViewById(R.id.fabSendSms), findViewById(R.id.fabSaveCordinates),
                        findViewById(R.id.fabShareCordinates), findViewById(R.id.fabWeather)},
                new LinearLayout[]{findViewById(R.id.layout1), findViewById(R.id.layout2),
                        findViewById(R.id.layout3), findViewById(R.id.layout4)});

        fabMenu.setMeasurementsInFabMenuToShare(location.getLongitude(), location.getLatitude());
        fabMenu.setSmsPermissionGranted(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
        fabMenu.setOsm(osm);

        osm.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                Log.i("GPSA", "onScroll()");
                fabMenu.setOsm(osm);
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                Log.i("GPSA", "onZoom()");
                fabMenu.setOsm(osm);
                return false;
            }
        });
//        TextView modeltextview =
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                if (permissions[0].equalsIgnoreCase(Manifest.permission.ACCESS_FINE_LOCATION)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d("GPSA", "uprawnien"+requestCode+" "+permissions[0]+ " "+grantResults[0]);
                    Log.d(TAG, "permission access fine location was grated");
                    Toast.makeText(this, "permission access fine location was grated", Toast.LENGTH_SHORT).show();
                    this.recreate();
                }else{
                    Log.d(TAG, "permission access fine location was denied");
                    Toast.makeText(this, "permission access fine location was denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                if (permissions[0].equalsIgnoreCase(Manifest.permission.ACCESS_COARSE_LOCATION)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d("GPSA", "uprawnien"+requestCode+" "+permissions[0]+ " "+grantResults[0]);
                    Log.d(TAG, "permission access coarse location was grated");
                    Toast.makeText(this, "permission access coarse location was grated", Toast.LENGTH_SHORT).show();
                    this.recreate();
                }else{
                    Log.d(TAG, "permission access coarse location was denied");
                    Toast.makeText(this, "permission access coarse location was denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case MY_PERMISSION_ACCESS_SEND_SMS: {
                if(permissions[0].equalsIgnoreCase(Manifest.permission.SEND_SMS)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    fabMenu.setSmsPermissionGranted(true);
                    Log.d(TAG, "permission send sms was granted");
                    Toast.makeText(this, "permission send sms was granted", Toast.LENGTH_SHORT).show();
                    this.recreate();
                }else{
                    fabMenu.setSmsPermissionGranted(false);
                    Log.d(TAG, "permission send sms was denied");
                    Toast.makeText(this, "permission send sms was denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default: Log.d(TAG, "another permission");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetwork() != null;
    }
    private boolean isGpsAvailable() {
//        LocationManager connectivityManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.getBestProvider(new Criteria(), true) != null;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationChanged(@NonNull Location location) {
        bp = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED){
            location = locationManager.getLastKnownLocation(bp);
            bestprovider.setText("Best provider: "+bp);
            longitude.setText("Longitude: "+location.getLongitude());
            latitude.setText("Latitude: "+location.getLatitude());
            archivaldata.setText(archivaldata.getText()+" "+location.getLongitude()+" "+location.getLatitude()+"\n");
            amount++;
            fabMenu.setMeasurementsInFabMenuToShare(location.getLongitude(), location.getLatitude());
            Log.d("GPSA", amount+ " pomiar: "+bp+" "+location.getLongitude()+ " "+ location.getLatitude());
        }
    }

    private void addMarkerToMap(GeoPoint geoPoint) {
        Marker marker = new Marker(osm);
        marker.setPosition(geoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.baseline_gps_fixed_24));
        osm.getOverlays().clear();
        osm.getOverlays().add(marker);
        osm.invalidate();
        marker.setTitle("my position");
    }
}