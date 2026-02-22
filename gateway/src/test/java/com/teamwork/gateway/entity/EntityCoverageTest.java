package com.teamwork.gateway.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EntityCoverageTest {

    @Test
    void testTaskRecordLifecycleAndGettersSetters() throws Exception {
        // Test NoArgsConstructor and default ID generation
        TaskRecord record = new TaskRecord();
        assertThat(record.getId()).isNotNull();

        // Test Setters
        record.setId("custom-id");
        record.setParentTaskId("parent-1");
        record.setProfileId("profile-1");
        record.setStatus("RUNNING");
        record.setInputPayload("payload");
        LocalDateTime time = LocalDateTime.now();
        record.setCreatedAt(time);
        record.setUpdatedAt(time);

        // Test Getters
        assertThat(record.getId()).isEqualTo("custom-id");
        assertThat(record.getParentTaskId()).isEqualTo("parent-1");
        assertThat(record.getProfileId()).isEqualTo("profile-1");
        assertThat(record.getStatus()).isEqualTo("RUNNING");
        assertThat(record.getInputPayload()).isEqualTo("payload");
        assertThat(record.getCreatedAt()).isEqualTo(time);
        assertThat(record.getUpdatedAt()).isEqualTo(time);
        assertThat(record.toString()).contains("TaskRecord");
        assertThat(record.hashCode()).isNotZero();

        // Test JPA Lifecycle Hooks using Reflection or direct invocation
        // In reality these are protected/private, so we cast/invoke directly if
        // protected
        TaskRecord testLifecycleRecord = new TaskRecord() {
            @Override
            public void onCreate() {
                super.onCreate();
            }

            @Override
            public void onUpdate() {
                super.onUpdate();
            }
        };

        testLifecycleRecord.onCreate();
        assertThat(testLifecycleRecord.getCreatedAt()).isNotNull();
        assertThat(testLifecycleRecord.getUpdatedAt()).isNotNull();

        LocalDateTime oldUpdated = testLifecycleRecord.getUpdatedAt();
        Thread.sleep(10); // Ensure minimal time diff
        testLifecycleRecord.onUpdate();
        assertThat(testLifecycleRecord.getUpdatedAt()).isAfterOrEqualTo(oldUpdated);
    }

    @Test
    void testUserAccountLifecycleAndGettersSetters() {
        UserAccount user = new UserAccount();
        assertThat(user.getId()).isNotNull();

        user.setId("u-1");
        user.setTenantId("tenant-1");
        user.setUsername("alice");
        user.setDisplayName("Alice");
        user.setStatus("ACTIVE");

        assertThat(user.getId()).isEqualTo("u-1");
        assertThat(user.getTenantId()).isEqualTo("tenant-1");
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getStatus()).isEqualTo("ACTIVE");

        UserAccount lifecycleUser = new UserAccount() {
            @Override
            public void onCreate() {
                super.onCreate();
            }

            @Override
            public void onUpdate() {
                super.onUpdate();
            }
        };

        lifecycleUser.onCreate();
        assertThat(lifecycleUser.getCreatedAt()).isNotNull();
        assertThat(lifecycleUser.getUpdatedAt()).isNotNull();

        LocalDateTime oldUpdated = lifecycleUser.getUpdatedAt();
        lifecycleUser.onUpdate();
        assertThat(lifecycleUser.getUpdatedAt()).isAfterOrEqualTo(oldUpdated);
    }

    @Test
    void testTaskScheduleLifecycleAndGettersSetters() {
        TaskSchedule schedule = new TaskSchedule();
        assertThat(schedule.getId()).isNotNull();

        schedule.setTenantId("tenant-1");
        schedule.setName("Daily report");
        schedule.setEnabled(true);
        schedule.setScheduleType("CRON");
        schedule.setCronExpr("0 9 * * *");
        schedule.setIntervalSeconds(3600);
        schedule.setTimezone("Asia/Taipei");
        schedule.setTargetType("AGENT");
        schedule.setTargetRefId("apf-1");
        schedule.setPayloadJson("{\"input\":\"hi\"}");
        schedule.setPriority(1);
        schedule.setMaxConcurrentRuns(1);
        schedule.setContextMode("SHARED");
        schedule.setContextRetentionRuns(20);
        schedule.setContextMaxTokens(8000);
        schedule.setNotificationPolicyId("np-1");
        LocalDateTime nextRun = LocalDateTime.now().plusHours(1);
        schedule.setNextRunAt(nextRun);
        schedule.setCreatedBy("u-alice");

        assertThat(schedule.getTenantId()).isEqualTo("tenant-1");
        assertThat(schedule.getName()).isEqualTo("Daily report");
        assertThat(schedule.getScheduleType()).isEqualTo("CRON");
        assertThat(schedule.getTargetType()).isEqualTo("AGENT");
        assertThat(schedule.getContextMode()).isEqualTo("SHARED");
        assertThat(schedule.getNextRunAt()).isEqualTo(nextRun);

        TaskSchedule lifecycle = new TaskSchedule() {
            @Override
            public void onCreate() {
                super.onCreate();
            }

            @Override
            public void onUpdate() {
                super.onUpdate();
            }
        };
        lifecycle.onCreate();
        assertThat(lifecycle.getCreatedAt()).isNotNull();
        assertThat(lifecycle.getUpdatedAt()).isNotNull();

        LocalDateTime oldUpdated = lifecycle.getUpdatedAt();
        lifecycle.onUpdate();
        assertThat(lifecycle.getUpdatedAt()).isAfterOrEqualTo(oldUpdated);
    }

    @Test
    void testScheduleRunLifecycleAndGettersSetters() {
        ScheduleRun run = new ScheduleRun();
        assertThat(run.getId()).isNotNull();

        run.setScheduleId("sch-1");
        run.setTenantId("tenant-1");
        run.setTriggerType("AUTO");
        run.setStatus("RUNNING");
        LocalDateTime started = LocalDateTime.now();
        run.setStartedAt(started);
        run.setDurationMs(1200L);
        run.setResultSummary("ok");
        run.setErrorCode("NONE");
        run.setErrorMessage(null);

        assertThat(run.getScheduleId()).isEqualTo("sch-1");
        assertThat(run.getTriggerType()).isEqualTo("AUTO");
        assertThat(run.getDurationMs()).isEqualTo(1200L);

        ScheduleRun lifecycle = new ScheduleRun() {
            @Override
            public void onCreate() {
                super.onCreate();
            }
        };
        lifecycle.onCreate();
        assertThat(lifecycle.getCreatedAt()).isNotNull();
    }

    @Test
    void testScheduleContextSnapshotLifecycleAndGettersSetters() {
        ScheduleContextSnapshot snapshot = new ScheduleContextSnapshot();
        assertThat(snapshot.getId()).isNotNull();

        snapshot.setScheduleId("sch-1");
        snapshot.setRunId("run-1");
        snapshot.setTenantId("tenant-1");
        snapshot.setMessageCount(8);
        snapshot.setEstimatedTokens(2048);
        snapshot.setContextSummary("summary");
        snapshot.setToolResultSummary("tool summary");
        snapshot.setPendingTodos("todo-1,todo-2");

        assertThat(snapshot.getRunId()).isEqualTo("run-1");
        assertThat(snapshot.getEstimatedTokens()).isEqualTo(2048);
        assertThat(snapshot.getPendingTodos()).contains("todo-1");

        ScheduleContextSnapshot lifecycle = new ScheduleContextSnapshot() {
            @Override
            public void onCreate() {
                super.onCreate();
            }
        };
        lifecycle.onCreate();
        assertThat(lifecycle.getCreatedAt()).isNotNull();
    }

    @Test
    void testTaskTriggerLifecycleAndGettersSetters() {
        TaskTrigger trigger = new TaskTrigger();
        assertThat(trigger.getId()).isNotNull();

        trigger.setTenantId("tenant-1");
        trigger.setName("nightly");
        trigger.setTriggerSource("WEBHOOK");
        trigger.setTargetType("SKILL");
        trigger.setTargetRefId("skill.daily");
        trigger.setEnabled(true);
        trigger.setSecretRef("secret://x");
        trigger.setWebhookKey("wh_abc");
        trigger.setIdempotencyTtlSeconds(3600);
        trigger.setNotificationPolicyId("np-1");

        assertThat(trigger.getTriggerSource()).isEqualTo("WEBHOOK");
        assertThat(trigger.getWebhookKey()).isEqualTo("wh_abc");
        assertThat(trigger.getNotificationPolicyId()).isEqualTo("np-1");

        TaskTrigger lifecycle = new TaskTrigger() {
            @Override
            public void onCreate() {
                super.onCreate();
            }

            @Override
            public void onUpdate() {
                super.onUpdate();
            }
        };
        lifecycle.onCreate();
        assertThat(lifecycle.getCreatedAt()).isNotNull();
        assertThat(lifecycle.getUpdatedAt()).isNotNull();

        LocalDateTime oldUpdated = lifecycle.getUpdatedAt();
        lifecycle.onUpdate();
        assertThat(lifecycle.getUpdatedAt()).isAfterOrEqualTo(oldUpdated);
    }

    @Test
    void testContextDeletionAuditLifecycleAndGettersSetters() {
        ContextDeletionAudit audit = new ContextDeletionAudit();
        assertThat(audit.getId()).isNotNull();

        audit.setTaskId("task-1");
        audit.setMode("RANGE");
        audit.setRemovedCount(3);
        audit.setFromIndex(2);
        audit.setToIndex(4);
        audit.setReason("cleanup");
        audit.setOperatorId("system");

        assertThat(audit.getTaskId()).isEqualTo("task-1");
        assertThat(audit.getMode()).isEqualTo("RANGE");
        assertThat(audit.getRemovedCount()).isEqualTo(3);
        assertThat(audit.getFromIndex()).isEqualTo(2);
        assertThat(audit.getToIndex()).isEqualTo(4);
        assertThat(audit.getReason()).isEqualTo("cleanup");
        assertThat(audit.getOperatorId()).isEqualTo("system");

        ContextDeletionAudit lifecycle = new ContextDeletionAudit() {
            @Override
            public void onCreate() {
                super.onCreate();
            }
        };
        lifecycle.onCreate();
        assertThat(lifecycle.getCreatedAt()).isNotNull();
    }
}
