package com.example.fireforce;

import androidx.fragment.app.FragmentActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.fireforce.databinding.ActivityMapsBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private Socket mSocket;
    private SQLiteDatabase mydb;
    private TextView distanceRemaining;
    private TextView firemanName;
    private  TextView timeRemaining;
    private Button fireExtinguished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mSocket.connect();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);
        Cursor result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();

        distanceRemaining = findViewById(R.id.user_distance_remaining);
        firemanName = findViewById(R.id.user_fireman_name);
        timeRemaining = findViewById(R.id.user_time_remaining);
        fireExtinguished = findViewById(R.id.user_fire_extinguished);

        JSONObject dataA = new JSONObject();
        try {
            dataA.put("id", result.getString(0));
            dataA.put("token", result.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("userRequestFireman", dataA);
        mSocket.on("userResultFireman", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MapsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });



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

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-7.289346, 112.809688);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16));

    }


}