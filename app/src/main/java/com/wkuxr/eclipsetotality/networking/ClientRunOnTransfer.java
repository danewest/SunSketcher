package com.wkuxr.eclipsetotality.networking;

import static com.wkuxr.eclipsetotality.activities.SendConfirmationActivity.prefs;
import static com.wkuxr.eclipsetotality.database.MetadataDB.db;

import android.util.Log;

import com.wkuxr.eclipsetotality.database.Metadata;
import com.wkuxr.eclipsetotality.database.MetadataDAO;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

//untested transfer code added
//add to server: ---------------------------------------------------------------------------------------------------------------------
//int databaseFilenameIndex
//...name = + Integer.toString(databaseFilenameIndex++)

public class ClientRunOnTransfer {
    public static void clientTransferSequence() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicReference<Socket> socketHolder = new AtomicReference<>();

        Socket ssocket = new Socket("161.6.109.198", 443);

        Future<Void> future = executorService.submit(task);

        //backup code if timed out
        try {
            future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("Connection timeout. Moving connection time...");
            future.cancel(true); // Cancel the task
            setTransferAlarm();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        ssocket = socketHolder.get();//this may require ssocket to be a new varriable w a new name 
            if (ssocket != null) {
                startTransfer(ssocket);
                ssocket.close(); // Close the socket
            }
        
        executorService.shutdown();

        prefs.edit().putInt("finishedUpload", 1).apply();
    }

    static void managePorts(Socket ssocket) throws IOException {
        // to read data coming from the server
        BufferedReader fromThreadManager = new BufferedReader(new InputStreamReader(ssocket.getInputStream()));

        System.out.println("Connection Successful!");

        String inputLine;
        inputLine = fromThreadManager.readLine();

        if (inputLine.equals("0")) {
            setTransferAlarm();
            System.out.println("Transfer Rejected. Setting New Alarm.");
        } else if(inputLine.equals("-1")) {
            System.out.println("Single port config detected.");
             startTransfer(ssocket);
        } else {
            ssocket.close();
            System.out.println("Moving to port " + inputLine);
            Socket socket = new Socket("161.6.109.198", Integer.parseInt(inputLine));
            System.out.println("Successful!");
            startTransfer(socket);
        }
    }



    static void startTransfer(Socket ssocket) throws IOException {
        double latitude = 0;
        double longitude = 0;
        double altitude = 0;
        long time = 0;
        // work with travis, set these variables to the correct values

        //code added---------------------------------------------------------------------------------------
        db.initialize();
        MetadataDAO metadataDao = db.metadataDao();
        List<Metadata> metadataList = metadataDao.getAllImageMetas();


        //-------------------------------------------------------------------------------------------------


        Log.d("NetworkTransfer","Loading...");
        int currentPhoto = 0;
        String currentName = "nameError";

        // to read data coming from the server
        BufferedReader fromThreadManager = new BufferedReader(new InputStreamReader(ssocket.getInputStream()));

        Log.d("NetworkTransfer","Connection Successful!");

        
        
            // to send data to the server
            DataOutputStream toServer = new DataOutputStream(socket.getOutputStream());

            toServer.writeBytes(Integer.toString(metadataList.size()) + "\n");//---------------------------------------------------------
            toServer.flush();

            for(Metadata metadata : metadataList) {
                Log.d("NetworkTransfer","Importing Photo " + currentPhoto + " ...");

                String[] filepathSplit = metadata.getFilepath().split("/");
                currentName = filepathSplit[filepathSplit.length - 1];

                File file = new File(metadata.getFilepath());
                byte[] imageData = new byte[(int) file.length()];

                Log.d("NetworkTransfer","File length = " + (int) file.length());
                toServer.writeBytes((int) file.length() + "\n");
                toServer.flush();

                Log.d("NetworkTransfer","current name = " + currentName);
                toServer.writeBytes(currentName + "\n");
                toServer.flush();

                FileInputStream fileIn = new FileInputStream(file);
                fileIn.read(imageData);

                Log.d("NetworkTransfer","Starting Transfer...");

                String byteJustSent;

                // Send image data to server
                for (int i = 0; i < imageData.length; i++) {
                    //System.out.print(imageData[i]);
                    //System.out.print(Byte.toString(imageData[i]));
                    byteJustSent = Byte.toString(imageData[i]);
                    toServer.writeBytes(byteJustSent + "\n");
                    toServer.flush();
                }
                fileIn.close();

                //code added---------------------------------------------------------------------------------------
                latitude = metadata.getLatitude();
                longitude = metadata.getLongitude();
                altitude = metadata.getAltitude();
                time = metadata.getCaptureTime();



                //-------------------------------------------------------------------------------------------------

                // send metadata to server
                toServer.writeBytes(Double.toString(latitude) + "\n");
                toServer.flush();
                toServer.writeBytes(Double.toString(longitude) + "\n");
                toServer.flush();
                toServer.writeBytes(Double.toString(altitude) + "\n");
                toServer.flush();
                toServer.writeBytes(Long.toString(time) + "\n");
                toServer.flush();

                Log.d("NetworkTransfer","Transfer Successful!");

            }
            socket.close();

            Log.d("NetworkTransfer","Program Complete. Closing...");
        }

    static void setTransferAlarm() {
        // set an alarm to run ClientRunOnTransfer at a time in the future specified by the ID
        return;
    }
}