package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.WebhookTriggerRequest;
import com.teamwork.gateway.entity.TaskTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookSecurityServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private WebhookSecurityService webhookSecurityService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        webhookSecurityService = new WebhookSecurityService(redisTemplate, 300, 300);
    }

    @Test
    void validate_ShouldPass_WhenSignatureAndNonceAreValid() {
        TaskTrigger trigger = new TaskTrigger();
        trigger.setSecretRef("secret-123");

        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload("run report");
        request.setIdempotencyKey("evt-1");

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "nonce-ok";
        String signature = sign("secret-123", timestamp, nonce, "wh_1", request);

        when(valueOperations.setIfAbsent(eq("webhook:nonce:wh_1:nonce-ok"), eq("1"), any()))
                .thenReturn(true);

        assertThatCode(() -> webhookSecurityService.validate(
                trigger,
                "wh_1",
                request,
                timestamp,
                nonce,
                signature)).doesNotThrowAnyException();
    }

    @Test
    void validate_ShouldThrow_WhenSignatureInvalid() {
        TaskTrigger trigger = new TaskTrigger();
        trigger.setSecretRef("secret-123");

        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload("run report");
        request.setIdempotencyKey("evt-1");

        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> webhookSecurityService.validate(
                trigger,
                "wh_1",
                request,
                timestamp,
                "nonce-bad",
                "invalid-signature"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid webhook signature");
    }

    @Test
    void validate_ShouldThrow_WhenNonceReplayed() {
        TaskTrigger trigger = new TaskTrigger();
        trigger.setSecretRef("secret-123");

        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload("run report");
        request.setIdempotencyKey("evt-2");

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "nonce-replay";
        String signature = sign("secret-123", timestamp, nonce, "wh_1", request);

        when(valueOperations.setIfAbsent(eq("webhook:nonce:wh_1:nonce-replay"), eq("1"), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> webhookSecurityService.validate(
                trigger,
                "wh_1",
                request,
                timestamp,
                nonce,
                signature))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Replay detected");
    }

    @Test
    void validate_ShouldThrow_WhenTimestampOutOfWindow() {
        TaskTrigger trigger = new TaskTrigger();
        trigger.setSecretRef("secret-123");

        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload("run report");

        long oldTimestamp = Instant.now().minusSeconds(1000).getEpochSecond();

        assertThatThrownBy(() -> webhookSecurityService.validate(
                trigger,
                "wh_1",
                request,
                String.valueOf(oldTimestamp),
                "nonce-old",
                "sig"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of allowed window");
    }

    private String sign(
            String secret,
            String timestamp,
            String nonce,
            String webhookKey,
            WebhookTriggerRequest request) {
        String inputPayload = request.getInputPayload() == null ? "" : request.getInputPayload();
        String idempotencyKey = request.getIdempotencyKey() == null ? "" : request.getIdempotencyKey();
        String canonicalString = timestamp + "\n"
                + nonce + "\n"
                + webhookKey + "\n"
                + inputPayload + "\n"
                + idempotencyKey;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
