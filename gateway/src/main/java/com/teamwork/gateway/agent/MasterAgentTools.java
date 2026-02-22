package com.teamwork.gateway.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MasterAgentTools {

    /**
     * 取得指定時區的目前時間，供 Agent 在需要時間資訊時調用。
     */
    @Tool(description = "取得指定時區的目前時間，回傳 ISO-8601 格式字串")
    public String getCurrentTime(
            @ToolParam(description = "IANA 時區 ID，例如 Asia/Taipei、UTC；可為空，預設 UTC") String zoneId) {
        ZoneId zone = (zoneId == null || zoneId.isBlank()) ? ZoneId.of("UTC") : ZoneId.of(zoneId);
        return ZonedDateTime.now(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}

