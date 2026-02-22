package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.event.TaskStatusChangeEvent;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class MasterAgentTools {

    @Autowired(required = false)
    private SkillsCatalogService skillsCatalogService;

    @Autowired(required = false)
    private TaskRecordRepository taskRecordRepository;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private PendingUserQuestionStore pendingUserQuestionStore;

    @Autowired(required = false)
    private AskUserToolPolicyService askUserToolPolicyService;

    MasterAgentTools(
            SkillsCatalogService skillsCatalogService,
            TaskRecordRepository taskRecordRepository,
            ApplicationEventPublisher eventPublisher,
            PendingUserQuestionStore pendingUserQuestionStore,
            AskUserToolPolicyService askUserToolPolicyService) {
        this.skillsCatalogService = skillsCatalogService;
        this.taskRecordRepository = taskRecordRepository;
        this.eventPublisher = eventPublisher;
        this.pendingUserQuestionStore = pendingUserQuestionStore;
        this.askUserToolPolicyService = askUserToolPolicyService;
    }

    MasterAgentTools(
            SkillsCatalogService skillsCatalogService,
            TaskRecordRepository taskRecordRepository,
            ApplicationEventPublisher eventPublisher,
            PendingUserQuestionStore pendingUserQuestionStore) {
        this(skillsCatalogService, taskRecordRepository, eventPublisher, pendingUserQuestionStore, null);
    }

    public MasterAgentTools() {
    }

    /**
     * 取得指定時區的目前時間，供 Agent 在需要時間資訊時調用。
     */
    @Tool(description = "取得指定時區的目前時間，回傳 ISO-8601 格式字串")
    public String getCurrentTime(
            @ToolParam(description = "IANA 時區 ID，例如 Asia/Taipei、UTC；可為空，預設 UTC") String zoneId) {
        if (!isToolEnabled(AskUserToolPolicyService.TOOL_GET_CURRENT_TIME)) {
            return "getCurrentTime tool is disabled";
        }
        ZoneId zone = (zoneId == null || zoneId.isBlank()) ? ZoneId.of("UTC") : ZoneId.of(zoneId);
        return ZonedDateTime.now(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * 列出可用 skills 名稱。
     */
    @Tool(description = "列出目前可用的 skills 名稱，回傳逗號分隔字串")
    public String listAvailableSkills() {
        if (!isToolEnabled(AskUserToolPolicyService.TOOL_LIST_AVAILABLE_SKILLS)) {
            return "";
        }
        if (skillsCatalogService == null) {
            return "";
        }
        List<String> names = skillsCatalogService.listSkillNames();
        return String.join(",", names);
    }

    /**
     * 讀取指定 skill 內容。
     */
    @Tool(description = "讀取指定 skill 內容（markdown）")
    public String readSkillContent(
            @ToolParam(description = "skill 名稱（不含副檔名）") String skillName) {
        if (!isToolEnabled(AskUserToolPolicyService.TOOL_READ_SKILL_CONTENT)) {
            return "readSkillContent tool is disabled";
        }
        if (skillsCatalogService == null) {
            return "Skills catalog is not available.";
        }
        try {
            return skillsCatalogService.readSkill(skillName);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * 重新整理 skills 快取。
     */
    @Tool(description = "重新整理 skills 快取，回傳目前可用 skill 數量")
    public String refreshSkillsCache() {
        if (!isToolEnabled(AskUserToolPolicyService.TOOL_REFRESH_SKILLS_CACHE)) {
            return "0";
        }
        if (skillsCatalogService == null) {
            return "0";
        }
        return String.valueOf(skillsCatalogService.refreshNow());
    }

    /**
     * 由代理提出問題給使用者，任務狀態進入 WAITING_USER_INPUT。
     */
    @Tool(description = "向使用者提問，並將任務狀態標記為 WAITING_USER_INPUT")
    public String askUserQuestion(
            @ToolParam(description = "目前任務 ID") String taskId,
            @ToolParam(description = "要詢問使用者的問題") String question) {
        if (!isToolEnabled(AskUserToolPolicyService.TOOL_ASK_QUESTION)) {
            return "askUserQuestion tool is disabled";
        }
        if (taskId == null || taskId.isBlank()) {
            return "taskId is required";
        }
        if (question == null || question.isBlank()) {
            return "question is required";
        }
        if (taskRecordRepository == null || pendingUserQuestionStore == null || eventPublisher == null) {
            return "ask-user-question is not available in current runtime";
        }

        Optional<TaskRecord> taskOpt = taskRecordRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return "Task not found: " + taskId;
        }

        TaskRecord task = taskOpt.get();
        task.setStatus("WAITING_USER_INPUT");
        taskRecordRepository.save(task);

        pendingUserQuestionStore.put(taskId, question);
        eventPublisher.publishEvent(new TaskStatusChangeEvent(this, taskId, "WAITING_USER_INPUT", question));
        return "Question sent. taskId=" + taskId;
    }

    /**
     * 列出目前待使用者回覆的 taskId。
     */
    @Tool(description = "列出目前等待使用者回覆的 taskId 清單")
    public String listPendingUserQuestions() {
        if (!isToolEnabled(AskUserToolPolicyService.TOOL_LIST_PENDING)) {
            return "";
        }
        if (pendingUserQuestionStore == null) {
            return "";
        }
        return String.join(",", pendingUserQuestionStore.taskIds());
    }

    /**
     * 接收使用者回覆，任務狀態切回 RUNNING。
     */
    @Tool(description = "接收使用者回覆，將任務狀態從 WAITING_USER_INPUT 切回 RUNNING")
    public String submitUserAnswer(
            @ToolParam(description = "任務 ID") String taskId,
            @ToolParam(description = "使用者回覆內容") String answer) {
        if (!isToolEnabled(AskUserToolPolicyService.TOOL_SUBMIT_ANSWER)) {
            return "submitUserAnswer tool is disabled";
        }
        if (taskId == null || taskId.isBlank()) {
            return "taskId is required";
        }
        if (answer == null || answer.isBlank()) {
            return "answer is required";
        }
        if (taskRecordRepository == null || pendingUserQuestionStore == null || eventPublisher == null) {
            return "submit-user-answer is not available in current runtime";
        }

        Optional<TaskRecord> taskOpt = taskRecordRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return "Task not found: " + taskId;
        }

        TaskRecord task = taskOpt.get();
        task.setStatus("RUNNING");
        taskRecordRepository.save(task);
        pendingUserQuestionStore.remove(taskId);
        eventPublisher.publishEvent(new TaskStatusChangeEvent(this, taskId, "RUNNING", answer));
        return "Answer accepted. taskId=" + taskId;
    }

    private boolean isToolEnabled(String toolName) {
        if (askUserToolPolicyService == null) {
            return true;
        }
        return switch (toolName) {
            case AskUserToolPolicyService.TOOL_GET_CURRENT_TIME -> askUserToolPolicyService.canGetCurrentTime();
            case AskUserToolPolicyService.TOOL_LIST_AVAILABLE_SKILLS -> askUserToolPolicyService.canListAvailableSkills();
            case AskUserToolPolicyService.TOOL_READ_SKILL_CONTENT -> askUserToolPolicyService.canReadSkillContent();
            case AskUserToolPolicyService.TOOL_REFRESH_SKILLS_CACHE -> askUserToolPolicyService.canRefreshSkillsCache();
            case AskUserToolPolicyService.TOOL_ASK_QUESTION -> askUserToolPolicyService.canAskQuestion();
            case AskUserToolPolicyService.TOOL_LIST_PENDING -> askUserToolPolicyService.canListPending();
            case AskUserToolPolicyService.TOOL_SUBMIT_ANSWER -> askUserToolPolicyService.canSubmitAnswer();
            default -> false;
        };
    }
}

