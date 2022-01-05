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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.fireforce.databinding.ActivityMapsFiremanBinding;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MapsFiremanActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsFiremanBinding binding;
    private Socket mSocket;
    private SQLiteDatabase mydb;
    private TextView distanceRemaining;
    private TextView buildingName;
    private TextView buildingAddress;
    private TextView timeRemaining;
    private Button fireExtinguished;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    int stateZoom = 0;
    int stateZoom1 = 0;

    final Handler handlerOne = new Handler();

    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsFiremanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//         Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mSocket.connect();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);
        Cursor result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();

        distanceRemaining = findViewById(R.id.fireman_distance_remaining);
        buildingName = findViewById(R.id.fireman_building_name);
        buildingAddress = findViewById(R.id.fireman_building_address);
        timeRemaining = findViewById(R.id.fireman_time_remaining);
        fireExtinguished = findViewById(R.id.fireman_fire_extinguished);

        mydb.execSQL("update place set exec=?", new Boolean[]{true});

        JSONObject dataA = new JSONObject();
        try {
            dataA.put("id", result.getString(0));
            dataA.put("token", result.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("firemanRequestClient", dataA);
        mSocket.on("firemanResultClient", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MapsFiremanActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            buildingName.setText(data.getString("name"));
                            buildingAddress.setText(data.getString("address"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLocation();


        handlerOne.postDelayed(asd, 10000);


        fireExtinguished.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = new EditText(MapsFiremanActivity.this);
                input.setSingleLine();
                FrameLayout container = new FrameLayout(MapsFiremanActivity.this);
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = 45;
                params.rightMargin = 45;
                input.setLayoutParams(params);
                container.addView(input);

                new AlertDialog.Builder(MapsFiremanActivity.this)
                        .setTitle("Api Sudah Dipadamkan?")
                        .setMessage("Jika Api sudah benar-benar dipadamkan, mohon mengetik ulang \"Api Sudah Dipadamkan\"")
                        .setView(container)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Editable value = input.getText();
                                if(String.valueOf(value).equals("Api Sudah Dipadamkan")){
                                    Toast.makeText(MapsFiremanActivity.this, String.valueOf(value), Toast.LENGTH_SHORT).show();
                                    mSocket.emit("firemanFireExtinguished", dataA);
                                    mSocket.on("firemanFireExtinguishedResult", new Emitter.Listener() {
                                        @Override
                                        public void call(Object... args) {
                                            MapsFiremanActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(MapsFiremanActivity.this, "Api Sudah Dipadamkan", Toast.LENGTH_SHORT).show();
                                                    mydb.execSQL("update place set exec=?", new Boolean[]{false});
                                                    startActivity(new Intent(MapsFiremanActivity.this, HomeFiremanActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                                    finish();
                                                }
                                            });
                                        }
                                    });
                                }else{
                                    Toast.makeText(MapsFiremanActivity.this, "Input yang anda masukkan salah", Toast.LENGTH_SHORT).show();
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

        mSocket.on("firemanStreamLocationResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MapsFiremanActivity.this.runOnUiThread(new Runnable() {
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

    @Override
    public void onBackPressed() {
        handlerOne.removeCallbacks(asd);
        startActivity(new Intent(getApplicationContext(), HomeFiremanActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
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
//                    Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();

                    if(stateZoom1 == 0){
                        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                        assert supportMapFragment != null;
                        supportMapFragment.getMapAsync(MapsFiremanActivity.this);

                        Cursor result1 = mydb.rawQuery("select * from place", null);
                        result1.moveToFirst();
                        if(result1.getInt(5) > 0 && result1.getInt(4) > 0){
                            JSONObject dataB = new JSONObject();
                            try {
                                dataB.put("id", result1.getString(0));
                                dataB.put("token", result1.getString(3));
                                dataB.put("latitude", currentLocation.getLatitude());
                                dataB.put("longitude", currentLocation.getLongitude());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            mSocket.emit("firemanStreamLocation", dataB);
                        }

                        stateZoom1++;
                    }
                }
            }
        });
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    private Runnable asd = new Runnable() {
        @Override
        public void run() {
            fetchLocation();
            Cursor result1 = mydb.rawQuery("select * from place", null);
            result1.moveToFirst();
            if(result1.getInt(5) > 0 && result1.getInt(4) > 0){

                JSONObject dataB = new JSONObject();
                try {
                    dataB.put("id", result1.getString(0));
                    dataB.put("token", result1.getString(3));
                    dataB.put("latitude", currentLocation.getLatitude());
                    dataB.put("longitude", currentLocation.getLongitude());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mSocket.emit("firemanStreamLocation", dataB);
            }
            handlerOne.postDelayed(this, 5000);
        }
    };
}