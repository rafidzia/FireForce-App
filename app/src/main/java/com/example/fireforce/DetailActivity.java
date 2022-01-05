package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DetailActivity extends AppCompatActivity {
    Socket mSocket;
    RelativeLayout detailContainer;
    RelativeLayout.LayoutParams p;
    TextView floorId;
    HorizontalScrollView hv;
    LinearLayout ll;
    TextView roomInfo;
    SQLiteDatabase mydb;
    Boolean isEmerge = false;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        getSupportActionBar().hide();

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);
        Cursor result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();

        isEmerge = getIntent().getBooleanExtra("emerge", false);

        detailContainer = findViewById(R.id.detail_container);

        JSONObject dataA = new JSONObject();
        try {
            dataA.put("id", result.getString(0));
            dataA.put("token", result.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("userRequestDetail", dataA);
        mSocket.on("userDetailResult-" + result.getString(0), new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                DetailActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray data = (JSONArray) args[0];
                        runDefragRoom(data);
                    }
                });
            }
        });

//        detailContainer.removeAllViews();
//
//        floorId = new TextView(this);
//        p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        p.addRule(RelativeLayout.BELOW, R.id.hs3);
//        p.leftMargin = 20;
//        p.bottomMargin = -20;
//        floorId.setTextSize(15);
//        floorId.setText("Lt.1");
//        floorId.setId(View.generateViewId());
//        floorId.setLayoutParams(p);
//        detailContainer.addView(floorId);
//
//        hv = new HorizontalScrollView(this);
//        p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        p.addRule(RelativeLayout.BELOW, floorId.getId());
//        p.addRule(RelativeLayout.CENTER_HORIZONTAL);
//        hv.setHorizontalScrollBarEnabled(false);
//        hv.setSmoothScrollingEnabled(true);
//        hv.setLayoutParams(p);
//        hv.setId(View.generateViewId());
//
//        ll = new LinearLayout(this);
//        p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        ll.setOrientation(LinearLayout.HORIZONTAL);
//        ll.setPadding(10, 0, 10, 0);
//        ll.setLayoutParams(p);
//
//        roomInfo = new TextView(this);
//        p = new RelativeLayout.LayoutParams(140, 200);
//        roomInfo.setBackgroundResource(R.drawable.roundcorner);
//        roomInfo.setElevation(10);
//        p.setMargins(15,15, 15, 15);
//        roomInfo.setPadding(5, 5, 5, 5);
//        roomInfo.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
//        roomInfo.setText("Ruang A");
//        roomInfo.setLayoutParams(p);
//
//        ll.addView(roomInfo);
//
//        roomInfo = new TextView(this);
//        p = new RelativeLayout.LayoutParams(140, 200);
//        roomInfo.setBackgroundResource(R.drawable.roundcorner);
//        roomInfo.setElevation(10);
//        p.setMargins(15,15, 15, 15);
//        roomInfo.setPadding(5, 5, 5, 5);
//        roomInfo.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
//        roomInfo.setText("Ruang A");
//        roomInfo.setLayoutParams(p);
//
//        ll.addView(roomInfo);
//
//
//        hv.addView(ll);
//
//        detailContainer.addView(hv);

    }

    private void runDefragRoom(JSONArray data){
        HorizontalScrollView prevFloor = new HorizontalScrollView(this);
        detailContainer.removeAllViews();
        for(int i = data.length() - 1; i >= 0; i--){
            JSONObject data1 = new JSONObject();
            JSONArray data2 = new JSONArray();
            String floorText = "";
            try {
                data1 = data.getJSONObject(i);
                floorText = data1.getString("name").substring(1, data1.getString("name").length());
                data2 = data1.getJSONArray("data");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            floorId = new TextView(this);
            p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if(i < data.length() - 1){
                p.addRule(RelativeLayout.BELOW, prevFloor.getId());
            }
            p.leftMargin = 20;
            p.bottomMargin = -20;
            floorId.setTextSize(15);
            floorId.setText("Lt." + floorText);
            floorId.setId(View.generateViewId());
            floorId.setLayoutParams(p);
            detailContainer.addView(floorId);

            hv = new HorizontalScrollView(this);
            p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.addRule(RelativeLayout.BELOW, floorId.getId());
            p.addRule(RelativeLayout.CENTER_HORIZONTAL);
            hv.setHorizontalScrollBarEnabled(false);
            hv.setSmoothScrollingEnabled(true);
            hv.setLayoutParams(p);
            hv.setId(View.generateViewId());
            prevFloor = hv;

            ll = new LinearLayout(this);
            p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setPadding(10, 0, 10, 0);
            ll.setLayoutParams(p);


            for(int j = 0; j < data2.length(); j++){
                Integer roomStatus = 0;
                String roomName = "";
                try {
                    roomStatus = data2.getJSONArray(j).optInt(0);
                    roomName = data2.getJSONArray(j).optString(1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                roomInfo = new TextView(this);
                p = new RelativeLayout.LayoutParams(140, 200);
                roomInfo.setBackgroundResource(R.drawable.roundcorner);
                roomInfo.setElevation(10);
                p.setMargins(15,15, 15, 15);
                roomInfo.setPadding(5, 5, 5, 5);
                roomInfo.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                roomInfo.setMaxLines(2);
                if(roomName.length() > 10){
                    roomName = roomName.substring(0, 10) + "..";
                }
                roomInfo.setText(roomName + "\n");
                roomInfo.setLayoutParams(p);

                switch(roomStatus){
                    case 1:
                        roomInfo.setBackgroundResource(R.drawable.roundcornerfire);
                        roomInfo.setTextColor(getResources().getColor(R.color.white));
                        break;
                    case 2:
                        roomInfo.setBackgroundResource(R.drawable.roundcornersmoke);
                        roomInfo.setTextColor(getResources().getColor(R.color.white));
                        break;
                    case 3:
                        roomInfo.setBackgroundResource(R.drawable.roundcornerfiresmoke);
                        roomInfo.setTextColor(getResources().getColor(R.color.white));
                        break;
                }
                ll.addView(roomInfo);
            }
            hv.addView(ll);
            detailContainer.addView(hv);
        }
    }

    @Override
    public void onBackPressed() {
        if(!isEmerge){
            super.onBackPressed();
        }else{
            startActivity(new Intent(getApplicationContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }

    }
}