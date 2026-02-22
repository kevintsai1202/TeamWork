package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.teamwork.gateway.event.TaskStatusChangeEvent;
import java.util.Optional;
import java.util.List;

import com.teamwork.gateway.ai.ChatModelFactory;
import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.repository.AiModelRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterAgentTest {

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AiModelRepository aiModelRepository;

    @Mock
    private ChatModelFactory chatModelFactory;

    @Mock
    private AgentRoutingService agentRoutingService;

    @Mock
    private AgentObservabilityService agentObservabilityService;

    @Mock
    private UnifiedAgentProvider unifiedAgentProvider;

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private MasterAgent masterAgent;

    @Test
    void processTask_WhenTaskNotFound_ShouldAbort() {
        // Arrange
        when(taskRecordRepository.findById("unknown")).thenReturn(Optional.empty());

        // Act
        masterAgent.processTask("unknown", "payload");

        // Assert
        verify(taskRecordRepository, never()).save(any(TaskRecord.class));
        verify(agentObservabilityService).recordTaskStarted("unknown");
    }

    @Test
    void processTask_WhenTaskExists_ShouldCompleteSuccessfully() {
        // Arrange
        TaskRecord mockRecord = new TaskRecord();
        // Simulate DB finding the task
        when(taskRecordRepository.findById("task-123")).thenReturn(Optional.of(mockRecord));

        // Simulate reading active AI Model from DB
        AiModel mockAiModel = new AiModel();
        mockAiModel.setName("mock-model");
        mockAiModel.setProvider("OPENAI");
        mockAiModel.setActive(true);
        when(aiModelRepository.findAll()).thenReturn(List.of(mockAiModel));

        when(chatModelFactory.createChatModel(mockAiModel)).thenReturn(chatModel);
        SubAgentDescriptor selected = new SubAgentDescriptor(
            "general-researcher",
            "desc",
            List.of("getCurrentTime"),
            "CLAUDE",
            "agents/subagents/general-researcher.md",
            true,
            0);
        SubAgentRoutingDecision routingDecision = new SubAgentRoutingDecision(
            selected,
            0.5,
            0.6,
            0.56,
            false,
            "matched");
        AgentExecutionContext executionContext = new AgentExecutionContext(
            "task-123",
            "input payload",
            mockAiModel,
            chatModel,
            "general-researcher",
            "agents/subagents/general-researcher.md",
            "CLAUDE",
            false,
            false,
            "",
            "input payload",
            0L,
            "LOCAL",
            null);
        RoutingPlan routingPlan = new RoutingPlan(executionContext, unifiedAgentProvider, routingDecision);
        when(agentRoutingService.plan("task-123", "input payload", null, mockAiModel, chatModel))
            .thenReturn(routingPlan);
        when(unifiedAgentProvider.execute(any(AgentExecutionContext.class))).thenReturn("Mock AI Response");

        // Act
        masterAgent.processTask("task-123", "input payload");

        // Assert
        verify(taskRecordRepository, atLeastOnce()).save(mockRecord);
        // Ensure final status is COMPLETED
        assert mockRecord.getStatus().equals("COMPLETED");

        // Verify that events were published
        verify(eventPublisher, atLeastOnce()).publishEvent(any(TaskStatusChangeEvent.class));

        verify(agentRoutingService).plan("task-123", "input payload", null, mockAiModel, chatModel);
        verify(agentObservabilityService).recordTaskStarted("task-123");
        verify(agentObservabilityService).recordTaskCompleted(eq("task-123"), anyLong());

        ArgumentCaptor<AgentExecutionContext> contextCaptor = ArgumentCaptor.forClass(AgentExecutionContext.class);
        verify(unifiedAgentProvider).execute(contextCaptor.capture());
        AgentExecutionContext context = contextCaptor.getValue();
        assert context.selectedSubAgentName().equals("general-researcher");
        assert context.selectedSubAgentReferencePath().equals("agents/subagents/general-researcher.md");
        // T14 驗證：sandbox 欄位（無 profile → sandboxEnabled=false）
        assert !context.sandboxEnabled();
    }

    @Test
    void processTask_WhenExceptionOccurs_ShouldMarkFailed() {
        // Arrange
        TaskRecord mockRecord = new TaskRecord();
        mockRecord.setId("task-456");
        // Simulate DB finding the task
        when(taskRecordRepository.findById("task-456")).thenReturn(Optional.of(mockRecord));
        // Simulate a DB error ONLY on the first try (when setting to RUNNING)
        // On the second save attempt inside catch block (setting to FAILED), it should
        // succeed.
        when(taskRecordRepository.save(any(TaskRecord.class)))
                .thenThrow(new RuntimeException("DB Error"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        masterAgent.processTask("task-456", "payload");

        // Assert
        // Expect final status updated to FAILED in memory, though save might still
        // throw in mock
        // Normalmente first save throws, so catch block runs, changing status to
        // FAILED and calls save again.
        assert mockRecord.getStatus().equals("FAILED");

        // Verify that event was published correctly
        verify(eventPublisher, atLeastOnce()).publishEvent(any(TaskStatusChangeEvent.class));
        verify(agentObservabilityService).recordTaskStarted("task-456");
        verify(agentObservabilityService).recordTaskFailed(eq("task-456"), any(Exception.class));
    }

    @Test
    void processTask_WhenInterrupted_ShouldMarkFailed() throws InterruptedException {
        // Arrange
        TaskRecord mockRecord = new TaskRecord();
        mockRecord.setId("task-789");
        when(taskRecordRepository.findById("task-789")).thenReturn(Optional.of(mockRecord));

        // Start processTask in a separate thread so we can interrupt it during
        // Thread.sleep
        Thread testThread = new Thread(() -> masterAgent.processTask("task-789", "payload"));
        testThread.start();

        // Give it a tiny moment to start and enter Thread.sleep
        Thread.sleep(500);

        // Act
        testThread.interrupt();

        // Wait for it to finish its catch block
        testThread.join();

        // Assert
        verify(taskRecordRepository, atLeast(2)).save(mockRecord);
        assert mockRecord.getStatus().equals("FAILED");
        verify(eventPublisher, atLeastOnce()).publishEvent(any(TaskStatusChangeEvent.class));
        verify(agentObservabilityService).recordTaskStarted("task-789");
        verify(agentObservabilityService).recordTaskFailed(eq("task-789"), any(Exception.class));
    }
}
