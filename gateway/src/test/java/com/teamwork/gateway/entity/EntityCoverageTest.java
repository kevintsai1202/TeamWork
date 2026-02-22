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
}
