package com.otpower88.linebot;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private Switch serviceSwitch;
    private TextView statusText, timeText;
    private EditText groupNameEdit;
    private Button testButton, clearLogButton;
    private TextView logText;
    
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("otpower88_LineBot", MODE_PRIVATE);
        initViews();
        loadSettings();
        checkPermissions();
        updateTime();
    }
    
    private void initViews() {
        serviceSwitch = findViewById(R.id.serviceSwitch);
        statusText = findViewById(R.id.statusText);
        timeText = findViewById(R.id.timeText);
        groupNameEdit = findViewById(R.id.groupNameEdit);
        testButton = findViewById(R.id.testButton);
        clearLogButton = findViewById(R.id.clearLogButton);
        logText = findViewById(R.id.logText);
        
        serviceSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                if (isAccessibilityEnabled()) {
                    startService();
                } else {
                    serviceSwitch.setChecked(false);
                    showAccessibilityDialog();
                }
            } else {
                stopService();
            }
        });
        
        testButton.setOnClickListener(v -> runTest());
        clearLogButton.setOnClickListener(v -> clearLog());
        
        groupNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                prefs.edit().putString("groupName", s.toString()).apply();
                addLog("群組名稱已更新: " + s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void loadSettings() {
        String groupName = prefs.getString("groupName", "工作接單群組");
        groupNameEdit.setText(groupName);
    }
    
    private boolean isAccessibilityEnabled() {
        String service = getPackageName() + "/" + LineAccessibilityService.class.getName();
        String enabledServices = Settings.Secure.getString(
            getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(service);
    }
    
    private void checkPermissions() {
        if (!isAccessibilityEnabled()) {
            showAccessibilityDialog();
        }
    }
    
    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
            .setTitle("🔧 需要無障礙權限")
            .setMessage("otpower88搶單機器人需要無障礙服務權限來:\n\n" +
                       "📱 監控LINE通知\n" +
                       "⚡ 自動執行回覆\n" +
                       "🎯 精準搶單操作\n\n" +
                       "請在設定中找到 'otpower88搶單機器人' 並開啟權限。")
            .setPositiveButton("前往設定", (dialog, which) -> {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            })
            .setNegativeButton("稍後", null)
            .show();
    }
    
    private void startService() {
        statusText.setText("🟢 搶單服務運行中");
        statusText.setTextColor(0xFF28A745);
        addLog("=== otpower88搶單機器人已啟動 ===");
        addLog("工作時段: 07:00-19:00");
        addLog("監控群組: " + groupNameEdit.getText().toString());
        addLog("系統狀態: ✅ 就緒，等待搶單機會");
        
        // 檢查當前是否在工作時間
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 7 && hour < 19) {
            addLog("⚡ 當前在工作時段內，搶單功能已激活");
        } else {
            addLog("⏰ 當前非工作時段，將在明天07:00自動激活");
        }
    }
    
    private void stopService() {
        statusText.setText("🔴 搶單服務已停止");
        statusText.setTextColor(0xFFDC3545);
        addLog("=== otpower88搶單機器人已停止 ===");
        addLog("💤 系統進入待機模式");
    }
    
    private void runTest() {
        addLog("=== 開始關鍵字匹配測試 ===");
        
        String[] testMessages = {
            "9/15(週一)\n08:00\n新北市坪林區藤寮坑3號\n>\n台北市文山區興隆路三段111號萬芳醫院門診",
            "9/20(週三)\n14:00\n台北市文山區羅斯福路六段280號景美醫院\n>\n新北市坪林區大粗坑15號", 
            "@5523ㄚ 宏 請準時\n9/15(週一)\n08:00\n新北市坪林區藤寮坑3號",
            "大家好，今天天氣不錯呢"
        };
        
        boolean[] expectedResults = {true, true, false, false};
        String[] testNames = {"標準工作訊息", "反向路線訊息", "已確認訊息(應排除)", "普通聊天(應忽略)"};
        
        int passCount = 0;
        for (int i = 0; i < testMessages.length; i++) {
            boolean matches = testMessages[i].matches(".*\\d{1,2}/\\d{1,2}\\(週.{1}\\).*\\d{2}:\\d{2}.*(新北市|台北市).*>.*");
            boolean excluded = testMessages[i].contains("@");
            boolean finalResult = matches && !excluded;
            
            boolean testPassed = (finalResult == expectedResults[i]);
            String result = testPassed ? "✅ 通過" : "❌ 失敗";
            addLog("測試 " + (i + 1) + " (" + testNames[i] + "): " + result);
            
            if (testPassed) passCount++;
        }
        
        addLog("=== 測試完成: " + passCount + "/" + testMessages.length + " 通過 ===");
        
        if (passCount == testMessages.length) {
            Toast.makeText(this, "🎉 所有測試通過！關鍵字設定正確", Toast.LENGTH_LONG).show();
            addLog("🎯 系統配置完美，準備實戰搶單！");
        } else {
            Toast.makeText(this, "⚠️ 部分測試失敗，請檢查設定", Toast.LENGTH_LONG).show();
            addLog("⚠️ 建議檢查關鍵字模式設定");
        }
    }
    
    private void clearLog() {
        logText.setText("");
        addLog("[系統] otpower88搶單機器人日誌已清除");
        addLog("[資訊] 系統運行正常，等待指令");
    }
    
    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        String currentTime = String.format("%02d:%02d", 
            calendar.get(Calendar.HOUR_OF_DAY), 
            calendar.get(Calendar.MINUTE));
        timeText.setText("🕐 " + currentTime);
        
        // 每分鐘更新時間
        timeText.postDelayed(this::updateTime, 60000);
    }
    
    public void addLog(String message) {
        runOnUiThread(() -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new java.util.Date());
            String logEntry = String.format("[%s] %s\n", timestamp, message);
            logText.append(logEntry);
            
            // 自動滾動到底部
            final int scrollAmount = logText.getLayout() != null ? 
                logText.getLayout().getLineTop(logText.getLineCount()) - logText.getHeight() : 0;
            if (scrollAmount > 0) {
                logText.scrollTo(0, scrollAmount);
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 檢查服務狀態
        serviceSwitch.setChecked(isAccessibilityEnabled());
        updateTime();
    }
}
