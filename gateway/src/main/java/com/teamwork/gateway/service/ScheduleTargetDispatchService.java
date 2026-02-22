package com.teamwork.gateway.service;

import com.teamwork.gateway.agent.MasterAgent;
import com.teamwork.gateway.agent.SkillsCatalogService;
import com.teamwork.gateway.entity.ScheduleContextSnapshot;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.entity.TaskSchedule;
import com.teamwork.gateway.entity.ToolConfig;
import com.teamwork.gateway.repository.AgentProfileRepository;
import com.teamwork.gateway.repository.TaskRecordRepository;
import com.teamwork.gateway.repository.ToolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * 排程目標分派服務：統一處理 AGENT/TOOL/SKILL 路由。
 */
@Service
@RequiredArgsConstructor
public class ScheduleTargetDispatchService {

    private final AgentProfileRepository agentProfileRepository;
    private final ToolConfigRepository toolConfigRepository;
    private final SkillsCatalogService skillsCatalogService;
    private final TaskRecordRepository taskRecordRepository;
    private final MasterAgent masterAgent;

    /**
     * 依 schedule.targetType 分派執行目標。
     */
    public DispatchResult dispatch(TaskSchedule schedule, Optional<ScheduleContextSnapshot> previousSnapshot) {
        String targetType = normalize(schedule.getTargetType());
        return switch (targetType) {
            case "AGENT" -> dispatchAgent(schedule, previousSnapshot);
            case "TOOL" -> dispatchTool(schedule, previousSnapshot);
            case "SKILL" -> dispatchSkill(schedule, previousSnapshot);
            default -> throw new IllegalArgumentException("Unsupported schedule targetType: " + schedule.getTargetType());
        };
    }

    /**
     * AGENT 目標：直接以 targetRefId 作為 profileId 建立 task 並非同步派發。
     */
    private DispatchResult dispatchAgent(TaskSchedule schedule, Optional<ScheduleContextSnapshot> previousSnapshot) {
        String profileId = schedule.getTargetRefId();
        if (profileId == null || profileId.isBlank() || !agentProfileRepository.existsById(profileId)) {
            throw new IllegalArgumentException("Agent profile not found: " + profileId);
        }

        TaskRecord task = new TaskRecord();
        task.setProfileId(profileId);
        task.setParentTaskId(null);
        task.setStatus("PENDING");
        task.setInputPayload(buildScheduleInput(schedule, previousSnapshot, "AGENT", null));
        TaskRecord saved = taskRecordRepository.save(task);

        masterAgent.processTask(saved.getId(), saved.getInputPayload());

        return new DispatchResult(
                saved.getId(),
                "AGENT",
                "profileId=" + profileId,
                "Dispatched to AGENT profile");
    }

    /**
     * TOOL 目標：驗證工具存在後，以限定工具上下文派發到 MasterAgent。
     */
    private DispatchResult dispatchTool(TaskSchedule schedule, Optional<ScheduleContextSnapshot> previousSnapshot) {
        String targetRefId = schedule.getTargetRefId();
        ToolConfig tool = resolveTool(targetRefId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + targetRefId));

        String selectedProfileId = fallbackProfileId();
        TaskRecord task = new TaskRecord();
        task.setProfileId(selectedProfileId);
        task.setParentTaskId(null);
        task.setStatus("PENDING");
        task.setInputPayload(buildScheduleInput(schedule, previousSnapshot, "TOOL", tool.getName()));
        TaskRecord saved = taskRecordRepository.save(task);

        masterAgent.processTask(saved.getId(), saved.getInputPayload());

        return new DispatchResult(
                saved.getId(),
                "TOOL",
                "toolName=" + tool.getName(),
                "Dispatched to TOOL flow via MasterAgent");
    }

    /**
     * SKILL 目標：驗證 skill 存在後，將 skill 內容作為上下文派發到 MasterAgent。
     */
    private DispatchResult dispatchSkill(TaskSchedule schedule, Optional<ScheduleContextSnapshot> previousSnapshot) {
        String skillName = schedule.getTargetRefId();
        String skillContent = skillsCatalogService.readSkill(skillName);

        String selectedProfileId = fallbackProfileId();
        TaskRecord task = new TaskRecord();
        task.setProfileId(selectedProfileId);
        task.setParentTaskId(null);
        task.setStatus("PENDING");
        task.setInputPayload(buildScheduleInput(schedule, previousSnapshot, "SKILL", skillContent));
        TaskRecord saved = taskRecordRepository.save(task);

        masterAgent.processTask(saved.getId(), saved.getInputPayload());

        return new DispatchResult(
                saved.getId(),
                "SKILL",
                "skillName=" + skillName,
                "Dispatched to SKILL flow via MasterAgent");
    }

    private Optional<ToolConfig> resolveTool(String targetRefId) {
        if (targetRefId == null || targetRefId.isBlank()) {
            return Optional.empty();
        }
        Optional<ToolConfig> byId = toolConfigRepository.findById(targetRefId);
        if (byId.isPresent()) {
            return byId;
        }
        return toolConfigRepository.findByName(targetRefId);
    }

    private String fallbackProfileId() {
        return agentProfileRepository.findAll().stream()
                .findFirst()
                .map(profile -> profile.getId())
                .orElse("schedule-system-profile");
    }

    private String buildScheduleInput(
            TaskSchedule schedule,
            Optional<ScheduleContextSnapshot> previousSnapshot,
            String targetType,
            String extraContext) {

        StringBuilder builder = new StringBuilder();
        builder.append("[SCHEDULE_TARGET]")
                .append(" type=").append(targetType)
                .append(" targetRefId=").append(schedule.getTargetRefId())
                .append("\n");

        if (previousSnapshot.isPresent()) {
            String summary = previousSnapshot.get().getContextSummary();
            if (summary != null && !summary.isBlank()) {
                builder.append("[SHARED_CONTEXT]").append(summary).append("\n");
            }
        }

        if (extraContext != null && !extraContext.isBlank()) {
            builder.append("[TARGET_CONTEXT]").append(extraContext).append("\n");
        }

        String payload = schedule.getPayloadJson() == null ? "" : schedule.getPayloadJson();
        builder.append("[PAYLOAD]").append(payload);
        return builder.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 分派結果：回傳本次路由摘要與關聯 taskId。
     */
    public record DispatchResult(String taskId, String targetType, String targetSummary, String resultSummary) {
    }
}
