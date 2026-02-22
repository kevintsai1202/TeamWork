package com.teamwork.gateway.agent;

import java.util.List;

/**
 * 取得可用 sub-agent 描述來源。
 */
public interface SubAgentDescriptorRepository {

    /**
     * 取得目前啟用的 sub-agent 描述。
     */
    List<SubAgentDescriptor> findEnabledDescriptors();
}
