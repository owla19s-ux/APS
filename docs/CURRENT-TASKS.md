# APS 当前任务总表

更新时间：2026-10-13

> 本文件只维护当前仍需施工 / 验收 / 设计的任务。产品定义、架构原则、UI 正式设计和仓库操作规则分别由 PROJECT 与对应 docs 负责。

## 施工节奏调整（2026-10-12）

为减少小改动反复等待完整工作流、同时避免功能方向偏离，当前施工采用“功能链分批施工、分层验证、批次复核”：

1. 先以 `PROJECT/UI/README.md`、`PROJECT/SPEC/RELEASE-SCOPE-0.1.1.md` 和 `PROJECT/MAP/README.md` 为需求索引；不得只依据最近一次报错选择工作。
2. 同一功能链的相关改动可连续完成。每个小改动先做必要的源码检查和局部测试，不要求每一步都人工等待完整 Verify。
3. 涉及数据模型、持久化、权限、安全、公共接口或跨模块契约时，提前执行针对性验证。
4. 每批结束时执行与变更范围相匹配的测试和完整集成工作流；检查真实工作流终态，不以提交成功代替验证。
5. 每批对照正式 UI 与产品范围，记录“已实现且有证据 / 已接通但待运行验证 / 未实现 / 存在问题”，防止把页面入口当成完整功能。
6. 仅将影响 0.1.1 需求、安全、数据可靠性、构建或主链的缺口作为当前阻塞；未来能力登记到 0.1.2，不无故打断当前施工。
7. 一次施工围绕一个可验收功能链，避免混入无关重构。合并后按需要检查主线工作流；不要求每个局部提交都单独触发完整验证。

### 当前校准结果

- 2026-10-12：PR #88 已合并，合并后 Verify #273 与 Android Build and Release #826 均已成功。
- 当前任务表上次更新时间为 2026-10-07，与近期合并及连接模块进展存在时间差；本次更新施工规则，后续按实际源码、测试和工作流证据逐项刷新功能状态。
- 当前先按功能表核对 API、GitHub、Local/File、普通对话页、Context 工作面和软件开发结构的端到端缺口，再按优先级分批施工；不因 API 局部完善而无限扩展审查。

## 1. 当前第一阶段主线

### T1 — Space → 目录 → Context 工作面
**状态：开发中**

验收重点：
- 空间页进入 Space，按目录展示 Context；
- Context 主要对话是主体；
- Context 使用的 Address / Resource、Connection、AI 权限以及当前存在的任务 / 状态能力可查看；
- Context 的连接与配置独立展开 / 收起；
- 不把内部服务对象全部平铺成复杂 Dashboard。

### T2 — Context Conversation
**状态：开发中**

目标：

```
Space → 目录 → Context
 ↓
Connection / Resource
 ↓
AI Connection
 ↓
Context 对话
 ↓
真实读取 / 工作
```

### T3 — Task / Dispatcher / Execution
**状态：开发中**

继续验证：
- Task 生命周期；
- 拆分 / 做 / 审查；
- 三项 AI 权限；
- Dispatcher；
- ConstructionLock；
- Execution；
- Commit 前置条件。

### T4 — GitHub / Commit / Verify
**状态：普通对话 GitHub 只读、多文件上下文和目录浏览入口已接入；Actions 运行列表与单次运行详情已接入。合并提交 `b22b720` 的 Android Tests #38205186446、Verify #38205186448、Android Build and Release #38205186456 均已通过，真实设备链路待验收**

继续验证：
- GitHub Connector；
- Repository / Branch；
- 文件读取 / 修改；
- Commit；
- Actions / Verify；
- 远程 Repository 作为 GitHub Connection 的 Resource 来源。

### T5 — Local Connector
**状态：基础接口与普通对话页入口已接入，三项 CI 通过**

本地项目能力保留。

当前已新增 Local Connection、元数据 Store、Android SAF Gateway 与 LocalFileConnector；普通对话页可以选择目录、浏览文件、读取文本、编辑并显式保存；连接级读写开关守卫实际操作。

后续目标：
- 补撤权后的状态提示与重新授权；
- 后续复用到 Context 工作面；
- 补撤权、文件冲突、写入失败恢复及 Provider 差异测试；
- 不恢复 BridgeFS 产品模型，不提前建立复杂本地执行引擎。

### T6 — 云端测试与主链验证
**状态：开发中**

建立：
- 模块测试；
- 契约测试；
- Context 主链测试；
- Commit → Actions → Verify 验证。

### T7 — 独立对话 / 设置
**状态：UI 已确认 / 开发中**

