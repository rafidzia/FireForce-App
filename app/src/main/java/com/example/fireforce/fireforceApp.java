package com.example.fireforce;

import android.app.Application;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class fireforceApp extends Application {
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Constants.socketURL);
        } catch(URISyntaxException e){
            throw new RuntimeException(e);
        }
    }
    public Socket getSocket() {
        return mSocket;
    }
}
