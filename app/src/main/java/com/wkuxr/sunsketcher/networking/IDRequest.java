package com.wkuxr.sunsketcher.networking;

import static com.wkuxr.sunsketcher.activities.MainActivity.singleton;

import android.content.Context;
import android.util.Log;

import com.wkuxr.sunsketcher.App;

import java.net.*;
import java.io.*;

public class IDRequest {
    public static void clientTransferSequence(Context context) throws Exception {

        
        Socket ssocket = new Socket("161.6.109.198", 443);

        managePortsID(ssocket);


    }

    static void managePortsID(Socket ssocket) throws IOException {
        // to read data coming from the server
        BufferedReader fromThreadManager = new BufferedReader(new InputStreamReader(ssocket.getInputStream()));

        Log.d("Networktransfer","Connection Successful!");

        String inputLine;
        inputLine = fromThreadManager.readLine();

        if (inputLine.equals("0")) {
            //setTransferAlarm();
            Log.d("Networktransfer","Fatal Error. Connection Rejected. New alarm not set.");
        } else if(inputLine.equals("-1")) {
            Log.d("Networktransfer","Single port config detected.");
            startIDRequest(ssocket);
        } else {
            ssocket.close();
            Log.d("Networktransfer","Moving to port " + inputLine);
            Socket socket = new Socket("161.6.109.198", Integer.parseInt(inputLine));
            Log.d("Networktransfer","Successful!");
            startIDRequest(socket);
        }
    }

    static void startIDRequest(Socket ssocket) throws IOException {
        // to read data coming from the server
        BufferedReader fromThreadManager = new BufferedReader(new InputStreamReader(ssocket.getInputStream()));

        Log.d("NetworkTransfer","Connection Successful!");

        // to send data to the server
        DataOutputStream toServer = new DataOutputStream(ssocket.getOutputStream());

        toServer.writeBytes("IDRequest" + "\n");//---------------------------------------------------------
        toServer.flush();

        String transferID = fromThreadManager.readLine();
        App.getContext().getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE).edit().putLong("clientID", Long.parseLong(transferID)).apply();

        ssocket.close();
        Log.d("NetworkTransfer","Program Complete. Closing...");
    }

}