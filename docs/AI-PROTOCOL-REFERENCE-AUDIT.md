# AI API 多协议参考模块审查

审查日期：2026-10-11  
目标版本：APS 0.1.1（版本名按当前发布决策回调；协议能力保持不变）  
状态：已完成静态比较；第三方依赖接入策略为“复用协议设计与测试经验，保持 APS 现有网络栈”，后续可按能力再引入依赖。

## 1. 参考项目

### tddworks/openai-kotlin

- 仓库：https://github.com/tddworks/openai-kotlin
- 许可证：Apache-2.0
- 公开说明：Kotlin Multiplatform 客户端，包含 OpenAI、Anthropic、Gemini、Ollama 客户端和统一 Gateway；提供流式调用及类型化请求模型。
- 可借鉴：每个服务商独立客户端、统一 Gateway、模块边界、流式输出测试和请求/响应类型分离。
- 暂不整库接入的原因：APS 已有 OkHttp、Gson、Coroutine 和自有 AIConnector / AIProfileStore；直接引入其 JVM Gateway 会额外带入另一套网络与依赖体系。现阶段更小的改动是保留现有 APS 调用契约，用独立协议适配器实现同类边界，并用契约测试验证。
- 再评估条件：当工具调用、多模态、重试策略或更多协议使自维护适配器成本超过依赖成本时，比较具体模块的依赖树、Android 构建兼容性、API 覆盖和许可证后再决定。

### X4n71uM/AI-API-Client（GPT Mobile）

- 仓库：https://github.com/X4n71uM/AI-API-Client
- 可借鉴：Android 端按 Profile 管理多个模型服务、分别处理 OpenAI / Anthropic / Gemini / Ollama 协议、支持自定义地址。
- 接入限制：其仓库许可证和具体版本必须在实际复制代码或引入依赖前重新核实；不直接复制整套应用代码或 UI。

### android-ai-chat-sdk

- 仓库：https://github.com/salmanashraf/android-ai-chat-sdk
- 可借鉴：Provider Registry、统一配置入口、先验证连接配置再开始对话的交互思路。
- 不直接移植 UI：APS 现有界面和数据模型不同，且该项目的 Compose UI / Room 历史不属于本次协议层必需范围。

## 2. APS 当前实施策略

不把某个完整聊天应用当作 APS 的内部模块直接搬入。当前采用薄适配器方案：

- AIProtocol：稳定的协议 ID 与显示名称分离；自定义协议不需要先修改公共数据类型。
- AIConnectorFactory：内置协议映射，并允许按稳定字符串 ID 注入额外适配器。
- AIModelCatalogConnector：把模型目录与真实聊天测试作为可选能力，不强迫所有 Connector 实现相同方法。
- OpenAICompatibleConnector：保留当前兼容协议实现。
- AnthropicConnector：独立处理 Messages 请求、x-api-key、版本头、内容块和 SSE。
- GeminiConnector：独立处理 generateContent、模型目录、x-goog-api-key 和 SSE。
- AIProfileStore：保存协议 ID；没有协议字段的旧 Profile 继续按 OpenAI 兼容协议解释。
- 设置页：选择协议后，由所选 Connector 处理模型列表和实际聊天测试。

## 3. 验收要求

- [ ] 三种内置协议分别验证请求 URL、认证头、消息体和响应解析。
- [ ] 流式响应与取消行为通过测试；服务商特有的 SSE 事件不能混用。
- [ ] 模型列表失败必须保留 HTTP 状态和安全的错误摘要，并允许手动填写模型 ID。
- [ ] API Key 不进入日志或普通 UI 摘要。
- [ ] 旧 Profile 缺少协议字段时不丢失，默认映射到 OpenAI 兼容协议。
- [ ] 新增协议只需注册适配器并提供契约测试，不修改对话页核心调用流程。
- [ ] Android 单元测试、构建工作流通过；另行完成设备启动与真实服务端测试。

本文件是参考审查和施工记录，不代表所有验收项已经通过。实际状态以对应测试和工作流结果为准。
