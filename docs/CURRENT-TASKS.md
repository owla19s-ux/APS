# APS 当前任务总表

更新时间：2026-10-14

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

当前已新增 Local Connection、元数据 Store、Android SAF Gateway 与 LocalFileConnector；普通对话页可以选择目录、浏览文件、读取文本、编辑并显式保存；连接级读写开关守卫实际操作。 保存已有文件必须带上编辑器打开时的原始内容快照；Connector 通过内容比对拒绝覆盖期间已变化的文件，避免调用方无意绕过冲突检测。

后续目标：
- 补撤权后的状态提示与重新授权；
- 后续复用到 Context 工作面；
- 补撤权、文件冲突、写入失败恢复及 Provider 差异测试；
- 不恢复 BridgeFS 产品模型，不提前建立复杂本地执行引擎。

### T6 — 云端测试与主链验证
**状态：开发中**

本批同步加强 Android Tests 工作流：除 `:app:testDebugUnitTest` 外，增加 `:app:compileDebugAndroidTestKotlin` 与 `:app:compileDebugAndroidTestJavaWithJavac`。首次运行暴露出 instrumentation 测试遗留包名错误，以及 Verify 测试夹具中的时间戳参数拼写错误；已修正两处，等待最新提交 CI 复验。该工作流只编译 instrumentation 测试源码，不执行真机测试。

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

本批基础数据层已新增 `DevelopmentNode`、`DevelopmentRelation`、`DevelopmentTreeStore` 与测试：稳定 ID / code / parentId 分离；关系单独存储并去重；节点限制在同一 Context 内；移动节点防止形成循环；父节点存在子节点时拒绝删除；删除节点时清理关联边；损坏 JSON 原文备份后恢复为空集合。首批 Android Tests 已通过（run 38222950294），已合并至 main（43fc960）。main 上 Verify / Build and Release 当时仍在运行。第二批新增树投影、根节点/子节点查询、入边/出边查询与对应仪器测试。第三批独立建立 `DevelopmentTaskLink` / `DevelopmentTaskLinkStore`，支持 Task—节点多对多关联、Context 校验与查询；PR #97 Android Tests #38224193343 已通过并已合并（`2c4f6a2`）。第四批 PR #98 已合并（Android Tests #38225525007 通过），建立 `DevelopmentTask` / `DevelopmentTaskStore`，包含 CREATED、READY、RUNNING、COMPLETED、FAILED、CANCELLED 状态、迁移守卫、执行次数、结果摘要、失败信息与本地持久化。PR #102 已合并（Android Tests #38226828700 通过），修复时间戳倒退并纠正已完成任务终态测试。生命周期事件记录已整合进 `DevelopmentTaskStore`，避免引入第二套 Task 身份；PR #103 已合并，Android Tests #38234148573 通过。PR #104 已合并，Android Tests #38234490761 通过，Task 状态与事件回执在单次操作中使用同一 SharedPreferences Editor 提交；TaskStore 的 create/transition/delete 曾仅依赖实例级 @Synchronized，不同 Store 实例可能同时读改写并覆盖彼此更新；本批新增进程内共享互斥锁串行化三类写操作，待 CI 验证。该锁只覆盖同一进程，不承诺跨进程写入。TaskLink 对真实 Task 的存在性校验与节点删除清理已合并。PR #105 已合并，新增 `DevelopmentVerification` / `DevelopmentVerificationStore` 保存实际 Actions 运行快照。普通对话页 Actions 详情已增加“关联到 Task”入口，限制为当前对话已关联的 Context，并保存 API 返回的运行状态、结论、URL、提交 SHA 与时间；PR #106 Android Tests #38236109281 通过并已合并。后续批次增加“验证记录”入口，按当前 Context 列出记录、查看详情并打开对应 GitHub 页面；该批等待 CI，真机链路待验收。自动同步、Artifacts、失败重试、Dispatcher 与用户确认交互仍未完成。PR #115 已合并到 main（提交 `af3f450c`），结构节点可直达关联施工卡并按节点筛选；从筛选页新建 Task 会自动关联当前节点。合并后 Android Tests #38348620576、Verify #38348620598、Android Build and Release #38348620581 均成功。当前批次新增关系边管理 UI，支持当前 Context 内选择源/目标节点、设置关系类型与备注、查看并删除关系；删除只移除关系边，不删除节点或施工卡。PR #116 已合并（`c04b4e8c`），合并后 Android Tests #685、Verify #301、Android Build and Release #854 均成功。PR #117 已合并到 main（`de2fdcee`）；Android Tests #38368722469、Verify #38368722382、Android Build and Release #38368722384 均成功。PR #119 已合并（`a61c9b74`），Task 增加拆分 / 施工 / 审查职责字段、旧记录兼容默认值、创建界面选择与持久化测试；PR Android Tests #38369295278 成功，主线工作流待确认。PR #120 已合并到 main（提交 `4e807ecc`），合并后的 Android Tests #38371902123、Verify #38371902199、Android Build and Release #38371902115 均成功。该批新增本地 READY 队列选择器与“待调度队列”入口，只筛选当前 Context 的 READY Task，按创建时间排序；当前 UI 暂未提供职责筛选入口。下一批 `feat/task-ai-execution-handoff` 已新增显式 Task→AI 交接服务与详情页“交给 AI 执行”入口：只有 READY Task 可执行；先进入 RUNNING，成功将 AI 返回内容保存为 REVIEW_REQUIRED，失败记录 FAILED 且不写入异常原文；不自动修改本地代码、不提交 GitHub、不触发 Verify。该批待 CI 检查。PR #99 的 `ConstructionTask` 仍暂缓合并，避免形成两套任务模型。 本轮 PR #113 已合并（主线提交 `044b298c`）：施工卡详情支持逐条解除 Task—节点关联；解除只删除关联，不删除 Task、节点或已保存的 Verify 记录。合并后 Android Tests #679、Verify #298、Android Build and Release #850 均成功。

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

