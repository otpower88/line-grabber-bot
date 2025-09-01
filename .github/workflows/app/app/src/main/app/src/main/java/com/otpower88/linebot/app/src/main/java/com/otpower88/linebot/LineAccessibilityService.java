package com.otpower88.linebot;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Bundle;
import android.content.SharedPreferences;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.Calendar;

public class LineAccessibilityService extends AccessibilityService {
    private Pattern keywordPattern;
    private Random random = new Random();
    private long lastReplyTime = 0;
    private Handler handler = new Handler();
    private SharedPreferences prefs;
    
    // 統計數據
    private int totalAttempts = 0;
    private int successCount = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("otpower88_LineBot", MODE_PRIVATE);
        
        // 初始化關鍵字模式
        keywordPattern = Pattern.compile(".*\\d{1,2}/\\d{1,2}\\(週.{1}\\).*\\d{2}:\\d{2}.*(新北市|台北市).*>.*");
        
        // 載入統計數據
        totalAttempts = prefs.getInt("totalAttempts", 0);
        successCount = prefs.getInt("successCount", 0);
        
        broadcastLog("🤖 otpower88搶單服務已啟動");
        broadcastLog("⚙️ 關鍵字模式已載入");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return;
        }
        
        if (!"com.linecorp.line.android".equals(event.getPackageName())) {
            return;
        }
        
        String notificationTitle = "";
        String notificationText = "";
        
        if (event.getText() != null && !event.getText().isEmpty()) {
            notificationTitle = event.getText().get(0).toString();
        }
        
        if (event.getContentDescription() != null) {
            notificationText = event.getContentDescription().toString();
        }
        
        processMessage(notificationTitle, notificationText);
    }
    
    private void processMessage(String title, String text) {
        // 檢查群組名稱
        String groupName = prefs.getString("groupName", "工作接單群組");
        if (!title.contains(groupName)) {
            return;
        }
        
        broadcastLog("📱 收到目標群組訊息: " + groupName);
        
        // 檢查關鍵字匹配
        if (!keywordPattern.matcher(text).matches()) {
            broadcastLog("⚠️ 訊息不匹配關鍵字模式，跳過");
            return;
        }
        
        broadcastLog("✅ 關鍵字匹配成功！");
        
        // 排除已確認的訊息
        if (text.contains("@")) {
            broadcastLog("🚫 訊息已被確認，跳過回覆");
            return;
        }
        
        // 檢查工作時間
        if (!isInWorkTime()) {
            broadcastLog("⏰ 當前非工作時段 (07:00-19:00)，跳過");
            return;
        }
        
        // 防重複機制
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastReplyTime < 5000) {
            broadcastLog("🔄 距離上次回覆不足5秒，跳過");
            return;
        }
        
        // 準備執行搶單
        executeReplyWithDelay();
    }
    
    private void executeReplyWithDelay() {
        // 隨機延遲 (0.5-1.2秒)
        int delay = 500 + random.nextInt(700);
        
        // 根據時間調整延遲策略
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 7 && hour <= 10) {
            // 早上競爭激烈，稍微快一點
            delay = 300 + random.nextInt(500); // 0.3-0.8秒
        }
        
        broadcastLog("⏱️ 延遲 " + delay + "ms 後執行搶單");
        
        handler.postDelayed(() -> {
            executeReply();
            lastReplyTime = System.currentTimeMillis();
        }, delay);
    }
    
    private void executeReply() {
        try {
            totalAttempts++;
            broadcastLog("🎯 開始執行第 " + totalAttempts + " 次搶單嘗試");
            
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                broadcastLog("❌ 無法獲取窗口信息");
                saveStats();
                return;
            }
            
            // 等待界面載入
            Thread.sleep(800);
            
            // 尋找輸入框
            AccessibilityNodeInfo inputField = findInputField(rootNode);
            if (inputField == null) {
                broadcastLog("❌ 找不到輸入框");
                saveStats();
                return;
            }
            
            // 生成隨機回覆數字 (0-9，加重0,5,8的機率)
            int[] weightedNumbers = {0,1,2,3,4,5,6,7,8,9,0,5,8}; // 0,5,8出現3次
            String replyNumber = String.valueOf(weightedNumbers[random.nextInt(weightedNumbers.length)]);
            
            // 點擊輸入框
            inputField.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Thread.sleep(200);
            
            // 輸入數字
            Bundle arguments = new Bundle();
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                replyNumber
            );
            boolean textSet = inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            
            if (!textSet) {
                broadcastLog("❌ 文字輸入失敗");
                saveStats();
                return;
            }
            
            broadcastLog("✏️ 已輸入數字: " + replyNumber);
            Thread.sleep(150);
            
            // 尋找發送按鈕
            AccessibilityNodeInfo sendButton = findSendButton(rootNode);
            if (sendButton == null) {
                broadcastLog("❌ 找不到發送按鈕");
                saveStats();
                return;
            }
            
            // 點擊發送
            boolean sent = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            
            if (sent) {
                successCount++;
                broadcastLog("🎉 成功發送回覆: " + replyNumber);
                broadcastLog("📊 成功率: " + successCount + "/" + totalAttempts + 
                           " (" + String.format("%.1f", (successCount * 100.0 / totalAttempts)) + "%)");
            } else {
                broadcastLog("❌ 發送按鈕點擊失敗");
            }
            
            // 返回桌面
            Thread.sleep(300);
            performGlobalAction(GLOBAL_ACTION_HOME);
            broadcastLog("🏠 已返回桌面");
            
            saveStats();
            
        } catch (Exception e) {
            broadcastLog("❌ 搶單執行異常: " + e.getMessage());
            saveStats();
        }
    }
    
    private AccessibilityNodeInfo findInputField(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        if (className.contains("EditText")) {
            return node;
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findInputField(child);
            if (result != null) return result;
        }
        
        return null;
    }
    
    private AccessibilityNodeInfo findSendButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        
        String textStr = text != null ? text.toString() : "";
        String descStr = desc != null ? desc.toString() : "";
        
        if (textStr.contains("發送") || textStr.contains("傳送") || textStr.equals("Send") ||
            descStr.contains("發送") || descStr.contains("傳送") || descStr.equals("Send")) {
            return node;
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findSendButton(child);
            if (result != null) return result;
        }
        
        return null;
    }
    
    private boolean isInWorkTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= 7 && hour < 19; // 07:00-19:00
    }
    
    private void saveStats() {
        prefs.edit()
            .putInt("totalAttempts", totalAttempts)
            .putInt("successCount", successCount)
            .apply();
    }
    
    private void broadcastLog(String message) {
        // 這裡實際上會通過Intent發送日誌給MainActivity
        // 為了簡化，這裡只是一個占位符
    }
    
    @Override
    public void onInterrupt() {
        broadcastLog("⚠️ 無障礙服務被中斷");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        saveStats();
        broadcastLog("🛑 otpower88搶單服務已停止");
    }
}
