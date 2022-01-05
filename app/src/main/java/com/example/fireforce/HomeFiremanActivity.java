package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class HomeFiremanActivity extends AppCompatActivity {
    Socket mSocket;
    TextView placeText;
    SQLiteDatabase mydb;
    Switch firemanSwitch;
    TextView firemanStatusDetail;
//    Button checkButton;
    Button execButton;
    ImageButton settingButton;
    Boolean isOnFire = false;
    String demand = "";
    Cursor result;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_fireman);
        getSupportActionBar().hide();

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);

        placeText = findViewById(R.id.place_text_fireman);
        firemanSwitch = findViewById(R.id.fireman_switch);
        firemanStatusDetail = findViewById(R.id.fireman_status_detail);
//        checkButton = findViewById(R.id.fireman_check_button);
        execButton = findViewById(R.id.fireman_execute_button);
        settingButton = findViewById(R.id.fireman_setting);

        result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();
        placeText.setText(result.getString(2));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLocation();

        final Handler handlerOne = new Handler();
        handlerOne.postDelayed(new Runnable() {
            @Override
            public void run() {
                Cursor result1 = mydb.rawQuery("select * from place", null);
                result1.moveToFirst();
                if(result1.getInt(5) > 0 && result1.getInt(4) > 0){
                    fetchLocation();
                    JSONObject dataB = new JSONObject();
                    try {
                        dataB.put("latitude", currentLocation.getLatitude());
                        dataB.put("longitude", currentLocation.getLongitude());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mSocket.emit("firemanStreamLocation", dataB);
                }
                handlerOne.postDelayed(this, 5000);
            }
        }, 5000);



        JSONObject dataA = new JSONObject();
        try {
            dataA.put("id", result.getString(0));
            dataA.put("token", result.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("firemanRequestStatus", dataA);
        mSocket.on("firemanStatusResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                HomeFiremanActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        Boolean status = false;
                        try {
                            status = data.getBoolean("status");
                            demand = data.getString("demand");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(status){
                            isOnFire = true;
                        }else{
                            isOnFire = false;
                        }
                        if(firemanSwitch.isChecked()){
                            makeEnable();
                        }
                    }
                });
            }
        });

        if(result.getInt(4) > 0){
            firemanSwitch.setChecked(true);
            makeEnable();
        }else{
            firemanSwitch.setChecked(false);
            makeDisable();
        }
        firemanSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    mydb.execSQL("update place set switch=?", new Boolean[]{true});
                    makeEnable();
                }else{
                    mydb.execSQL("update place set switch=?", new Boolean[]{false});
                    makeDisable();
                }
            }
        });

//        checkButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });

        execButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MapsFiremanActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SettingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

    }

    private void makeDisable(){
//        checkButton.setVisibility(View.INVISIBLE);
        FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/FireSmokeDetected-" + result.getString(0));
        execButton.setVisibility(View.INVISIBLE);
        firemanStatusDetail.setText("Anda Tidak Aktif");
    }
    private void makeEnable(){
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/FireSmokeDetected-" + result.getString(0));

        if(isOnFire){
//            checkButton.setVisibility(View.VISIBLE);
            execButton.setVisibility(View.VISIBLE);
            firemanStatusDetail.setText("Terjadi Kebakaran");
        }else{
//            checkButton.setVisibility(View.INVISIBLE);
            execButton.setVisibility(View.INVISIBLE);
            firemanStatusDetail.setText("Kondisi Aman");
        }
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
//                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.myMap);
//                    assert supportMapFragment != null;
//                    supportMapFragment.getMapAsync(MainActivity.this);
                }
            }
        });
    }
}