### T8 — 软件开发结构
**状态：结构树、Task 生命周期、Task—节点关联增删、已保存 Verify 快照及结构节点直达施工卡已进入主线；本批新增关系边管理 UI，待 CI 与设备验收；Dispatcher 与真机验收仍待施工**

基础组织前提：Space → 目录（分组）→ Context（项目）。每个 Context 具有名称，并可维护多个 Address（名称 / 地址 / 备注）。

目标：在软件开发 Context 中建立统一的软件开发结构能力，连接：

```text
目标 / UI → 功能 → 架构 → 施工点 → 施工卡 → 代码 / 产物 → 验证
```

原则：不新增 Project 顶层模型；不做独立 Dashboard；底层关系允许一对多 / 多对多；Task 负责施工交接，结构负责语义关系，Receipt / Evidence / Verify 负责事实依据。

本批基础数据层已新增 `DevelopmentNode`、`DevelopmentRelation`、`DevelopmentTreeStore` 与测试：稳定 ID / code / parentId 分离；关系单独存储并去重；节点限制在同一 Context 内；移动节点防止形成循环；父节点存在子节点时拒绝删除；删除节点时清理关联边；损坏 JSON 原文备份后恢复为空集合。首批 Android Tests 已通过（run 38222950294），已合并至 main（43fc960）。main 上 Verify / Build and Release 当时仍在运行。第二批新增树投影、根节点/子节点查询、入边/出边查询与对应仪器测试。第三批独立建立 `DevelopmentTaskLink` / `DevelopmentTaskLinkStore`，支持 Task—节点多对多关联、Context 校验与查询；PR #97 Android Tests #38224193343 已通过并已合并（`2c4f6a2`）。第四批 PR #98 已合并（Android Tests #38225525007 通过），建立 `DevelopmentTask` / `DevelopmentTaskStore`，包含 CREATED、READY、RUNNING、COMPLETED、FAILED、CANCELLED 状态、迁移守卫、执行次数、结果摘要、失败信息与本地持久化。PR #102 已合并（Android Tests #38226828700 通过），修复时间戳倒退并纠正已完成任务终态测试。生命周期事件记录已整合进 `DevelopmentTaskStore`，避免引入第二套 Task 身份；PR #103 已合并，Android Tests #38234148573 通过。PR #104 已合并，Android Tests #38234490761 通过，Task 状态与事件回执在单次操作中使用同一 SharedPreferences Editor 提交；跨实例并发仍未解决。TaskLink 对真实 Task 的存在性校验与节点删除清理已合并。PR #105 已合并，新增 `DevelopmentVerification` / `DevelopmentVerificationStore` 保存实际 Actions 运行快照。普通对话页 Actions 详情已增加“关联到 Task”入口，限制为当前对话已关联的 Context，并保存 API 返回的运行状态、结论、URL、提交 SHA 与时间；PR #106 Android Tests #38236109281 通过并已合并。后续批次增加“验证记录”入口，按当前 Context 列出记录、查看详情并打开对应 GitHub 页面；该批等待 CI，真机链路待验收。自动同步、Artifacts、失败重试、Dispatcher 与用户确认交互仍未完成。PR #115 已合并到 main（提交 `af3f450c`），结构节点可直达关联施工卡并按节点筛选；从筛选页新建 Task 会自动关联当前节点。合并后 Android Tests #38348620576、Verify #38348620598、Android Build and Release #38348620581 均成功。当前批次新增关系边管理 UI，支持当前 Context 内选择源/目标节点、设置关系类型与备注、查看并删除关系；删除只移除关系边，不删除节点或施工卡。PR #99 的 `ConstructionTask` 仍暂缓合并，避免形成两套任务模型。 本轮 PR #113 已合并（主线提交 `044b298c`）：施工卡详情支持逐条解除 Task—节点关联；解除只删除关联，不删除 Task、节点或已保存的 Verify 记录。合并后 Android Tests #679、Verify #298、Android Build and Release #850 均成功。

其中 UI / 交互必须预留“待用户确认 → 已确认 → 已变更 / 待重新确认”的状态；用户确认是设计认可，不等同于代码 Verify。第一版可以暂不实现完整确认交互。

### T9 — 多 AI 协作施工
**状态：待设计**

这是当前尚未解决的重要问题。

需要单独确定：

1. 一个 Context 内多个 AI Connection 如何分工；
2. 任务如何交接；
3. 上下文 / 工作状态如何共享；
4. AI 如何确认前一个 AI 的结果；
5. Repository / Branch 施工权如何交接；
6. 并行施工如何避免冲突；
7. Dispatcher 需要保存哪些最小状态。

**当前只保留“请求 AI 协助”的入口和边界，不提前确定具体协作协议。**

### APK 应用标识状态（2026-10-13 更新）

