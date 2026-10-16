# APS × Codex Cloud 接入与验证（试运行）

> 状态：**准备中**。本文件是外部开发工具接入说明，不是 APS 产品架构变更，不表示 Codex Cloud 已授权或已执行。
>
> 范围：`owla19s-ux/APS`，从 `main` 建立隔离任务；不修改产品架构、不自动发布 APK、不合并未经审查的 PR。

## 1. 仓库已验证基线（2026-10-16）

- main：`024ca95d542ed7e74e1d7bf726376f36e1d7613d`
- Android Tests：Run #763 / ID `38767234417`，`success`；
- Verify：Run #324 / ID `38767234421`，`success`。
- 两个运行都对应上述 main commit；不能把历史运行结果当成未来 commit 的验证。
- 工程：Android/Kotlin，JDK 17，Gradle 8.10，compileSdk 35；`android-tests.yml` 在 push(main)、PR→main、手动触发时运行。
- Release 工作流 `android-build.yml` 仅手动触发；Codex 接入试验不得触发正式发包。

## 2. 账号侧：创建并发布 Codex Cloud 环境（必须由用户授权）

通过 ChatGPT/Codex 的 **Web 或桌面端** 创建 Codex Cloud 环境（移动端可在已发布环境上发起任务；第一次创建/发布使用 Web/桌面端）：

1. 使用有 Codex Cloud 权限的 ChatGPT 账户，进入 Codex → Cloud / Environments；
2. 如有提示，连接 GitHub 并授权访问 `owla19s-ux/APS`；当前聊天中 GitHub 可操作，不代表 Codex Cloud 已获授权；
3. 选择 APS 仓库及 `main` 作为起点，建立隔离环境；
4. 在环境配置/准备阶段验证 Java 17、Gradle 8.10 和 Android SDK（platform android-35、build-tools 35.0.0）可用；需要下载安装时，只允许可信依赖源（Google Android SDK、Gradle、Maven Central、Google Maven）；
5. 先运行第 3 节诊断命令，确认可以编译与测试，再发布环境。

不要提交或粘贴 OpenAI API Key、GitHub token、签名 keystore 或 Release Secrets。首轮无需安装 Android 模拟器，不需要生产签名。

## 3. 最小 Cloud 环境验收（先只读 + 测试）

在 Codex Cloud 环境的 APS 仓库根目录运行：

```bash
git rev-parse HEAD
java -version
gradle --version
echo "ANDROID_HOME=${ANDROID_HOME:-<unset>}"
gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestJavaWithJavac --stacktrace
```

说明：仓库没有内置 Gradle Wrapper，当前 CI 使用 `gradle/actions/setup-gradle@v4` 提供 8.10；Codex Cloud 若没有 `gradle`、Android SDK 或网络无法获取依赖，应如实报告缺失项并先修环境，不能把失败解释为“业务代码有错”。`compileDebugAndroidTest*` 只编译仪器测试，不等于手机或模拟器执行过仪器测试。

首轮 Codex 任务可直接使用以下指令：

> 读取 `AGENTS.md`、`README.md`、`docs/CODEX-CLOUD-BOOTSTRAP.md` 和 `.github/workflows/android-tests.yml`。只诊断环境并执行第 3 节的测试命令，不改业务代码，不提交 Commit/PR，不运行 release，不要求生产签名。报告实际 Git SHA、JDK/Gradle/Android SDK 状态、每项命令退出码及测试输出。失败时记录第一条真实错误与修复建议，不得声称测试通过。

## 4. GitHub CI 验证与后续首个实际施工试验

- 本文档以单独分支与**草稿 PR**提交到 main，确认 PR 触发 `Android Tests`；核对 Run ID、事件、head SHA、Job 结果。
- 注意：PR 的 `github.sha` 可以是 GitHub 合成的测试合并提交；核验其 PR 与 head 关联，不强求与分支 HEAD 完全相等。
- Codex Cloud 侧测试成功后，再让 Codex 从新任务分支处理一个**范围极小、可测试**的更改并发 PR；只验证 `Codex → GitHub PR → Actions → Review`，不能以本文件 PR 冒充 Codex 实际施工成功。
- `Verify` 当前主要在 main push / 手动时执行；PR 必须通过 `Android Tests`，不得把未运行的 Verify 当成成功。
- PR Review / CI 通过后由用户决定是否合并；本试验不触发 Release APK。

## 5. 达标定义与状态登记

| 检查项 | 当前状态 | 证据 |
|---|---|---|
| APS GitHub 仓库连接读写 | 已确认 | GitHub 连接对仓库有 push 权限 |
| 原有 main CI 基线 | 已验证 | Android Tests #763、Verify #324 |
| Codex Cloud 环境与仓库授权 | **未验证** | 必须取得 Cloud 环境成功运行证据 |
| Codex Cloud 内 Gradle 测试 | **未验证** | Cloud 实际命令输出/退出码 |
| 从隔离 PR 触发 Android Tests | **待验证** | 当前试验 PR 的 Run/Job |
| Codex 发起 PR → CI → 复审完整链路 | **未验证** | 首个由 Codex 实际完成的 PR |

原则：讨论、文档、Commit、Workflow 已触发、Job success、手机安装验证是不同状态；不混为一谈。
