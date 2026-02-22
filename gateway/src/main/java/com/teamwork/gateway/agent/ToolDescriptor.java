package com.teamwork.gateway.agent;

/**
 * 工具描述物件：統一描述可被 ChatClient.tools(...) 注入的工具。
 *
 * @param name       工具名稱（需對應 DB tool_configs.name）
 * @param type       工具型別（例如 BUILT_IN）
 * @param provider   工具來源提供者（例如 LOCAL）
 * @param toolObject 實際工具物件
 */
public record ToolDescriptor(String name, String type, String provider, Object toolObject) {
}
