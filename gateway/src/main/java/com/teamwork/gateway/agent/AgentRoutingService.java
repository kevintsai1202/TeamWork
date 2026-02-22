package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AgentProfile;
import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.repository.AgentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 路由服務：集中 sub-agent/sandbox/provider 決策，作為單一路由真相來源。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRoutingService {

    private final SubAgentDescriptorRepository subAgentDescriptorRepository;
    private final SubAgentRouter subAgentRouter;
    private final AgentProfileRepository agentProfileRepository;
    private final UnifiedAgentRegistry unifiedAgentRegistry;

    /**
     * 建立本次任務的完整路由方案。
     */
    public RoutingPlan plan(
            String taskId,
            String inputPayload,
            String profileId,
            AiModel aiModel,
            ChatModel chatModel) {

        List<SubAgentDescriptor> descriptors = subAgentDescriptorRepository.findEnabledDescriptors();
        SubAgentRoutingDecision routingDecision = subAgentRouter.route(inputPayload, chatModel, descriptors);

        AgentProfile profile = loadAgentProfile(profileId);
        boolean sandboxEnabled = profile != null && profile.isSandboxEnabled();
        String sandboxType = (profile != null && profile.getSandboxType() != null)
                ? profile.getSandboxType() : "LOCAL";
        String dockerImage = (profile != null) ? profile.getDockerImage() : null;

        AgentExecutionContext executionContext = new AgentExecutionContext(
                taskId,
                inputPayload,
                aiModel,
                chatModel,
                routingDecision.selected().name(),
                routingDecision.selected().referencePath(),
                routingDecision.selected().ownerProvider(),
                routingDecision.fallbackUsed(),
                sandboxEnabled,
                extractLanguage(inputPayload),
                inputPayload,
                0L,
                sandboxType,
                dockerImage
        );

        UnifiedAgentProvider provider = unifiedAgentRegistry.resolve(executionContext);

        log.info("Routing plan built. taskId={}, subAgent={}, fallbackUsed={}, sandboxEnabled={}, sandboxType={}",
                taskId,
                routingDecision.selected().name(),
                routingDecision.fallbackUsed(),
                sandboxEnabled,
                sandboxType);

        return new RoutingPlan(executionContext, provider, routingDecision);
    }

    /**
     * 載入 AgentProfile，若 profileId 為空或找不到則回傳 null（降級為非沙盒模式）。
     */
    private AgentProfile loadAgentProfile(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }
        return agentProfileRepository.findById(profileId).orElse(null);
    }

    /**
     * 從輸入 payload 嘗試提取語言標記（格式：[lang:python]...），找不到時回傳空字串。
     */
    private String extractLanguage(String inputPayload) {
        if (inputPayload == null) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\[lang(?:uage)?:([a-z]+)]", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(inputPayload);
        return matcher.find() ? matcher.group(1).toLowerCase() : "";
    }
}
