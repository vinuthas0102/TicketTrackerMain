package com.tickettracker.service;

import com.tickettracker.util.JsonUtil;
import com.tickettracker.util.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ChatNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ChatNotificationService.class);

    private final String emailWebhookUrl;
    private final String smsWebhookUrl;
    private final String whatsappWebhookUrl;
    private final String webhookApiKey;
    private final int webhookTimeoutMs;

    public ChatNotificationService() {
        this.emailWebhookUrl = PropertiesLoader.getProperty("chat.webhook.email.url", "");
        this.smsWebhookUrl = PropertiesLoader.getProperty("chat.webhook.sms.url", "");
        this.whatsappWebhookUrl = PropertiesLoader.getProperty("chat.webhook.whatsapp.url", "");
        this.webhookApiKey = PropertiesLoader.getProperty("chat.webhook.api.key", "");
        String timeoutStr = PropertiesLoader.getProperty("chat.webhook.timeout.ms", "5000");
        this.webhookTimeoutMs = Integer.parseInt(timeoutStr);
    }

    public void sendNotification(String channel, String content, String recipientName,
            byte[] userId, String attachmentName) {
        String webhookUrl = getWebhookUrl(channel);
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.warn("No webhook URL configured for channel: {}. Message will be stored in-app only.", channel);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("channel", channel);
            payload.put("content", content);
            payload.put("recipientName", recipientName);
            payload.put("userId", bytesToHex(userId));
            if (attachmentName != null) {
                payload.put("attachmentName", attachmentName);
            }
            payload.put("timestamp", System.currentTimeMillis());

            String jsonPayload = JsonUtil.getObjectMapper().writeValueAsString(payload);

            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            if (webhookApiKey != null && !webhookApiKey.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + webhookApiKey);
            }
            conn.setConnectTimeout(webhookTimeoutMs);
            conn.setReadTimeout(webhookTimeoutMs);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Webhook notification sent successfully for channel: {} (response: {})", channel, responseCode);
            } else {
                logger.warn("Webhook notification failed for channel: {} (response: {})", channel, responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            logger.warn("Failed to send webhook notification for channel: {} - {}", channel, e.getMessage(), e);
        }
    }

    private String getWebhookUrl(String channel) {
        if (channel == null) return null;
        switch (channel.toLowerCase()) {
            case "email": return emailWebhookUrl;
            case "sms": return smsWebhookUrl;
            case "whatsapp": return whatsappWebhookUrl;
            default: return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