- 主线 `applicationId` 已调整为 `com.aps`；本次 Build and Release 产物 `APS.apk` 已从实际 APK 的二进制 Manifest 字符串中确认包含 `com.aps`，不再使用 `com.owla19s.aps` 作为安装包标识。
- Android `namespace` 与 Kotlin/Java 源码 package 仍为 `com.owla19s.aps`，这是本次有意保留的内部命名空间，不应误报为已完成整体包名迁移。
- 本次主线构建产物来自提交 `044b298c349b4d6a9252fecf13e23678095d9ba0`，Build and Release run #850 成功；应用标签与版本号尚未通过解包检查，本次只确认 application ID。
- 因 `applicationId` 改变，Android 会将新包识别为不同应用，通常不能作为旧 `com.owla19s.aps` 的直接覆盖升级；真机安装前需考虑旧应用数据备份/迁移。
- 后续若决定彻底移除内部 `owla19s` 命名空间，应单独施工并检查 Kotlin/Java package、Manifest、测试包名、BuildConfig 引用及 Gradle 配置。
### T10 — 正式 APK / 真机验收
**状态：待主链收口后验收**

范围：
- 安装 / 启动；
- 空间页 / Context 工作面操作；
- 生命周期；
- 系统权限；
- 实际 API / GitHub；
- 最终用户体验。

## 2. 当前优先级

第一阶段施工顺序以当前 UI / 功能地图为索引，按“先打通用户高频入口和可复用连接能力，再接组织页与调度结构”推进：

1. **Conversation 基础闭环**：对话独立创建 / 切换 / 管理、自动保存与恢复、错误处理；明确当前 UI 占位响应，不把它视为真实 AI 已接通。
2. **AI / API / Model**：多 Profile 持久化、默认连接、旧配置迁移和连接管理 UI 已提交并通过对应工作流；当前统一 Connector Factory / Registry 已提交并通过 Verify #178、Android Tests #422、Build and Release #731。已修复 Profile Store 不能因某个密钥解密失败而丢失其他 Profile 元数据的问题；对话级连接选择和模型 ID 覆盖已接入首版并持久化；对话内模型目录拉取与选择已接入首版；全局 API 设置增加真实聊天请求测试入口；SSE 流式输出及运行中取消已有单元测试覆盖；后续补流式失败/中断边界，并进行真实服务商端到端验收。
3. **Local / File Connector**：本地目录授权、读取 / 修改 / 保存、失败处理；可由普通对话页和 Context 工作面按授权复用。
4. **GitHub 接入 UI 与真实读取**：普通对话页已接入令牌验证/加密保存、仓库/分支选择、目录浏览、最多 5 个文件的只读上下文注入及本机令牌清除入口；PR #48 合并后的 Android Tests #505、Verify #230、Android Build and Release #783 均通过。下一步完成真实设备与实际 GitHub / AI 端到端验收，重点覆盖授权、目录浏览、多文件选择、一次性上下文注入和失败重试。
5. **Local ↔ GitHub 协作**：在两侧连接能力稳定后，再明确同步方向、覆盖 / 冲突处理和验证规则。
6. **Space → 目录 → Context UI**：把组织管理页接到已经独立稳定的 Conversation、Local 和 GitHub 能力；不把 Space 提前变成开发阻塞项。
7. **软件开发结构 / Task / Dispatcher / Verify**：按实际需要逐步建立结构、施工交接和证据链，不把设计文档当成已经运行的机制。
8. **多 AI 协作协议与真机整体验收**：在单连接主链可靠后再设计多 AI 交接；最后执行 APK 安装、真实 API / GitHub 和端到端验收。

这是施工优先级，不代表低优先级项目不重要，也不要求前一项的所有边缘功能完美后才能开始后一项。发现共享机制或验证阻塞时，可以按依赖调整顺序。

功能状态和跨模块依赖见 `PROJECT/MAP/README.md`。该地图是持续维护的索引，不是开发前置审批表；每次只更新本轮受影响的行。
 
## 3. 明确不做

- 固定 Decision AI / Worker AI；
- 固定 AI A / AI B；
- 普通 Context 问题自动多 AI；
- 复杂 Context Dashboard；
- 无限 Agent Loop；
- GitHub 第二套 Workspace 模型；
- BridgeFS 作为当前产品模块；
- Secretary / Orchestrator 作为既定架构；
- ProjectContinuationCoordinator 旧 continuation 运行链。

## 4. 任务维护规则

1. 新任务先判断是否属于当前第一阶段主线。
2. 架构决策写入 PROJECT，不在本表形成第二套规格。
3. 历史过程写入 STATUS 或专项历史文档。
4. 未确定的能力标记“待设计”，不能写成“已设计”。
5. 每次施工后只更新实际发生变化的任务状态。
