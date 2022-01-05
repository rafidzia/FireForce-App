package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class HomeActivity extends AppCompatActivity {
    Socket mSocket;
    TextView placeText;
    ImageButton settingButton;
    Button checkButton;
    Button checkFireman;
    ImageView statusIcon;
    TextView statusDetail;
    Integer conditions = 0;
    SQLiteDatabase mydb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getSupportActionBar().hide();

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);

        placeText = findViewById(R.id.place_text);
        statusIcon = findViewById(R.id.user_fire_status);

        Cursor result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();

        FirebaseMessaging.getInstance().subscribeToTopic("/topics/FireSmokeDetected-" + result.getString(0));
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/notify-" + result.getString(0));

        placeText.setText(result.getString(2));
        statusDetail = findViewById(R.id.user_status_detail);
        checkButton = findViewById(R.id.user_check_button);
        checkFireman = findViewById(R.id.user_fireman_button);

        settingButton = findViewById(R.id.user_setting);

        JSONObject dataA = new JSONObject();
        try {
            dataA.put("id", result.getString(0));
            dataA.put("token", result.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("userRequestStatus", dataA);
        mSocket.on("userStatusResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                HomeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray data = (JSONArray) args[0];
                        int maxdata = 0;
                        for (int i = 0; i < data.length(); i++) {
                            if (data.optInt(i) > maxdata) {
                                maxdata = data.optInt(i);

                            }
                            conditions = maxdata;
                            switch (conditions){
                                case 0:
                                    statusIcon.setImageResource(R.drawable.ic_baseline_check_circle_200);
                                    statusDetail.setText("Kondisi Aman");
//                                    checkButton.setVisibility(View.INVISIBLE);
                                    checkFireman.setVisibility(View.INVISIBLE);
                                    break;
                                case 1:
                                    statusIcon.setImageResource(R.drawable.ic_baseline_error_200);
                                    statusDetail.setText("Terdeteksi Api");
//                                    checkButton.setVisibility(View.VISIBLE);
                                    checkFireman.setVisibility(View.INVISIBLE);
                                    break;
                                case 2:
                                    statusIcon.setImageResource(R.drawable.ic_baseline_error_200);
                                    statusDetail.setText("Terdeteksi Asap");
//                                    checkButton.setVisibility(View.VISIBLE);
                                    checkFireman.setVisibility(View.INVISIBLE);
                                    break;
                                case 3:
                                    statusIcon.setImageResource(R.drawable.ic_baseline_error_200);
                                    statusDetail.setText("Terdeteksi Kebakaran");
//                                    checkButton.setVisibility(View.VISIBLE);
                                    checkFireman.setVisibility(View.VISIBLE);
                                    break;
                            }
                        }
                    }
                });
            }
        });

        mSocket.on("userConditionChange" + result.getString(0), new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                HomeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSocket.emit("userRequestStatus", dataA);
                    }
                });
            }
        });

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), DetailActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        checkFireman.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MapsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
}