package com.teamwork.gateway.event;

/**
 * 排程觸發事件：由 runtime scheduler 發佈，交由執行流程處理。
 */
public record ScheduleTriggeredEvent(String scheduleId, String triggerType) {
}