### APK 应用标识状态（2026-10-15 复核）

- 当前 `app/build.gradle`：`applicationId 'com.aps'`、`namespace 'com.owla19s.aps'`、`versionName '0.1.1'`；`strings.xml` 中应用显示名称为 `APS`。
- 之前 Build and Release run #850 对提交 `044b298c349b4d6a9252fecf13e23678095d9ba0` 的 APK 检查确认安装包标识为 `com.aps`；该证据不能证明当前 HEAD 的 APK 标签、版本号或内部 package 迁移已通过验证。
- 结论：应用显示名称和安装包标识已调整；Gradle namespace 与 Kotlin/Java 源码 package 尚未统一，仍需独立迁移，不能标记为全部完成。
- `applicationId` 从旧值改为 `com.aps` 后，Android 通常会把它识别为不同应用，不能假设可直接覆盖安装旧 `com.owla19s.aps`；设备测试前需考虑旧数据备份。
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


### 2026-10-14 批次施工：Task AI 执行生命周期

- Context 功能选择 PR #129 已合并；合并提交 `09dc4434` 的 Android Tests、Verify、Android Build and Release 均成功。
- 本批围绕已有 `DevelopmentTaskExecutionService` 做生命周期收口：施工卡页不再用裸 `Thread + runBlocking` 执行 AI 请求，改用 `lifecycleScope` 管理协程，在 IO dispatcher 执行；Activity 生命周期取消时将取消传播给执行服务，由服务尽力把 RUNNING 记录为 CANCELLED。
- 新增 instrumentation 测试覆盖 AI 请求取消后的 CANCELLED 状态、事件记录及敏感异常文本不落入错误字段。
- 边界保持不变：只接受 READY Task；成功结果进入 REVIEW_REQUIRED；失败记录 FAILED；不会自动修改本地文件、提交 GitHub 或触发/判定 Verify。
- 本批代码待 CI 验证；Activity 销毁/旋转时的设备行为、真实 Provider 取消响应和重复点击体验仍需设备验收。

## 2026-10-14 批次施工：Task 执行取消控制与状态竞态

- 前置：PR #130 已合并；PR Android Tests 通过。主线 Android Tests 与 Verify 已通过，Android Build and Release 尚待终态确认。
- 本批针对真实状态竞态：AI 执行处于 RUNNING 时，原界面仍允许手动“提交审查 / 标记失败 / 取消任务”，可能与执行服务的最终状态迁移冲突。
- 改为对当前 Activity 管理的执行任务显示“取消 AI 执行”，由取消 Job 传递 CancellationException，交给执行服务记录 CANCELLED；结束后刷新本地 Task 快照。
- 非当前 Activity 持有的遗留 RUNNING Task 仍保留手动恢复操作，避免进程中断后任务无法处理。
- 不改变 AI 结果审查规则，不自动改文件、提交代码或触发 GitHub Verify。
- PR #131 已合并；PR Android Tests 已通过。主线后续验证见工作流记录；真机取消与 Provider 实际取消行为仍待验收。

