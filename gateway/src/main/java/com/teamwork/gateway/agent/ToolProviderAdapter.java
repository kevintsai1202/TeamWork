package com.teamwork.gateway.agent;

import java.util.List;

/**
 * 工具提供者抽象。
 *
 * <p>不同來源（本地/遠端/skills）可透過此介面回報可用工具清單，
 * 由 ToolCapabilityRegistry 統一彙整。</p>
 */
public interface ToolProviderAdapter {

    /**
     * 回傳此 provider 提供的工具描述清單。
     */
    List<ToolDescriptor> listTools();
}
