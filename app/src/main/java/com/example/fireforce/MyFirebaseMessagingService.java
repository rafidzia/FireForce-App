package com.example.fireforce;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    public Map<String, String> data;
    private SQLiteDatabase mydb;
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        Log.d("from", remoteMessage.getFrom());
        String dataFrom = remoteMessage.getFrom().split("-")[1];
        Intent intentIn = null;

        if(!remoteMessage.getFrom().split("-")[0].equals("/topics/FireSmokeDetected")){
            return;
        }

//        Intent intentIn = new Intent("fcm-data-in");
        if(dataFrom.substring(0, 1).equals("C")){
            intentIn = new Intent(getApplicationContext(), EmergencyActivity.class);
//            Log.d("asd", "asd1");
        }else if(dataFrom.substring(0, 1).equals("T")){
            intentIn = new Intent(getApplicationContext(), EmergencyFiremanActivity.class);
//            Log.d("asd", "asd2");
        }
        for(Map.Entry<String, String> minidata : data.entrySet()){
            Log.d(minidata.getKey(), minidata.getValue());
            intentIn.putExtra(minidata.getKey(), minidata.getValue());
        }
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intentIn);

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);
        Cursor result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();

//        Intent localIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
//        localIntent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
//        localIntent.putExtra("extra_pkgname", getPackageName());

//        Intent localIntent = new Intent(getApplicationContext(), EmergencyActivity.class);

//        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        localIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentIn.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentIn.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intentIn);
    }
}
