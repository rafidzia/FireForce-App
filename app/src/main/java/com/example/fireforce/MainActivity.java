package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private Socket mSocket;
    private int state = 0;
    private int nextOptions = 0;
    String typeOption = "";
    SQLiteDatabase mydb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);

        mydb.execSQL("CREATE TABLE IF NOT EXISTS place(id VARCHAR, option VARCHAR, name VARCHAR, token VARCHAR, switch BOOLEAN, exec BOOLEAN);");

        Cursor result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();
        if(result.getCount() > 0){
            typeOption = result.getString(1);
        }

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();
        mSocket.connect();
        mSocket.emit("enteringApp");
        mSocket.on("connected", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(result.getCount() > 0){
                            JSONObject data = new JSONObject();
                            try {
                                data.put("option", result.getString(1));
                                data.put("name", result.getString(2));
                                data.put("token", result.getString(3));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mSocket.emit("findPlace", data);
                            mSocket.on("findPlaceResult", placeResult);
                        }else{
                            state++;
                            if(state == 2){
                                moveToChoose();
                            }
                        }
                    }
                });
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                state++;
                if(state == 2){
                    if(nextOptions == 0){
                        moveToChoose();
                    }else if(nextOptions == 1){
                        if(typeOption.equals("user")){
                            moveToHome();
                        }else if(typeOption.equals("fireman")){
                            moveToHomeFireman();
                        }
                    }
                }
            }
        }, 2000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(state < 2){
                    Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
                    moveTaskToBack(true);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                }
            }
        }, 5000);
    }

    private void moveToHome(){
        startActivity(new Intent(MainActivity.this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
    private void moveToHomeFireman(){
        startActivity(new Intent(MainActivity.this, HomeFiremanActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
    private void moveToChoose(){
        startActivity(new Intent(MainActivity.this, ChooseActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private final Emitter.Listener placeResult = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Boolean status = false;
                    try {
                        status = data.getBoolean("status");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(status){
                        nextOptions = 1;
                        state++;
                        if(state == 2){
                            if(typeOption.equals("user")){
                                moveToHome();
                            }else if(typeOption.equals("fireman")){
                                moveToHomeFireman();
                            }
                        }
                    }else{
//                        moveToChoose();
                    }
                }
            });
        }
    };
}