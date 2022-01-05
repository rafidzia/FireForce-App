package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class ChooseActivity extends AppCompatActivity {
    ImageButton buttonUser;
    ImageButton buttonFireman;
    RelativeLayout chooseUser;
    RelativeLayout chooseFireman;
    String resultChoose = "";
    Button buttonNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        getSupportActionBar().hide();

        buttonUser = findViewById(R.id.choose_user_icon);
        buttonFireman = findViewById(R.id.choose_fireman_icon);
        chooseUser = findViewById(R.id.choose_user);
        chooseFireman = findViewById(R.id.choose_fireman);
        buttonNext = findViewById(R.id.choose_next);
        buttonUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseUser.setElevation(10);
                chooseFireman.setElevation(0);
                buttonNext.setVisibility(View.VISIBLE);
                resultChoose = "user";
            }
        });
        buttonFireman.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseUser.setElevation(0);
                chooseFireman.setElevation(10);
                buttonNext.setVisibility(View.VISIBLE);
                resultChoose = "fireman";
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SearchActivity.class).putExtra("chooseResult", resultChoose).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });



    }
}