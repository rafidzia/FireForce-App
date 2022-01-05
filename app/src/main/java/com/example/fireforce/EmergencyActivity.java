package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class EmergencyActivity extends AppCompatActivity {
    Socket mSocket;
    Button checkButton;
    Button firemanButton;
    TextView statusDetails;
    SQLiteDatabase mydb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);
        getSupportActionBar().hide();

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mSocket.connect();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);
        Cursor result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();


        JSONObject dataA = new JSONObject();
        try {
            dataA.put("id", result.getString(0));
            dataA.put("token", result.getString(3));
            dataA.put("floor", getIntent().getStringExtra("floor"));
            dataA.put("room", getIntent().getStringExtra("room"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("userSearchByFloorRoom", dataA);
        mSocket.on("userSearchByFloorRoomResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                EmergencyActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        String floor = "", room = "";
                        try {
                            floor = data.getString("floor");
                            room = data.getString("room");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        statusDetails.setText("Terjadi Kebakaran Lantai " + floor + " " + room);
                    }
                });
            }
        });

        checkButton = findViewById(R.id.warning_check_button);
        firemanButton = findViewById(R.id.warning_fireman_button);
        statusDetails = findViewById(R.id.warning_user_status_detail);

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), DetailActivity.class).putExtra("emerge", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

        firemanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MapsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }else{
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }



    }



    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();

    }
}