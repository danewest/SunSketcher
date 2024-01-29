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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Key;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class ClientRunOnTransfer {
    public static boolean clientTransferSequence() throws Exception {

        MetadataDB.Companion.createDB(App.getContext());
        Socket socket = new Socket("161.6.109.198", 443);

        //continue only if client is from the US
        InputStream inputStream = socket.getInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 10000);
        BufferedReader fromServer = new BufferedReader(new InputStreamReader(bufferedInputStream));
        String clearToSend = fromServer.readLine();

        if(!clearToSend.equals("True")) {
            //do not retry
            prefs.edit().putInt("finishedUpload", 2).apply();
            //the intent here is that 2 indicates that the client is ineligible for transfer at all, and should not retry
            //because the function returns true the app will nto attempt to retry
            return true;
        }

        //in event client is from US
        //attempt to complete transfer
        boolean success = startTransfer(socket);

        prefs.edit().putInt("finishedUpload", 1).apply();
        //trust, this could be null if the transfer fails
        return success;
    }


    static boolean startTransfer(Socket socket) throws Exception {
        //Authentication and Security
        //Generate RSA key needed for authentication and security
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        //Open server communication streams
        ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream fromServer = new ObjectInputStream(socket.getInputStream());

        //Send public key to server
        toServer.writeObject(publicKey);

        //Receive AES key from server
        byte[] encryptedMessage = (byte[]) fromServer.readObject();

        // Decrypt key using private key
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedMessage = cipher.doFinal(encryptedMessage);
        SecretKey aesKey = new SecretKeySpec(decryptedMessage, "AES");

        //Encrypt passkey with AES key and send to server
        send("SarahSketcher2024", aesKey, toServer);


        //--------------------------------------------------------------------------------------------------------------
        //Value Initialization

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

        SharedPreferences prefs = App.getContext().getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        int clientID = (int) prefs.getLong("clientID", 9999999);



        //-------------------------------------------------------------------------------------------------------------------
        //begin transfer messaging
        //send transfer request
        send("transferRequest", aesKey, toServer);


        //send client ID
        send(Integer.toString(clientID), aesKey, toServer);

        //send total number of photos
        send(Integer.toString(metadataList.size()), aesKey, toServer);

        for (Metadata metadata : metadataList) {
            Context context = App.getContext();
            prefs = context.getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
            if(metadata.getId() > (prefs.getInt("numUploaded", -1) + 1)) {

                //initialize values
                String[] filepathSplit = metadata.getFilepath().split("/");
                currentName = filepathSplit[filepathSplit.length - 1];

                File file = new File(metadata.getFilepath());
                byte[] imageData = new byte[(int) file.length()];

                //---------------------------------------------------------------------------------------------------------
                //send pseudometadata

                Log.d("NetworkTransfer", "current name = " + currentName);
                send(currentName, aesKey, toServer);

                Log.d("NetworkTransfer", "File length = " + (int) file.length());
                send(Integer.toString((int) file.length()), aesKey, toServer);

                //---------------------------------------------------------------------------------------------------------
                //gather and send image
                Log.d("NetworkTransfer", "Importing Photo " + currentPhoto + " ...");
                FileInputStream fileIn = new FileInputStream(file);
                fileIn.read(imageData);

                Log.d("NetworkTransfer", "Starting Transfer...");

                //send one photo to the server
                send(imageData, aesKey, toServer);
                fileIn.close();

                //----------------------------------------------------------------------------------------------------------
                //metadata transfer

                latitude = metadata.getLatitude();
                longitude = metadata.getLongitude();
                altitude = metadata.getAltitude();
                time = metadata.getCaptureTime();

                // send metadata to server
                send(Double.toString(latitude), aesKey, toServer);
                send(Double.toString(longitude), aesKey, toServer);
                send(Double.toString(altitude), aesKey, toServer);
                send(Long.toString(time), aesKey, toServer);
                Log.d("NetworkTransfer", "Transfer Successful!");
                SharedPreferences.Editor prefEdit = prefs.edit();

                //increment pref value for number of images uploaded after each successful upload, such that the number can be used to pick up transfer from where it was left off if connection was dropped
                prefEdit.putInt("numUploaded", prefs.getInt("numUploaded", -1) + 1);
                prefEdit.commit();
            }
        }


        if(new String(recieve(aesKey, fromServer)).equals("freeToDisconnect")) {
            socket.close();
            Log.d("NetworkTransfer", "Program Complete. Closing...");
            return true;
        }
        return true;
    }

    public static void send(String message, Key aesKey, ObjectOutputStream toServer) throws Exception {
        Cipher AEScipher = Cipher.getInstance("AES");
        AEScipher.init(Cipher.ENCRYPT_MODE, aesKey);

        byte[] encryptedMessage = AEScipher.doFinal(message.getBytes());
        toServer.writeObject(encryptedMessage);
    }

    public static void send(byte[] message, Key aesKey, ObjectOutputStream toServer) throws Exception {
        Cipher AEScipher = Cipher.getInstance("AES");
        AEScipher.init(Cipher.ENCRYPT_MODE, aesKey);

        byte[] encryptedMessage = AEScipher.doFinal(message);
        toServer.writeObject(encryptedMessage);
    }

    public static byte[] recieve(Key aesKey, ObjectInputStream fromServer) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);

        byte[] encryptedMessage = (byte[]) fromServer.readObject();
        byte[] decryptedMessage = cipher.doFinal(encryptedMessage);
        return decryptedMessage;
    }
}