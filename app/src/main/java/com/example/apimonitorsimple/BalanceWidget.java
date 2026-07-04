package com.example.apimonitorsimple;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import okhttp3.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BalanceWidget extends AppWidgetProvider {
    
    private static final String TRANSIT_URL = "https://sub2.aihuangniu.com/v1/usage";
    private static final String DEEPSEEK_URL = "https://api.deepseek.com/user/balance";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if ("REFRESH_BALANCE".equals(intent.getAction())) {
            // 刷新所有小组件
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new android.content.ComponentName(context, BalanceWidget.class));
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
    
    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.balance_widget);
        
        // 设置点击刷新的意图
        Intent intent = new Intent(context, BalanceWidget.class);
        intent.setAction("REFRESH_BALANCE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRefreshButton, pendingIntent);
        
        // 设置打开应用的意图
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetTitle, openAppPendingIntent);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
        
        // 异步获取余额
        new FetchBalanceTask(context, appWidgetManager, appWidgetId).execute();
    }
    
    private static class FetchBalanceTask extends AsyncTask<Void, Void, String[]> {
        private Context context;
        private AppWidgetManager appWidgetManager;
        private int appWidgetId;
        
        FetchBalanceTask(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
        }
        
        @Override
        protected String[] doInBackground(Void... voids) {
            String[] results = new String[2];
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            Gson gson = new Gson();
            
            // 获取三方中转余额
            String transitKey = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
                    .getString("transit_api_key", "");
            if (!transitKey.isEmpty()) {
                Request request = new Request.Builder()
                        .url(TRANSIT_URL)
                        .addHeader("Authorization", "Bearer " + transitKey)
                        .build();
                
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        results[0] = response.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            // 获取DeepSeek余额
            String deepseekKey = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
                    .getString("deepseek_api_key", "");
            if (!deepseekKey.isEmpty()) {
                Request request = new Request.Builder()
                        .url(DEEPSEEK_URL)
                        .addHeader("Authorization", "Bearer " + deepseekKey)
                        .build();
                
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        results[1] = response.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            return results;
        }
        
        @Override
        protected void onPostExecute(String[] results) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.balance_widget);
            
            // 更新三方中转余额
            if (results[0] != null) {
                try {
                    Gson gson = new Gson();
                    BalanceNotificationService.TransitBalance transitBalance = 
                            gson.fromJson(results[0], BalanceNotificationService.TransitBalance.class);
                    views.setTextViewText(R.id.transitBalance, String.format("$%.2f", transitBalance.balance));
                    
                    // 保存到SharedPreferences
                    context.getSharedPreferences("balance_data", Context.MODE_PRIVATE)
                            .edit()
                            .putString("transit_balance", String.format("$%.2f", transitBalance.balance))
                            .apply();
                } catch (Exception e) {
                    views.setTextViewText(R.id.transitBalance, "解析失败");
                }
            } else {
                views.setTextViewText(R.id.transitBalance, "--");
            }
            
            // 更新DeepSeek余额
            if (results[1] != null) {
                try {
                    Gson gson = new Gson();
                    BalanceNotificationService.DeepseekBalance deepseekBalance = 
                            gson.fromJson(results[1], BalanceNotificationService.DeepseekBalance.class);
                    if (deepseekBalance.balance_infos != null && deepseekBalance.balance_infos.length > 0) {
                        views.setTextViewText(R.id.deepseekBalance, String.format("$%.2f", deepseekBalance.balance_infos[0].balance));
                        
                        // 保存到SharedPreferences
                        context.getSharedPreferences("balance_data", Context.MODE_PRIVATE)
                                .edit()
                                .putString("deepseek_balance", String.format("$%.2f", deepseekBalance.balance_infos[0].balance))
                                .apply();
                    } else {
                        views.setTextViewText(R.id.deepseekBalance, "无余额");
                    }
                } catch (Exception e) {
                    views.setTextViewText(R.id.deepseekBalance, "解析失败");
                }
            } else {
                views.setTextViewText(R.id.deepseekBalance, "--");
            }
            
            // 更新时间
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            views.setTextViewText(R.id.lastUpdated, "更新: " + sdf.format(new java.util.Date()));
            
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
    @Override
    public void onEnabled(Context context) {
        // 第一个小组件被创建时调用
    }
    
    @Override
    public void onDisabled(Context context) {
        // 最后一个小组件被删除时调用
    }
}