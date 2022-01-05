package com.example.fireforce;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SearchActivity extends AppCompatActivity {
    EditText searchInput;
    EditText tokenInput;
    RelativeLayout listPlaceContainer;
    ListView listPlace;
    String resultChoose;
    Socket mSocket;
    ArrayAdapter adapter1;
    Button nextButon;
    boolean state = false;
    SQLiteDatabase mydb;

    ArrayList<String> placeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        getSupportActionBar().hide();

        mydb = openOrCreateDatabase("fireman", MODE_PRIVATE, null);

        fireforceApp app = (fireforceApp) getApplication();
        mSocket = app.getSocket();

        mSocket.on("searchPlaceResult", searchResult);
        mSocket.on("findPlaceResult", placeResult);

        resultChoose = getIntent().getStringExtra("chooseResult");

        searchInput = findViewById(R.id.search_input);
        tokenInput = findViewById(R.id.token_input);
        listPlaceContainer = findViewById(R.id.search_list_container);
        nextButon = findViewById(R.id.search_next);
        listPlace = findViewById(R.id.search_list);

        adapter1 = new ArrayAdapter<String>(this, R.layout.listview_search, placeList);
        listPlace.setAdapter(adapter1);

        listPlace.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String a = (String) (listPlace.getItemAtPosition(i));
                searchInput.setText(a);
                searchInput.setSelection(searchInput.getText().length());
                if(checkPlaceOnList()){
                    tokenInput.setVisibility(View.VISIBLE);
                    nextButon.setVisibility(View.VISIBLE);
                    state = true;
                    placeList.clear();
                    adapter1.notifyDataSetChanged();
                    listPlaceContainer.getLayoutParams().height = 0;
                }else{
                    tokenInput.setVisibility(View.INVISIBLE);
                    nextButon.setVisibility(View.INVISIBLE);
                    state = false;
                }
            }
        });


        searchInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                JSONObject data = new JSONObject();
                try {
                    data.put("option", resultChoose);
                    data.put("place", searchInput.getText());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if(checkPlaceOnList()){
                    tokenInput.setVisibility(View.VISIBLE);
                    nextButon.setVisibility(View.VISIBLE);
                    state = true;
                }else{
                    tokenInput.setVisibility(View.INVISIBLE);
                    nextButon.setVisibility(View.INVISIBLE);
                    state = false;
                }
                if(!state && !searchInput.getText().toString().isEmpty()){
                    mSocket.emit("searchPlace", data);
                }
                if(searchInput.getText().toString().isEmpty()){
                    placeList.clear();
                    adapter1.notifyDataSetChanged();
                    listPlaceContainer.getLayoutParams().height = 0;
                }

            }
        });

        nextButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject data = new JSONObject();
                try{
                    data.put("option", resultChoose);
                    data.put("name", searchInput.getText());
                    data.put("token", tokenInput.getText());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit("findPlace", data);

            }
        });

    }

    private final Emitter.Listener searchResult = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            SearchActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    JSONArray place = new JSONArray();
                    try {
                        place = data.getJSONArray("data");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    placeList.clear();
                    for(int i = 0; i < place.length(); i++){
                        placeList.add(place.optString(i));
                    }
                    if(place.length() <= 3){
                        listPlaceContainer.getLayoutParams().height = 85 * place.length();
                    }else{
                        listPlaceContainer.getLayoutParams().height = 255;
                    }
                    adapter1.notifyDataSetChanged();
                }
            });
        }
    };

    private final Emitter.Listener placeResult = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            SearchActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Boolean status = false;
                    String id = "";
                    try {
                        status = data.getBoolean("status");
                        id = data.getString("id");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(status){
                        mydb.execSQL("insert into place (id, option, name, token) values (?, ?, ?, ?)", new String[]{id, resultChoose, String.valueOf(searchInput.getText()), String.valueOf(tokenInput.getText())});
                        if(resultChoose.equals("user")){
                            startActivity(new Intent(getApplicationContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        }else if(resultChoose.equals("fireman")){
                            startActivity(new Intent(getApplicationContext(), HomeFiremanActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(), "Token Mismatch", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    };

    private boolean checkPlaceOnList(){
        if(!searchInput.getText().toString().isEmpty()){
            if(placeList.contains(searchInput.getText().toString())){
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), ChooseActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}