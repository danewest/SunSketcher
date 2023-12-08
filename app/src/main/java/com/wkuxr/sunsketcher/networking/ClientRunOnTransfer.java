package com.wkuxr.sunsketcher.networking;

import static com.wkuxr.sunsketcher.activities.SendConfirmationActivity.Companion;
import static com.wkuxr.sunsketcher.activities.SendConfirmationActivity.prefs;
import static com.wkuxr.sunsketcher.database.MetadataDB.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.wkuxr.sunsketcher.App;
import com.wkuxr.sunsketcher.database.Metadata;
import com.wkuxr.sunsketcher.database.MetadataDAO;
import com.wkuxr.sunsketcher.database.MetadataDB;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientRunOnTransfer {
    public static boolean clientTransferSequence() throws IOException {

        MetadataDB.Companion.createDB(App.getContext());
        Socket ssocket = new Socket("161.6.109.198", 443);

        boolean success = startTransfer(ssocket);

        prefs.edit().putInt("finishedUpload", 1).apply();
        //trust, this could be null if the transfer fails
        return success;
    }

    static void managePorts(Socket ssocket) throws IOException {
        // to read data coming from the server
        BufferedReader fromThreadManager = new BufferedReader(new InputStreamReader(ssocket.getInputStream()));

        Log.d("Networktransfer", "Connection Successful!");

        String inputLine;
        inputLine = fromThreadManager.readLine();

        if (inputLine.equals("0")) {
            Log.d("Networktransfer", "Transfer Rejected. Setting New Alarm.");
        } else if (inputLine.equals("-1")) {
            Log.d("Networktransfer", "Single port config detected.");
            startTransfer(ssocket);
        } else {
            ssocket.close();
            Log.d("Networktransfer", "Moving to port " + inputLine);
            Socket socket = new Socket("161.6.109.198", Integer.parseInt(inputLine));
            Log.d("Networktransfer", "Successful!");
            startTransfer(socket);
        }
    }

    static boolean startTransfer(Socket ssocket) throws IOException {
        double latitude = 0;
        double longitude = 0;
        double altitude = 0;
        long time = 0;

        db.initialize();
        MetadataDAO metadataDao = db.metadataDao();
        List<Metadata> metadataList = metadataDao.getAllImageMetas();

        Log.d("NetworkTransfer", "Loading...");
        int currentPhoto = 0;
        String currentName = "nameError";

        Log.d("NetworkTransfer", "Connection Successful!");

        DataOutputStream toServer = new DataOutputStream(ssocket.getOutputStream());

        toServer.writeBytes("transferRequest" + "\n");
        toServer.flush();

        SharedPreferences prefs = App.getContext().getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        int clientID = (int) prefs.getLong("clientID", 9999999);

        toServer.writeBytes(Integer.toString(clientID) + "\n");
        toServer.flush();

        toServer.writeBytes(metadataList.size() + "\n");
        toServer.flush();

        for (Metadata metadata : metadataList) {
            Context context = App.getContext();
            prefs = context.getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
            if(metadata.getId() > (prefs.getInt("numUploaded", -1) + 1)) {
                Log.d("NetworkTransfer", "Importing Photo " + currentPhoto + " ...");

                String[] filepathSplit = metadata.getFilepath().split("/");
                currentName = filepathSplit[filepathSplit.length - 1];

                File file = new File(metadata.getFilepath());
                byte[] imageData = new byte[(int) file.length()];

                Log.d("NetworkTransfer", "File length = " + (int) file.length());
                toServer.writeBytes((int) file.length() + "\n");
                toServer.flush();

                Log.d("NetworkTransfer", "current name = " + currentName);
                toServer.writeBytes(currentName + "\n");
                toServer.flush();

                FileInputStream fileIn = new FileInputStream(file);
                fileIn.read(imageData);

                Log.d("NetworkTransfer", "Starting Transfer...");

                String byteJustSent;

                // Send image data to server
                for (byte imageDatum : imageData) {
                    // System.out.print(imageData[i]);
                    // System.out.print(Byte.toString(imageData[i]));
                    byteJustSent = Byte.toString(imageDatum);
                    toServer.writeBytes(byteJustSent + "\n");
                    toServer.flush();
                }

                //image coruption checking resend function
                /*if(fromServer.readLine().equals("retryTransfer\n")) {
                    Log.d("NetworkTransfer", "Image Corruption Detected, retrying photo send;");

                    fileIn.close();
                    fileIn = new FileInputStream(file);
                    fileIn.read(imageData);

                    for (byte imageDatum : imageData) {
                        byteJustSent = Byte.toString(imageDatum);
                        toServer.writeBytes(byteJustSent + "\n");
                        toServer.flush();
                    }
                    
                    //server will now terminate connection if files are still invalid
                }*/
                
                fileIn.close();

                latitude = metadata.getLatitude();
                longitude = metadata.getLongitude();
                altitude = metadata.getAltitude();
                time = metadata.getCaptureTime();

                // send metadata to server
                toServer.writeBytes(latitude + "\n");
                toServer.flush();
                toServer.writeBytes(longitude + "\n");
                toServer.flush();
                toServer.writeBytes(altitude + "\n");
                toServer.flush();
                toServer.writeBytes(time + "\n");
                toServer.flush();

                Log.d("NetworkTransfer", "Transfer Successful!");
                SharedPreferences.Editor prefEdit = prefs.edit();

                //increment pref value for number of images uploaded after each successful upload, such that the number can be used to pick up transfer from where it was left off if connection was dropped
                prefEdit.putInt("numUploaded", prefs.getInt("numUploaded", -1) + 1);
                prefEdit.commit();
            }
        }

        BufferedReader fromServer = new BufferedReader(new InputStreamReader(ssocket.getInputStream()));

        if(fromServer.readLine().equals("freeToDisconnect")) {
            ssocket.close();
            Log.d("NetworkTransfer", "Program Complete. Closing...");
            return true;
        }
        return true;
    }
}