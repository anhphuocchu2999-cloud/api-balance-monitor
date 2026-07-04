package com.example.apimonitorsimple;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BalanceNotificationService extends Service {
    
    private static final String CHANNEL_ID = "balance_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL = 5 * 60 * 1000; // 5分钟更新一次
    
    private Handler handler;
    private Runnable updateRunnable;
    private OkHttpClient client;
    private Gson gson;
    
    private static final String TRANSIT_URL = "https://sub2.aihuangniu.com/v1/usage";
    private static final String DEEPSEEK_URL = "https://api.deepseek.com/user/balance";
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("正在初始化...", "正在初始化..."));
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateBalances();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(updateRunnable);
        handler.post(updateRunnable);
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "API额度监控",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("显示API余额信息");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Intent refreshIntent = new Intent(this, BalanceWidget.class);
        refreshIntent.setAction("REFRESH_BALANCE");
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(this, 0, refreshIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_refresh, "刷新", refreshPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    private void updateBalances() {
        String transitKey = getSharedPreferences("api_keys", MODE_PRIVATE)
                .getString("transit_api_key", "");
        String deepseekKey = getSharedPreferences("api_keys", MODE_PRIVATE)
                .getString("deepseek_api_key", "");
        
        if (!transitKey.isEmpty()) {
            fetchTransitBalance(transitKey);
        }
        
        if (!deepseekKey.isEmpty()) {
            fetchDeepseekBalance(deepseekKey);
        }
    }
    
    private void fetchTransitBalance(String apiKey) {
        Request request = new Request.Builder()
                .url(TRANSIT_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateNotification("查询失败", "请检查网络连接");
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    TransitBalance balance = gson.fromJson(body, TransitBalance.class);
                    String balanceStr = String.format("$%.2f", balance.balance);
                    
                    // 保存余额信息
                    getSharedPreferences("balance_data", MODE_PRIVATE)
                            .edit()
                            .putString("transit_balance", balanceStr)
                            .apply();
                    
                    updateNotificationFromSavedData();
                } else {
                    updateNotification("查询失败", "HTTP " + response.code());
                }
            }
        });
    }
    
    private void fetchDeepseekBalance(String apiKey) {
        Request request = new Request.Builder()
                .url(DEEPSEEK_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateNotification("查询失败", "请检查网络连接");
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    DeepseekBalance balance = gson.fromJson(body, DeepseekBalance.class);
                    
                    if (balance.balance_infos != null && balance.balance_infos.length > 0) {
                        String balanceStr = String.format("$%.2f", balance.balance_infos[0].balance);
                        
                        // 保存余额信息
                        getSharedPreferences("balance_data", MODE_PRIVATE)
                                .edit()
                                .putString("deepseek_balance", balanceStr)
                                .apply();
                    }
                    
                    updateNotificationFromSavedData();
                } else {
                    updateNotification("查询失败", "HTTP " + response.code());
                }
            }
        });
    }
    
    private void updateNotificationFromSavedData() {
        String transitBalance = getSharedPreferences("balance_data", MODE_PRIVATE)
                .getString("transit_balance", "--");
        String deepseekBalance = getSharedPreferences("balance_data", MODE_PRIVATE)
                .getString("deepseek_balance", "--");
        
        String title = "API额度监控";
        String content = "三方: " + transitBalance + " | DeepSeek: " + deepseekBalance;
        
        updateNotification(title, content);
    }
    
    private void updateNotification(String title, String content) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, createNotification(title, content));
    }
    
    // 数据模型
    public static class TransitBalance {
        public double balance;
        public String planName;
        public String mode;
    }
    
    public static class DeepseekBalance {
        public boolean is_available;
        public BalanceInfo[] balance_infos;
        
        public static class BalanceInfo {
            public double balance;
        }
    }
}