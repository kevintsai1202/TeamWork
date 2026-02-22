package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.WebhookTriggerRequest;
import com.teamwork.gateway.entity.TaskTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebhookSecurityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String NONCE_KEY_PREFIX = "webhook:nonce:";

    private final StringRedisTemplate redisTemplate;
    private final long allowedSkewSeconds;
    private final long nonceTtlSeconds;
    private final Map<String, Long> inMemoryNonceStore = new ConcurrentHashMap<>();

    public WebhookSecurityService(
            StringRedisTemplate redisTemplate,
            @Value("${gateway.webhook.allowed-skew-seconds:300}") long allowedSkewSeconds,
            @Value("${gateway.webhook.nonce-ttl-seconds:300}") long nonceTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.allowedSkewSeconds = allowedSkewSeconds;
        this.nonceTtlSeconds = nonceTtlSeconds;
    }

    public void validate(
            TaskTrigger trigger,
            String webhookKey,
            WebhookTriggerRequest request,
            String timestampHeader,
            String nonceHeader,
            String signatureHeader) {
        if (isBlank(timestampHeader) || isBlank(nonceHeader) || isBlank(signatureHeader)) {
            throw new IllegalArgumentException("Missing webhook security headers");
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid X-Trigger-Timestamp");
        }

        long nowSeconds = Instant.now().getEpochSecond();
        if (Math.abs(nowSeconds - timestampSeconds) > allowedSkewSeconds) {
            throw new IllegalArgumentException("Webhook timestamp is out of allowed window");
        }

        String secret = trigger.getSecretRef();
        if (isBlank(secret)) {
            throw new IllegalArgumentException("Webhook secret is not configured");
        }

        String canonicalString = buildCanonicalString(timestampHeader, nonceHeader, webhookKey, request);
        String expectedSignature = hmacSha256Hex(secret, canonicalString);
        String providedSignature = normalizeSignature(signatureHeader);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        ensureNonceNotReplayed(webhookKey, nonceHeader);
    }

    private void ensureNonceNotReplayed(String webhookKey, String nonce) {
        String nonceKey = NONCE_KEY_PREFIX + webhookKey + ":" + nonce;
        if (redisTemplate != null) {
            Boolean inserted = redisTemplate.opsForValue()
                    .setIfAbsent(nonceKey, "1", Duration.ofSeconds(nonceTtlSeconds));
            if (!Boolean.TRUE.equals(inserted)) {
                throw new IllegalArgumentException("Replay detected: nonce already used");
            }
            return;
        }

        long now = Instant.now().getEpochSecond();
        long expiresAt = now + nonceTtlSeconds;
        cleanupExpiredInMemoryNonce(now);
        Long existing = inMemoryNonceStore.putIfAbsent(nonceKey, expiresAt);
        if (existing != null && existing >= now) {
            throw new IllegalArgumentException("Replay detected: nonce already used");
        }
    }

    private void cleanupExpiredInMemoryNonce(long nowEpochSeconds) {
        inMemoryNonceStore.entrySet().removeIf(entry -> entry.getValue() < nowEpochSeconds);
    }

    private String buildCanonicalString(
            String timestamp,
            String nonce,
            String webhookKey,
            WebhookTriggerRequest request) {
        String inputPayload = request != null && request.getInputPayload() != null
                ? request.getInputPayload()
                : "";
        String idempotencyKey = request != null && request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : "";
        return timestamp + "\n"
                + nonce + "\n"
                + webhookKey + "\n"
                + inputPayload + "\n"
                + idempotencyKey;
    }

    private String normalizeSignature(String signatureHeader) {
        String value = signatureHeader.trim();
        if (value.regionMatches(true, 0, "sha256=", 0, 7)) {
            value = value.substring(7);
        }
        return value.toLowerCase();
    }

    private String hmacSha256Hex(String secret, String canonicalString) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toLowerCase();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate webhook signature", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}