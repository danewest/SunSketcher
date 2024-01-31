package com.wkuxr.sunsketcher.networking;

import static com.wkuxr.sunsketcher.activities.MainActivity.singleton;

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

public class IDRequest {
    public static boolean clientTransferSequence() throws Exception {
        Log.d("NetworkTransfer", "Loading...");
        Socket socket = new Socket("161.6.109.198", 443);

        //continue only if client is from the US
        BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        String clearToSend = fromServer.readLine();

        Log.d("NetworkTransfer", "Clear to send recieved.");
        Log.d("NetworkTransfer", clearToSend);

        if(!clearToSend.contains("True")) {
            //do not retry
            Log.d("NetworkTransfer", "Clear to send is false.");
            prefs.edit().putInt("finishedUpload", 2).apply();
            //the intent here is that 2 indicates that the client is ineligible for transfer at all, and should not retry
            //because the function returns true the app will not attempt to retry
            return true;
        }

        Log.d("NetworkTransfer", "Clear to send is true.");

        //in event client is from US
        //attempt to complete transfer
        boolean success = startTransfer(socket);

        //prefs.edit().putInt("finishedUpload", 1).apply();
        //trust, this could be null if the transfer fails
        return success;
    }


    static boolean startTransfer(Socket socket) throws Exception {
        Log.d("NetworkTransfer", "Starting ID request...");
        //Authentication and Security
        //Generate RSA key needed for authentication and security
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        Log.d("NetworkTransfer", "Key Generator Initialized");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Log.d("NetworkTransfer", "Generating Keys");
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        Log.d("NetworkTransfer", "Keys Generated");

        //Open server communication streams
        ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream fromServer = new ObjectInputStream(socket.getInputStream());
        Log.d("NetworkTransfer", "Communication streams open");

        //Send public key to server
        toServer.writeObject(publicKey);
        Log.d("NetworkTransfer", "Public key sent");

        //Receive AES key from server
        byte[] encryptedMessage = (byte[]) fromServer.readObject();

        Log.d("NetworkTransfer", "Aes key recieved.");

        // Decrypt key using private key
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedMessage = cipher.doFinal(encryptedMessage);
        SecretKey aesKey = new SecretKeySpec(decryptedMessage, "AES");

        //Encrypt passkey with AES key and send to server
        send("SarahSketcher2024", aesKey, toServer);


        

        Log.d("NetworkTransfer", "Connection Successful!");

        SharedPreferences prefs = App.getContext().getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);


        //-------------------------------------------------------------------------------------------------------------------
        //begin transfer messaging
        //send ID request
        send("IDRequest", aesKey, toServer);

        
        String transferID = new String(recieve(aesKey, fromServer));
        singleton.getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE).edit().putLong("clientID", Long.parseLong(transferID)).apply();

        socket.close();
        Log.d("NetworkTransfer","Program Complete. Closing...");

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



