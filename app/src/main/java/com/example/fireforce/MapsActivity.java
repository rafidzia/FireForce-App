package com.example.fireforce;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.fireforce.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
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
    int stateZoom = 0;
    Cursor result;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;

    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mSocket.connect();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);
        result = mydb.rawQuery("select * from place", null);
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
                        JSONObject data = (JSONObject) args[0];
                        try {
                            String name = data.getString("name");
                            firemanName.setText("Pemadam " + name);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLocation();

        mSocket.on("userFiremanDone", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MapsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(getApplicationContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                    }
                });
            }
        });

        fireExtinguished.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = new EditText(MapsActivity.this);
                input.setSingleLine();
                FrameLayout container = new FrameLayout(MapsActivity.this);
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = 45;
                params.rightMargin = 45;
                input.setLayoutParams(params);
                container.addView(input);

                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("Api Sudah Dipadamkan?")
                        .setMessage("Jika Api sudah benar-benar dipadamkan, mohon mengetik ulang \"Api Sudah Dipadamkan\"")
                        .setView(container)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Editable value = input.getText();
                                if(String.valueOf(value).equals("Api Sudah Dipadamkan")){
                                    Toast.makeText(MapsActivity.this, String.valueOf(value), Toast.LENGTH_SHORT).show();
                                    mSocket.emit("userFireExtinguished", dataA);
                                    mSocket.on("firemanFireExtinguishedResult", new Emitter.Listener() {
                                        @Override
                                        public void call(Object... args) {
                                            MapsActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(MapsActivity.this, "Api Sudah Dipadamkan", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(MapsActivity.this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                                    finish();
                                                }
                                            });
                                        }
                                    });
                                }else{
                                    Toast.makeText(MapsActivity.this, "Input yang anda masukkan salah", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).create().show();


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
        if(stateZoom == 0){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 14));
        }

        mSocket.on("firemanStreamLocationResult-" + result.getString(0), new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MapsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject dataC = (JSONObject) args[0];
                        Double duration = 0.0;
                        Double distance = 0.0;
                        JSONArray coordinates = new JSONArray();
                        try {
                            duration = dataC.getDouble("duration");
                            distance = dataC.getDouble("distance");
                            coordinates = dataC.getJSONArray("coordinates");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        distanceRemaining.setText(String.valueOf((float)(distance/1000)) + " km");
                        timeRemaining.setText("Estimated " + String.valueOf((float)(duration/60)) + " mins");

                        mMap.clear();
                        JSONArray origin = coordinates.optJSONArray(0);
                        JSONArray destination = coordinates.optJSONArray(coordinates.length() - 1);
                        try {
                            LatLng selfLocation = new LatLng(origin.getDouble(1), origin.getDouble(0));
                            mMap.addMarker(new MarkerOptions().position(selfLocation).title("Self Location")).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_asd));
                            if(stateZoom == 0){
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selfLocation, 15));
                                stateZoom++;
                            }

                            LatLng targetLocation = new LatLng(destination.getDouble(1), destination.getDouble(0));
                            mMap.addMarker(new MarkerOptions().position(targetLocation).title("Target Location"));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        PolylineOptions polyopt = new PolylineOptions();

                        for(int i = 0; i < coordinates.length(); i++){
                            JSONArray polylatlng = coordinates.optJSONArray(i);
                            try {
                                polyopt.add(new LatLng(polylatlng.getDouble(1), polylatlng.getDouble(0)));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        polyopt.width(8f);
                        polyopt.color(R.color.purple_500);
                        mMap.addPolyline(polyopt);
                    }
                });
            }
        });

    }

    private void fetchLocation(){
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    assert supportMapFragment != null;
                    supportMapFragment.getMapAsync(MapsActivity.this);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}