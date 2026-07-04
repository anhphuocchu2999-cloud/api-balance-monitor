package com.example.apimonitorsimple;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    
    private WebView webView;
    private EditText transitApiKey;
    private EditText deepseekApiKey;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化视图
        webView = findViewById(R.id.webView);
        transitApiKey = findViewById(R.id.transitApiKey);
        deepseekApiKey = findViewById(R.id.deepseekApiKey);
        
        Button saveButton = findViewById(R.id.saveButton);
        Button startServiceButton = findViewById(R.id.startServiceButton);
        
        // 加载保存的API Key
        loadApiKeys();
        
        // 配置WebView
        setupWebView();
        
        // 保存按钮点击事件
        saveButton.setOnClickListener(v -> saveApiKeys());
        
        // 启动服务按钮点击事件
        startServiceButton.setOnClickListener(v -> startNotificationService());
        
        // 请求通知权限
        requestNotificationPermission();
        
        // 启动通知服务
        startNotificationService();
    }
    
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        
        // 添加JavaScript接口
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        // 加载本地HTML文件
        webView.loadUrl("file:///android_asset/monitor.html");
    }
    
    private void loadApiKeys() {
        String transitKey = getSharedPreferences("api_keys", MODE_PRIVATE)
                .getString("transit_api_key", "");
        String deepseekKey = getSharedPreferences("api_keys", MODE_PRIVATE)
                .getString("deepseek_api_key", "");
        
        transitApiKey.setText(transitKey);
        deepseekApiKey.setText(deepseekKey);
    }
    
    private void saveApiKeys() {
        String transitKey = transitApiKey.getText().toString().trim();
        String deepseekKey = deepseekApiKey.getText().toString().trim();
        
        getSharedPreferences("api_keys", MODE_PRIVATE)
                .edit()
                .putString("transit_api_key", transitKey)
                .putString("deepseek_api_key", deepseekKey)
                .apply();
        
        Toast.makeText(this, "API Key已保存", Toast.LENGTH_SHORT).show();
        
        // 刷新WebView
        webView.reload();
    }
    
    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, BalanceNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNotificationService();
            }
        }
    }
    
    // JavaScript接口
    public class WebAppInterface {
        @JavascriptInterface
        public String getTransitApiKey() {
            return getSharedPreferences("api_keys", MODE_PRIVATE)
                    .getString("transit_api_key", "");
        }
        
        @JavascriptInterface
        public String getDeepseekApiKey() {
            return getSharedPreferences("api_keys", MODE_PRIVATE)
                    .getString("deepseek_api_key", "");
        }
        
        @JavascriptInterface
        public void saveTransitApiKey(String key) {
            getSharedPreferences("api_keys", MODE_PRIVATE)
                    .edit()
                    .putString("transit_api_key", key)
                    .apply();
        }
        
        @JavascriptInterface
        public void saveDeepseekApiKey(String key) {
            getSharedPreferences("api_keys", MODE_PRIVATE)
                    .edit()
                    .putString("deepseek_api_key", key)
                    .apply();
        }
        
        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
        
        @JavascriptInterface
        public void refreshBalances() {
            runOnUiThread(() -> webView.reload());
        }
    }
}