## 2026-10-14 批次施工：保护运行中的 Task

- PR #131 的后续复核发现：活动执行期间虽然已把状态操作收敛到“取消 AI 执行”，详情菜单仍可删除 Task；取消 Job 刚发出但尚未清理映射时，若只检查 Job.isActive，也可能短暂重新显示普通状态操作。
- 当前批次以活动 Job 映射是否存在判断执行控制权；RUNNING Task 不显示删除入口；Store 层拒绝删除 RUNNING Task，防止绕过 UI 或并发操作造成执行结果丢失。
- 新增 Store instrumentation test：运行中删除被拒绝，转为 CANCELLED 后可删除。
- 不改变任务状态机、审查规则或 GitHub Verify 边界；待 PR CI 验证。

## 2026-10-14 批次施工：Task 失败恢复确认

- 失败 Task 选择“重新设为待执行”时，先显示上次失败原因与执行尝试次数，需用户确认后才转为 READY。
- 确认后清理当前错误与旧结果摘要，并明确提示：只重新排队，不自动调用 AI。
- 生命周期事件区分“首次进入待执行队列”与“失败后重新排队”，方便追查任务恢复过程。
- 新增 Store 测试验证失败恢复事件摘要；PR #134 已合并，PR Android Tests #38519142777 成功。
- 不自动重试 AI、不修改文件、不提交 GitHub、不触发 Verify。


## 2026-10-14 批次施工：待调度队列按职责筛选

- 待调度队列增加“全部职责 / 拆分 / 施工 / 审查”筛选入口，复用现有 DevelopmentTaskDispatcher.readyQueue 的 Context、READY 状态和创建时间规则。
- 仅过滤并展示本地 READY Task；不自动启动 AI，不改变 Task 状态，不触发 GitHub Workflow 或 Verify。
- 已有 Dispatcher 单元测试覆盖按职责筛选和状态不变；PR #135 已合并（提交 `7a3ba417`）；Android Tests #38585124993 的 unit-test Job 成功。设备交互仍待验收。


## 2026-10-15 批次施工：Task 执行结果接入运行诊断

- Task→AI 执行服务统一记录 started / success / failed / cancelled 事件和耗时，复用现有 RuntimeDiagnostics 本地日志。
- FAILED 的 Task 摘要只保留异常类型和固定失败类别，不写入任意异常消息；详细 API 日志仅记录受控事件、HTTP 状态、异常类型、失败类别和耗时，不记录请求正文、响应正文或凭据。
- 不改变 READY→RUNNING→REVIEW_REQUIRED / FAILED / CANCELLED 状态边界；不自动改文件、提交 GitHub 或触发 Verify。
- PR #136 已合并（提交 `42686bf`），Android Tests #747 成功；当前未取得合并后 Verify / Build and Release 的终态证据，不标记为通过。日志文件、崩溃记录和 API 连接记录仍待真机验收。


## 2026-10-15 验证状态补记

- PR #135（待调度队列按职责筛选）已合并，合并提交 `7a3ba417`；Android Tests #38585124993 的 unit-test Job 成功。
- 本次查询未取得合并提交对应的 Verify / Build and Release 运行记录，因此不标记这两项主线验证成功。


## 2026-10-15 批次施工：API 连接测试结果接入运行诊断

- AI/API 设置页的“测试聊天请求”和“获取模型列表”记录 started、success/empty、failed 及耗时，复用 RuntimeDiagnostics。
- 失败事件仅记录受控事件名、耗时、HTTP 状态、异常类型与固定失败类别；不记录请求/响应正文、API Key 或任意错误消息。
- 页面继续显示已有的可操作错误提示；日志用于事后排查，不替代真实服务商端到端验收。


## 2026-10-15 批次施工：诊断日志超限滚动

- 复核发现，单日日志达到 1 MiB 后旧实现会直接覆盖原文件，只保留截断标记和新记录，可能丢失此前排查崩溃所需的关键上下文。
- 本批改为超限时将当前日志段改名保留，再创建新的当日日志；若无法完成改名则不覆盖原日志。每个日志段仍有 1 MiB 上限，旧文件继续按 7 天保留策略清理。
- 增加日志滚动阈值单元测试；等待 PR CI 验证。真实设备文件操作仍待验收。
