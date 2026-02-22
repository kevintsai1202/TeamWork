package com.teamwork.gateway.config;

import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.repository.AiModelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Order(0)
@Slf4j
public class DefaultAiModelBootstrap implements ApplicationRunner {

    private final AiModelRepository aiModelRepository;

    public DefaultAiModelBootstrap(AiModelRepository aiModelRepository) {
        this.aiModelRepository = aiModelRepository;
    }

    /**
     * 啟動時將 .env 中的模型配置寫入 ai_models，並設為預設啟用模型。
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, String> envConfig = loadEnvConfig();

        String modelName = envConfig.getOrDefault("MODEL", "").trim();
        String baseUrl = envConfig.getOrDefault("BASE_URL", "").trim();
        String apiKey = envConfig.getOrDefault("API_KEY", "").trim();

        if (modelName.isEmpty() || baseUrl.isEmpty() || apiKey.isEmpty()) {
            log.info("Skip default AI model bootstrap: MODEL/BASE_URL/API_KEY not fully configured.");
            return;
        }

        List<AiModel> models = aiModelRepository.findAll();
        Optional<AiModel> existing = models.stream()
                .filter(m -> "OPENAI".equalsIgnoreCase(m.getProvider()))
                .filter(m -> modelName.equals(m.getName()))
                .findFirst();

        AiModel targetModel = existing.orElseGet(AiModel::new);
        targetModel.setProvider("OPENAI");
        targetModel.setName(modelName);
        targetModel.setEndpointUrl(baseUrl);
        targetModel.setApiKey(apiKey);
        targetModel.setActive(true);
        aiModelRepository.save(targetModel);

        // 將其他模型設為非預設，確保目前預設模型唯一且可預期。
        for (AiModel model : models) {
            if (!model.getId().equals(targetModel.getId()) && model.isActive()) {
                model.setActive(false);
                aiModelRepository.save(model);
            }
        }

        log.info("Default AI model is ready. provider=OPENAI, model={}, baseUrl={}", modelName, baseUrl);
    }

    /**
     * 讀取 .env 檔案，優先使用啟動目錄下 .env，其次上一層目錄 .env。
     */
    private Map<String, String> loadEnvConfig() {
        Map<String, String> values = new HashMap<>();
        mergeFromSystemEnv(values);

        for (Path candidate : List.of(Path.of(".env"), Path.of("..", ".env"))) {
            if (!Files.exists(candidate)) {
                continue;
            }
            try {
                List<String> lines = Files.readAllLines(candidate);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    int splitAt = trimmed.indexOf('=');
                    String key = trimmed.substring(0, splitAt).trim();
                    String value = trimmed.substring(splitAt + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
                log.info("Loaded default AI model config from {}", candidate.toAbsolutePath());
                break;
            } catch (IOException e) {
                log.warn("Failed to read .env file from {}", candidate.toAbsolutePath(), e);
            }
        }
        return values;
    }

    /**
     * 先載入系統環境變數，作為 .env 缺值時的備援。
     */
    private void mergeFromSystemEnv(Map<String, String> values) {
        putIfPresent(values, "MODEL", System.getenv("MODEL"));
        putIfPresent(values, "BASE_URL", System.getenv("BASE_URL"));
        putIfPresent(values, "API_KEY", System.getenv("API_KEY"));
    }

    /**
     * 僅在值有內容時覆寫設定。
     */
    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }
}

