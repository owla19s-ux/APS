# APS 当前状态与工作记录

更新时间：2026-10-15

> 当前事实以代码、Commit、Actions / Verify、APK / 真机结果为准。本文件用于记录当前阶段状态，不作为历史架构的复原依据。

## 一、第一阶段产品定位

APS 当前第一阶段定位：

> **AI 外部能力连接器 + 调度分配器。**

核心 UI：

- **空间页**：Space → 目录 → Context 的主要人机工作面；
- **对话页**：独立 AI 对话 / 查询 / 测试；
- **设置页**：API、GitHub 等配置。

空间页中的 Space、目录、Context、AI、状态、操作记录主要用于人类查看和操作。

## 二、当前正式模型

```text
Space
├── 名称
└── 目录
    ├── Context（项目）
    │   ├── 名称
    │   ├── Address[]（名称 / 地址 / 备注）
    │   └── 可组合能力
    └── ...
    ├── Connection
    ├── Task
    ├── Conversation
    ├── State
    ├── Records
    ├── Verification
    └── ...
```

Space 属于 Connection 体系，但主要职责是组织目录与 Context。

**Space 负责组织；Context 负责项目；Address 负责定位；Resource 负责实际内容；能力按 Context 组合。**

Address 属于 Context，回答“在哪里”；Resource 表示 Address 所定位的实际内容。Task、Conversation、State、工作记录、验证等不是所有 Context 的固定组成部分。

### 软件开发结构

当前已确认“软件开发结构”作为软件开发 Context 的可组合能力纳入 APS，不作为新的顶层 Project 模型。

```text
目标 / UI → 功能 → 架构 → 施工点 → 施工卡 → 代码 / 产物 → 验证
```

当前状态：**已设计，尚未实现。**

它与现有 Context、Task、Resource、State、Receipt / Evidence、Verify 的分工是：结构维护开发语义关系；Resource / Connection 提供实际资料和外部能力；Task 承接具体施工；State 保存当前工作状态；Receipt / Evidence / Verify 确认实际结果。

## 三、多 AI 协作状态

### 已确认

- Space 属于 Connection 体系，但主要承担目录与 Context 的组织职责；
- 目录用于分组 Context；Context 可以有多个 Address；Resource 由 Address 定位；
- Context 是 Space 内的具体工作上下文，能力按场景组合；
- 一个 Context 可以使用多个 Connection；
- AI 是 Connection，不是固定角色；
- AI 具有拆分 / 做 / 审查三个独立能力权限；
- Dispatcher 负责三阶段任务调度；
- 调度必须依据实际执行结果推进。

### 待设计

**“一个 Context 多 AI 协作施工”的具体交接与并行机制仍待设计。**

需要后续单独确定：

- 谁负责当前施工任务；
- AI 如何把任务交给另一个 AI；
- 如何共享上下文和当前工作状态；
- 如何确认协助结果；
- Repository / Branch 的施工权如何交接；
- 多 AI 并行时如何避免冲突；
- Dispatcher 保存哪些最小状态。

因此目前不能把 Secretary / Orchestrator、Decision / Worker 或复杂 Agent Runtime 标记成解决方案。

## 四、当前代码问题

- Context / Connection / Resource 正在向当前模型收口；
- GitHub 能力已经具备基础连接与读取能力，但部分施工接口仍带有旧本地文件语义；
- Local 能力保留，但现有本地操作模块较弱，需要后续按 Local Connector 重构；
- 旧 BridgeFS 命名和历史代码仍有残留，不能视为当前产品模块；
- Dispatcher 的正式代码边界仍需结合真实施工链继续确认；

## 五、文档状态

当前文档分三类：

1. **当前正式资料**：`PROJECT/SPEC`、`PROJECT/ARCHITECTURE`、`PROJECT/UI`；
2. **状态 / 任务 / 专项资料**：`docs/`；
3. **历史资料**：`docs/bridgefs-assets/` 及明确标记为历史的旧设计。

文档基线见：

`PROJECT/ARCHITECTURE/DOCUMENTATION-BASELINE.md`

## 六、第一阶段暂不施工

- 多 AI 完整协作协议；
- 完整 Agent Runtime；
- 无限 Agent Loop；
- 复杂 Workflow Engine；
- Secretary / Orchestrator；
- BridgeFS 作为产品模块。

## 七、2026-10-15 状态校准补记

### 应用标识与版本

- app/build.gradle 当前配置：applicationId=com.aps、namespace=com.owla19s.aps、versionName=0.1.1；应用显示名称由 strings.xml 设置为 APS。
- 因此安装包标识与应用显示名称已调整，但 namespace 和 Kotlin/Java 源码 package 仍保留 com.owla19s.aps，不能声称“所有包名已统一”。
- 下一批应把内部 namespace / package 迁移作为独立代码变更，完整检查源码声明、跨包 import、Manifest 组件引用、测试代码、R / BuildConfig 引用；迁移后必须以 Android Tests 和 Build and Release 结果验收。由于 applicationId 已是 com.aps，安装旧 com.owla19s.aps 的版本通常不能直接覆盖升级，真机验收前需考虑旧数据备份。

### 诊断日志功能链

- Task AI 执行与 API 连接测试已接入本地 RuntimeDiagnostics 事件记录。
- 日志文件具备单段大小限制、滚动保留与 7 天清理；日志查看 / 导出读取范围优化正在 PR #141 中等待 CI，未合并前不计入主线已完成能力。
- 真机文件写入、崩溃捕获、日志查看和系统文件选择器导出仍未完成设备验收。

### 验证边界

- PR Android Tests 成功不等于合并后的 Verify / Build and Release 成功。
- 未取得具体提交对应的工作流终态时，状态保持“未确认”，不写成“通过”。
