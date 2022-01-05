package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.messaging.FirebaseMessaging;

public class SettingActivity extends AppCompatActivity {
    Cursor result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getSupportActionBar().hide();
        getSupportActionBar().hide();

        Button logout = findViewById(R.id.logout);
        SQLiteDatabase mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);
        result = mydb.rawQuery("select * from place", null);
        result.moveToFirst();
//
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppUtils.clearData(getApplicationContext());
                SettingActivity.this.finish();
                System.exit(0);
//                FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/FireSmokeDetected-" + result.getString(0));
//                mydb.execSQL("delete from place where id = ?", new String[]{result.getString(0)});
//                startActivity(new Intent(getApplicationContext(), ChooseActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
//                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        if(result.getString(1).equals("user")){
            startActivity(new Intent(getApplicationContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }else if(result.getString(1).equals("fireman")){
            startActivity(new Intent(getApplicationContext(), HomeFiremanActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }

    }
}