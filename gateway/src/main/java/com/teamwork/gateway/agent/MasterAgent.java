package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.event.TaskStatusChangeEvent;
import com.teamwork.gateway.repository.TaskRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterAgent {

    private final TaskRecordRepository taskRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @Transactional
    public void processTask(String taskId, String inputPayload) {
        log.info("MasterAgent starts processing Task ID: {}", taskId);

        Optional<TaskRecord> recordOpt = taskRecordRepository.findById(taskId);
        if (recordOpt.isEmpty()) {
            log.error("TaskRecord not found for ID: {}. Aborting process.", taskId);
            return;
        }

        TaskRecord record = recordOpt.get();
        try {
            // 標記為執行中
            updateTaskStatus(record, "RUNNING");

            // TODO: (Phase 1.5) 與 Spring AI ChatClient 串接，根據 inputPayload 調用 LLM 思考邏輯
            // 這裡暫時使用 Thread.sleep 模擬大腦推論的耗時操作
            long thinkTime = (long) (Math.random() * 2000 + 1000);
            log.debug("Simulating Agent thinking for {} ms...", thinkTime);
            Thread.sleep(thinkTime);

            // 標記為完成
            updateTaskStatus(record, "COMPLETED");
            log.info("MasterAgent completed processing Task ID: {}", taskId);

        } catch (InterruptedException e) {
            log.warn("MasterAgent thread was interrupted for Task ID: {}", taskId);
            updateTaskStatus(record, "FAILED");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("MasterAgent encountered an error while processing Task ID: {}", taskId, e);
            updateTaskStatus(record, "FAILED");
        }
    }

    private void updateTaskStatus(TaskRecord record, String newStatus) {
        record.setStatus(newStatus);
        taskRecordRepository.save(record);
        log.info("Task status updated. TaskId: {}, Status: {}", record.getId(), newStatus);

        // 放出狀態更新事件給 SSE (或其他 Listener) 訂閱
        eventPublisher.publishEvent(new TaskStatusChangeEvent(this, record.getId(), newStatus, null));
    }
}
